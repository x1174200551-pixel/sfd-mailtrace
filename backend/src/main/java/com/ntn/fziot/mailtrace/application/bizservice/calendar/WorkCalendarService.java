package com.ntn.fziot.mailtrace.application.bizservice.calendar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkCalendarService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String MODULE_WORK_CALENDAR = "WORK_CALENDAR";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // P3：工作日历按企业归属和筛选。
    private final WorkCalendarMapper workCalendarMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final SlaPolicyMapper slaPolicyMapper;
    private final HolidayMapper holidayMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;

    /**
     * 查询工作日历列表。
     */
    public WorkCalendarListResponse listCalendars(CurrentUserPrincipal principal, Long enterpriseId,
                                                  String keyword, Boolean defaultCalendar) {
        // 1、仅管理员可进入工作日历配置。
        permissionService.assertPermission(principal, "work_calendar:read", "无权查看工作日历");

        // 2、按名称和默认标识筛选，默认日历优先展示。
        LambdaQueryWrapper<WorkCalendarEntity> wrapper = buildQuery(enterpriseId, keyword, defaultCalendar)
                .orderByDesc(WorkCalendarEntity::getDefaultCalendar)
                .orderByAsc(WorkCalendarEntity::getId);
        List<WorkCalendarVO> records = workCalendarMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();

        // 3、返回列表和摘要，供后续 SLA 页面选择日历。
        return new WorkCalendarListResponse(records, buildSummary(enterpriseId));
    }

    /**
     * 新建工作日历。
     */
    @Transactional
    public WorkCalendarVO createCalendar(CurrentUserPrincipal principal, WorkCalendarSaveRequest request) {
        // 1、校验管理员权限、工作日、时区和工作时段。
        permissionService.assertPermission(principal, "work_calendar:create", "无权新建工作日历");
        assertEnterpriseEnabled(request.getEnterpriseId());
        CalendarConfig config = validateCalendar(request);
        boolean defaultCalendar = Boolean.TRUE.equals(request.getDefaultCalendar());
        ensureDefaultCalendarUnique(request.getEnterpriseId(), defaultCalendar, null);

        // 2、写入日历主体，节假日留到后续 P1-W5-BE-05 单独维护。
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        fillCalendar(calendar, request, config, principal.account());
        calendar.setCreatedBy(principal.account());
        calendar.setUpdatedBy(principal.account());
        workCalendarMapper.insert(calendar);

        // 3、记录配置变更日志并返回最新数据。
        recordLog(principal, "CREATE", calendar.getId(), "新建工作日历：" + calendar.getCalendarName());
        return toVO(workCalendarMapper.selectById(calendar.getId()));
    }

    /**
     * 编辑工作日历。
     */
    @Transactional
    public WorkCalendarVO updateCalendar(CurrentUserPrincipal principal, Long id, WorkCalendarSaveRequest request) {
        // 1、校验权限、日历存在性和字段组合。
        permissionService.assertPermission(principal, "work_calendar:update", "无权编辑工作日历");
        WorkCalendarEntity existing = requireCalendar(id);
        assertEnterpriseEnabled(request.getEnterpriseId());
        CalendarConfig config = validateCalendar(request);
        boolean defaultCalendar = Boolean.TRUE.equals(request.getDefaultCalendar());
        ensureDefaultCalendarUnique(request.getEnterpriseId(), defaultCalendar, id);
        if (!existing.getEnterpriseId().equals(request.getEnterpriseId()) && isReferenced(id)) {
            throw new BusinessException(CODE_CONFLICT, "工作日历已被 SLA 策略或节假日引用，不能变更所属企业");
        }

        // 2、覆盖可编辑的工作日历配置。
        WorkCalendarEntity update = new WorkCalendarEntity();
        update.setId(id);
        fillCalendar(update, request, config, principal.account());
        workCalendarMapper.updateById(update);

        // 3、记录操作日志并返回最新日历。
        recordLog(principal, "UPDATE", id, "编辑工作日历：" + existing.getCalendarName());
        return toVO(workCalendarMapper.selectById(id));
    }

    /**
     * 设置或取消默认工作日历。
     */
    @Transactional
    public WorkCalendarVO updateDefault(CurrentUserPrincipal principal, Long id, WorkCalendarDefaultRequest request) {
        // 1、校验权限和日历存在性。
        permissionService.assertPermission(principal, "work_calendar:default", "无权设置默认工作日历");
        WorkCalendarEntity existing = requireCalendar(id);

        // 2、该接口只允许设置新的默认日历，不允许把系统置为无默认日历。
        if (!Boolean.TRUE.equals(request.getDefaultCalendar())) {
            throw new BusinessException(CODE_BAD_REQUEST, "系统必须保留一个默认工作日历");
        }
        workCalendarMapper.update(null, new LambdaUpdateWrapper<WorkCalendarEntity>()
                .ne(WorkCalendarEntity::getId, id)
                .eq(WorkCalendarEntity::getEnterpriseId, existing.getEnterpriseId())
                .eq(WorkCalendarEntity::getDefaultCalendar, true)
                .set(WorkCalendarEntity::getDefaultCalendar, false)
                .set(WorkCalendarEntity::getUpdatedBy, principal.account()));

        // 3、更新当前日历默认标识并记录日志。
        workCalendarMapper.update(null, new LambdaUpdateWrapper<WorkCalendarEntity>()
                .eq(WorkCalendarEntity::getId, id)
                .set(WorkCalendarEntity::getDefaultCalendar, true)
                .set(WorkCalendarEntity::getUpdatedBy, principal.account()));
        recordLog(principal, "SET_DEFAULT", id, "设为默认工作日历：" + existing.getCalendarName());

        // 4、返回最新日历。
        return toVO(workCalendarMapper.selectById(id));
    }

    /**
     * 删除工作日历。
     */
    @Transactional
    public void deleteCalendar(CurrentUserPrincipal principal, Long id) {
        // 1、校验权限和日历存在性。
        permissionService.assertPermission(principal, "work_calendar:delete", "无权删除工作日历");
        WorkCalendarEntity existing = requireCalendar(id);

        // 2、默认日历不能删除；已被 SLA 策略或节假日引用的日历也不能删除。
        if (Boolean.TRUE.equals(existing.getDefaultCalendar())) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认工作日历不能删除，请先设置其他默认日历");
        }
        long policyCount = slaPolicyMapper.selectCount(
                new LambdaQueryWrapper<SlaPolicyEntity>().eq(SlaPolicyEntity::getCalendarId, id));
        if (policyCount > 0) {
            throw new BusinessException(CODE_CONFLICT, "工作日历已被 SLA 策略引用，不能删除");
        }
        long holidayCount = holidayMapper.selectCount(
                new LambdaQueryWrapper<HolidayEntity>().eq(HolidayEntity::getCalendarId, id));
        if (holidayCount > 0) {
            throw new BusinessException(CODE_CONFLICT, "工作日历已配置节假日，不能删除");
        }

        // 3、逻辑删除并记录操作日志。
        workCalendarMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除工作日历：" + existing.getCalendarName());
    }

    private LambdaQueryWrapper<WorkCalendarEntity> buildQuery(Long enterpriseId, String keyword,
                                                               Boolean defaultCalendar) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<WorkCalendarEntity> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(WorkCalendarEntity::getEnterpriseId, enterpriseId);
        }
        if (!normalizedKeyword.isEmpty()) {
            wrapper.like(WorkCalendarEntity::getCalendarName, normalizedKeyword);
        }
        if (defaultCalendar != null) {
            wrapper.eq(WorkCalendarEntity::getDefaultCalendar, defaultCalendar);
        }
        return wrapper;
    }

    private WorkCalendarSummaryVO buildSummary(Long enterpriseId) {
        long total = workCalendarMapper.selectCount(buildQuery(enterpriseId, null, null));
        long defaultCount = workCalendarMapper.selectCount(
                buildQuery(enterpriseId, null, true));
        return new WorkCalendarSummaryVO(total, defaultCount);
    }

    private CalendarConfig validateCalendar(WorkCalendarSaveRequest request) {
        String timezone = normalize(request.getTimezone());
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BusinessException(CODE_BAD_REQUEST, "时区不合法：" + timezone);
        }

        String workdays = normalizeWorkdays(request.getWorkdays());
        LocalTime startTime = parseTime(request.getWorkStartTime(), "工作开始时间");
        LocalTime endTime = parseTime(request.getWorkEndTime(), "工作结束时间");
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(CODE_BAD_REQUEST, "工作开始时间必须早于工作结束时间");
        }
        return new CalendarConfig(timezone, workdays, startTime, endTime);
    }

    private String normalizeWorkdays(List<Integer> workdays) {
        if (workdays == null || workdays.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择工作日");
        }
        Set<Integer> uniqueDays = new LinkedHashSet<>();
        for (Integer day : workdays) {
            if (day == null || day < 1 || day > 7) {
                throw new BusinessException(CODE_BAD_REQUEST, "工作日仅支持 1-7");
            }
            if (!uniqueDays.add(day)) {
                throw new BusinessException(CODE_BAD_REQUEST, "工作日不能重复：" + day);
            }
        }
        return uniqueDays.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private LocalTime parseTime(String value, String fieldName) {
        String normalized = normalize(value);
        try {
            return LocalTime.parse(normalized.length() == 5 ? normalized : normalized.substring(0, 5), TIME_FORMATTER);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException exception) {
            throw new BusinessException(CODE_BAD_REQUEST, fieldName + "格式应为 HH:mm 或 HH:mm:ss");
        }
    }

    private void ensureDefaultCalendarUnique(Long enterpriseId, boolean defaultCalendar, Long excludeId) {
        if (!defaultCalendar) {
            return;
        }
        LambdaQueryWrapper<WorkCalendarEntity> wrapper = new LambdaQueryWrapper<WorkCalendarEntity>()
                .eq(WorkCalendarEntity::getEnterpriseId, enterpriseId)
                .eq(WorkCalendarEntity::getDefaultCalendar, true);
        if (excludeId != null) {
            wrapper.ne(WorkCalendarEntity::getId, excludeId);
        }
        Long count = workCalendarMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "默认工作日历已存在，请先编辑原默认日历");
        }
    }

    private void assertEnterpriseEnabled(Long enterpriseId) {
        EnterpriseEntity enterprise = enterpriseId == null ? null : enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业不存在");
        }
        if (!Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业已停用，不能配置工作日历");
        }
    }

    private boolean isReferenced(Long calendarId) {
        return slaPolicyMapper.selectCount(new LambdaQueryWrapper<SlaPolicyEntity>()
                .eq(SlaPolicyEntity::getCalendarId, calendarId)) > 0
                || holidayMapper.selectCount(new LambdaQueryWrapper<HolidayEntity>()
                .eq(HolidayEntity::getCalendarId, calendarId)) > 0;
    }

    private WorkCalendarEntity requireCalendar(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工作日历ID不能为空");
        }
        WorkCalendarEntity calendar = workCalendarMapper.selectById(id);
        if (calendar == null) {
            throw new BusinessException(CODE_NOT_FOUND, "工作日历不存在");
        }
        return calendar;
    }

    private void fillCalendar(WorkCalendarEntity calendar, WorkCalendarSaveRequest request,
                              CalendarConfig config, String operator) {
        calendar.setEnterpriseId(request.getEnterpriseId());
        calendar.setCalendarName(normalize(request.getCalendarName()));
        calendar.setTimezone(config.timezone());
        calendar.setWorkdays(config.workdays());
        calendar.setWorkStartTime(config.startTime());
        calendar.setWorkEndTime(config.endTime());
        calendar.setDefaultCalendar(Boolean.TRUE.equals(request.getDefaultCalendar()));
        calendar.setUpdatedBy(operator);
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_WORK_CALENDAR);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private WorkCalendarVO toVO(WorkCalendarEntity calendar) {
        return new WorkCalendarVO(
                calendar.getId(),
                calendar.getEnterpriseId(),
                calendar.getCalendarName(),
                calendar.getTimezone(),
                parseWorkdays(calendar.getWorkdays()),
                formatTime(calendar.getWorkStartTime()),
                formatTime(calendar.getWorkEndTime()),
                calendar.getDefaultCalendar(),
                calendar.getCreatedAt(),
                calendar.getUpdatedAt()
        );
    }

    private List<Integer> parseWorkdays(String workdays) {
        String normalized = normalize(workdays);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return List.of(normalized.split(",")).stream()
                .map(Integer::valueOf)
                .toList();
    }

    private String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record CalendarConfig(String timezone, String workdays, LocalTime startTime, LocalTime endTime) {
    }
}
