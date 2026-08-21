package com.ntn.fziot.mailtrace.application.bizservice.sysparam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.sysparam.TicketNumberRuleRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sysparam.TicketNumberRuleVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SysParamEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SysParamMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketNumberRuleService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final String PARAM_ENABLED = "ticket.no.enabled";
    private static final String PARAM_PREFIX = "ticket.no.prefix";
    private static final String PARAM_DATE_FORMAT = "ticket.no.date_format";
    private static final String PARAM_SEQ_LENGTH = "ticket.no.seq_length";
    private static final String PARAM_SEPARATOR = "ticket.no.separator";
    private static final String PARAM_DESCRIPTION = "ticket.no.description";
    private static final String DEFAULT_DESCRIPTION = "客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。";
    private static final int MAX_GENERATE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysParamMapper sysParamMapper;
    private final TicketMapper ticketMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;

    /**
     * 查询当前工单编号规则，并计算下一工单号预览。
     */
    public TicketNumberRuleVO getRule(CurrentUserPrincipal principal) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "ticket_number_rule:read", "无权查看编号规则");
        // 2、读取系统参数表中的编号规则配置
        TicketNumberRuleConfig config = loadConfig();
        // 3、按当前规则组装预览
        return buildVO(config);
    }

    /**
     * 根据页面输入临时生成下一工单号预览，不写入数据库。
     */
    public TicketNumberRuleVO previewRule(CurrentUserPrincipal principal, TicketNumberRuleRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "ticket_number_rule:preview", "无权预览编号规则");
        // 2、规范化并校验页面传入的编号规则
        TicketNumberRuleConfig config = toConfig(request);
        // 3、按规则返回预览结果
        return buildVO(config);
    }

    /**
     * 保存工单编号规则，保存后仅影响后续新建工单。
     */
    @Transactional
    public TicketNumberRuleVO updateRule(CurrentUserPrincipal principal, TicketNumberRuleRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "ticket_number_rule:update", "无权编辑编号规则");
        // 2、规范化并校验页面传入的编号规则
        TicketNumberRuleConfig config = toConfig(request);
        // 3、逐项写入系统参数表，业务页面不暴露内部参数键
        upsertParam(PARAM_ENABLED, String.valueOf(config.enabled()), "工单编号规则启用状态", principal.account());
        upsertParam(PARAM_PREFIX, config.prefix(), "工单号前缀", principal.account());
        upsertParam(PARAM_DATE_FORMAT, config.dateFormat(), "工单号日期格式", principal.account());
        upsertParam(PARAM_SEQ_LENGTH, String.valueOf(config.seqLength()), "工单号随机数位数", principal.account());
        upsertParam(PARAM_SEPARATOR, config.separator(), "工单号分隔符", principal.account());
        upsertParam(PARAM_DESCRIPTION, config.description(), "工单编号规则业务说明", principal.account());
        // 4、写入操作日志，便于后续审计
        recordLog(principal, "UPDATE", PARAM_PREFIX, "保存工单编号规则：" + config.previewTicketNo());
        // 5、返回保存后的最新预览结果
        return getRule(principal);
    }

    private TicketNumberRuleConfig loadConfig() {
        List<SysParamEntity> params = sysParamMapper.selectList(new LambdaQueryWrapper<SysParamEntity>()
                .in(SysParamEntity::getParamKey, List.of(
                        PARAM_ENABLED,
                        PARAM_PREFIX,
                        PARAM_DATE_FORMAT,
                        PARAM_SEQ_LENGTH,
                        PARAM_SEPARATOR,
                        PARAM_DESCRIPTION
                )));
        Map<String, SysParamEntity> paramMap = new HashMap<>();
        for (SysParamEntity param : params) {
            paramMap.put(param.getParamKey(), param);
        }
        return new TicketNumberRuleConfig(
                Boolean.parseBoolean(valueOf(paramMap, PARAM_ENABLED, "true")),
                normalizePrefix(valueOf(paramMap, PARAM_PREFIX, "TCK")),
                normalizeDateFormat(valueOf(paramMap, PARAM_DATE_FORMAT, "yyMMddHHmmss")),
                normalizeSeqLength(valueOf(paramMap, PARAM_SEQ_LENGTH, "6")),
                normalizeSeparator(valueOf(paramMap, PARAM_SEPARATOR, "-")),
                normalizeDescription(valueOf(paramMap, PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)),
                latestUpdatedAt(paramMap)
        );
    }

    private TicketNumberRuleConfig toConfig(TicketNumberRuleRequest request) {
        if (request == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "编号规则不能为空");
        }
        return new TicketNumberRuleConfig(
                Boolean.TRUE.equals(request.getEnabled()),
                normalizePrefix(request.getPrefix()),
                normalizeDateFormat(request.getDateFormat()),
                normalizeSeqLength(String.valueOf(request.getSeqLength())),
                normalizeSeparator(request.getSeparator()),
                normalizeDescription(request.getDescription()),
                LocalDateTime.now()
        );
    }

    private TicketNumberRuleVO buildVO(TicketNumberRuleConfig config) {
        LocalDateTime now = LocalDateTime.now();
        String dateKey = config.dateKey(now);
        String nextSeq = randomDigits(config.seqLength());
        String nextTicketNo = config.composeTicketNo(dateKey, nextSeq);
        return new TicketNumberRuleVO(
                config.enabled(),
                config.prefix(),
                config.dateFormat(),
                config.seqLength(),
                config.separator(),
                config.description(),
                now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                dateKey,
                0,
                nextSeq,
                nextTicketNo,
                "Re: " + nextTicketNo,
                config.updatedAt()
        );
    }

    private void upsertParam(String key, String value, String description, String operator) {
        SysParamEntity existing = sysParamMapper.selectOne(new LambdaQueryWrapper<SysParamEntity>()
                .eq(SysParamEntity::getParamKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            SysParamEntity entity = new SysParamEntity();
            entity.setParamKey(key);
            entity.setParamValue(value);
            entity.setParamDesc(description);
            entity.setCreatedBy(operator);
            entity.setUpdatedBy(operator);
            sysParamMapper.insert(entity);
            return;
        }
        sysParamMapper.update(null, new LambdaUpdateWrapper<SysParamEntity>()
                .eq(SysParamEntity::getId, existing.getId())
                .set(SysParamEntity::getParamValue, value)
                .set(SysParamEntity::getParamDesc, description)
                .set(SysParamEntity::getUpdatedBy, operator));
    }

    /**
     * 生成下一工单号，供工单创建时调用。
     * 该方法不是事务性的——调用方应在外层事务中调用。
     */
    @Transactional
    public String generateNextTicketNo() {
        TicketNumberRuleConfig config = loadConfig();
        if (!config.enabled()) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单编号规则未启用，请联系管理员配置");
        }
        LocalDateTime now = LocalDateTime.now();
        String dateKey = config.dateKey(now);
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String ticketNo = config.composeTicketNo(dateKey, randomDigits(config.seqLength()));
            if (!existsTicketNo(ticketNo)) {
                return ticketNo;
            }
        }
        throw new BusinessException(CODE_BAD_REQUEST, "工单号生成冲突，请稍后重试");
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, String bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode("SYSTEM");
        log.setActionCode(actionCode);
        log.setBizId(bizId);
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private String valueOf(Map<String, SysParamEntity> paramMap, String key, String defaultValue) {
        SysParamEntity param = paramMap.get(key);
        return param == null || normalize(param.getParamValue()).isEmpty() ? defaultValue : param.getParamValue();
    }

    private LocalDateTime latestUpdatedAt(Map<String, SysParamEntity> paramMap) {
        return paramMap.values().stream()
                .map(SysParamEntity::getUpdatedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String normalizePrefix(String value) {
        String prefix = normalize(value).toUpperCase();
        if (!prefix.matches("^[A-Z0-9]{2,8}$")) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单前缀仅支持 2-8 位大写英文和数字");
        }
        return prefix;
    }

    private String normalizeDateFormat(String value) {
        String dateFormat = normalize(value);
        if (!List.of("yyMMddHHmmss", "yyyyMMdd", "yyyyMM", "yyyy").contains(dateFormat)) {
            throw new BusinessException(CODE_BAD_REQUEST, "日期格式仅支持 yyMMddHHmmss、yyyyMMdd、yyyyMM 或 yyyy");
        }
        return dateFormat;
    }

    private int normalizeSeqLength(String value) {
        try {
            int seqLength = Integer.parseInt(normalize(value));
            if (seqLength < 1 || seqLength > 6) {
                throw new BusinessException(CODE_BAD_REQUEST, "随机数位数需为 1-6 位");
            }
            return seqLength;
        } catch (NumberFormatException exception) {
            throw new BusinessException(CODE_BAD_REQUEST, "随机数位数需为数字");
        }
    }

    private String normalizeSeparator(String value) {
        String separator = normalize(value);
        if (!separator.isEmpty() && !"-".equals(separator) && !"_".equals(separator)) {
            throw new BusinessException(CODE_BAD_REQUEST, "分隔符仅支持短横线、下划线或不设置");
        }
        return separator;
    }

    private String normalizeDescription(String value) {
        String description = normalize(value);
        return description.isEmpty() ? DEFAULT_DESCRIPTION : description;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private boolean existsTicketNo(String ticketNo) {
        Long count = ticketMapper.selectCount(new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getTicketNo, ticketNo));
        return count != null && count > 0;
    }

    private static final class TicketNumberRuleConfig {
        private final boolean enabled;
        private final String prefix;
        private final String dateFormat;
        private final int seqLength;
        private final String separator;
        private final String description;
        private final LocalDateTime updatedAt;

        private TicketNumberRuleConfig(boolean enabled, String prefix, String dateFormat,
                                       int seqLength, String separator, String description,
                                       LocalDateTime updatedAt) {
            this.enabled = enabled;
            this.prefix = prefix;
            this.dateFormat = dateFormat;
            this.seqLength = seqLength;
            this.separator = separator;
            this.description = description;
            this.updatedAt = updatedAt;
        }

        private String dateKey(LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern(dateFormat));
        }

        private String composeTicketNo(String dateKey, String nextSeq) {
            return List.of(prefix, dateKey, nextSeq).stream()
                    .filter(value -> value != null && !value.isEmpty())
                    .reduce((left, right) -> left + separator + right)
                    .orElse(prefix);
        }

        private String previewTicketNo() {
            String dateKey = dateKey(LocalDateTime.now());
            String nextSeq = "0".repeat(seqLength);
            return composeTicketNo(dateKey, nextSeq);
        }

        // --- 类 record 的访问器 ---
        boolean enabled() { return enabled; }
        String prefix() { return prefix; }
        String dateFormat() { return dateFormat; }
        int seqLength() { return seqLength; }
        String separator() { return separator; }
        String description() { return description; }
        LocalDateTime updatedAt() { return updatedAt; }
    }
}
