package com.ntn.fziot.mailtrace.application.bizservice.holiday;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidayListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidaySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidaySummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidayVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String MODULE_HOLIDAY = "HOLIDAY";

    private final HolidayMapper holidayMapper;
    private final WorkCalendarMapper workCalendarMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 查询节假日列表。
     */
    public HolidayListResponse listHolidays(CurrentUserPrincipal principal, Long calendarId, String keyword,
                                            LocalDate dateFrom, LocalDate dateTo) {
        // 1、仅管理员可进入节假日配置。
        assertAdmin(principal);

        // 2、校验日期范围，并按日历、名称、日期范围筛选。
        validateDateRange(dateFrom, dateTo);
        LambdaQueryWrapper<HolidayEntity> wrapper = buildQuery(calendarId, keyword, dateFrom, dateTo)
                .orderByAsc(HolidayEntity::getHolidayDate)
                .orderByAsc(HolidayEntity::getId);
        List<HolidayVO> records = holidayMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();

        // 3、返回列表和当前筛选条件下的总数，供前端配置页展示。
        long total = holidayMapper.selectCount(buildQuery(calendarId, keyword, dateFrom, dateTo));
        return new HolidayListResponse(records, new HolidaySummaryVO(total));
    }

    /**
     * 新建节假日。
     */
    @Transactional
    public HolidayVO createHoliday(CurrentUserPrincipal principal, HolidaySaveRequest request) {
        // 1、校验管理员权限、工作日历存在性和节假日日期唯一性。
        assertAdmin(principal);
        validateHoliday(request);
        ensureCalendarExists(request.getCalendarId());
        ensureHolidayDateUnique(request.getCalendarId(), request.getHolidayDate(), null);

        // 2、写入节假日主体。
        HolidayEntity holiday = new HolidayEntity();
        fillHoliday(holiday, request, principal.account());
        holiday.setCreatedBy(principal.account());
        holiday.setUpdatedBy(principal.account());
        holidayMapper.insert(holiday);

        // 3、记录配置变更日志并返回最新数据。
        recordLog(principal, "CREATE", holiday.getId(), "新建节假日：" + holiday.getHolidayName());
        return toVO(holidayMapper.selectById(holiday.getId()));
    }

    /**
     * 编辑节假日。
     */
    @Transactional
    public HolidayVO updateHoliday(CurrentUserPrincipal principal, Long id, HolidaySaveRequest request) {
        // 1、校验权限、节假日存在性、工作日历存在性和日期唯一性。
        assertAdmin(principal);
        HolidayEntity existing = requireHoliday(id);
        validateHoliday(request);
        ensureCalendarExists(request.getCalendarId());
        ensureHolidayDateUnique(request.getCalendarId(), request.getHolidayDate(), id);

        // 2、覆盖可编辑的日历、日期和名称。
        HolidayEntity update = new HolidayEntity();
        update.setId(id);
        fillHoliday(update, request, principal.account());
        holidayMapper.updateById(update);

        // 3、记录操作日志并返回最新节假日。
        recordLog(principal, "UPDATE", id, "编辑节假日：" + existing.getHolidayName());
        return toVO(holidayMapper.selectById(id));
    }

    /**
     * 删除节假日。
     */
    @Transactional
    public void deleteHoliday(CurrentUserPrincipal principal, Long id) {
        // 1、校验权限和节假日存在性。
        assertAdmin(principal);
        HolidayEntity existing = requireHoliday(id);

        // 2、逻辑删除节假日。
        holidayMapper.deleteById(id);

        // 3、记录操作日志，供配置审计追踪。
        recordLog(principal, "DELETE", id, "删除节假日：" + existing.getHolidayName());
    }

    private LambdaQueryWrapper<HolidayEntity> buildQuery(Long calendarId, String keyword,
                                                         LocalDate dateFrom, LocalDate dateTo) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<HolidayEntity> wrapper = new LambdaQueryWrapper<>();
        if (calendarId != null) {
            wrapper.eq(HolidayEntity::getCalendarId, calendarId);
        }
        if (!normalizedKeyword.isEmpty()) {
            wrapper.like(HolidayEntity::getHolidayName, normalizedKeyword);
        }
        if (dateFrom != null) {
            wrapper.ge(HolidayEntity::getHolidayDate, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(HolidayEntity::getHolidayDate, dateTo);
        }
        return wrapper;
    }

    private void validateHoliday(HolidaySaveRequest request) {
        if (request.getCalendarId() == null || request.getCalendarId() <= 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择工作日历");
        }
        if (request.getHolidayDate() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择节假日日期");
        }
        if (normalize(request.getHolidayName()).isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入节假日名称");
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(CODE_BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    private void ensureCalendarExists(Long calendarId) {
        if (workCalendarMapper.selectById(calendarId) == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工作日历不存在");
        }
    }

    private void ensureHolidayDateUnique(Long calendarId, LocalDate holidayDate, Long excludeId) {
        LambdaQueryWrapper<HolidayEntity> wrapper = new LambdaQueryWrapper<HolidayEntity>()
                .eq(HolidayEntity::getCalendarId, calendarId)
                .eq(HolidayEntity::getHolidayDate, holidayDate);
        if (excludeId != null) {
            wrapper.ne(HolidayEntity::getId, excludeId);
        }
        Long count = holidayMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "当前日历下该节假日日期已存在");
        }
    }

    private HolidayEntity requireHoliday(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "节假日ID不能为空");
        }
        HolidayEntity holiday = holidayMapper.selectById(id);
        if (holiday == null) {
            throw new BusinessException(CODE_NOT_FOUND, "节假日不存在");
        }
        return holiday;
    }

    private void fillHoliday(HolidayEntity holiday, HolidaySaveRequest request, String operator) {
        holiday.setCalendarId(request.getCalendarId());
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setHolidayName(normalize(request.getHolidayName()));
        holiday.setUpdatedBy(operator);
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_HOLIDAY);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private HolidayVO toVO(HolidayEntity holiday) {
        return new HolidayVO(
                holiday.getId(),
                holiday.getCalendarId(),
                holiday.getHolidayDate(),
                holiday.getHolidayName(),
                holiday.getCreatedAt(),
                holiday.getUpdatedAt()
        );
    }

    private void assertAdmin(CurrentUserPrincipal principal) {
        if (principal == null || !ROLE_ADMIN.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员可操作节假日");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
