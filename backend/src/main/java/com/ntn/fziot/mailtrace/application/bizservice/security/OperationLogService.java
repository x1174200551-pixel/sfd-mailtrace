package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计日志服务。
 * <p>
 * 集中管理所有业务模块的操作日志写入，避免各 Service 各自定义重复的 recordLog() 方法。
 * 自动从当前请求上下文捕获 requestUri 和 requestIp。
 * </p>
 */
@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录一条操作审计日志。
     *
     * @param principal  当前登录用户
     * @param moduleCode 模块编码（如 ROLE, DEPARTMENT, USER 等）
     * @param actionCode 动作编码（如 CREATE, UPDATE, ENABLE, DISABLE 等）
     * @param bizId      业务主键 ID
     * @param content    操作内容描述
     */
    public void record(CurrentUserPrincipal principal, String moduleCode, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(moduleCode);
        log.setActionCode(actionCode);
        log.setBizId(bizId != null ? String.valueOf(bizId) : null);
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());

        // 尝试从请求上下文获取 requestUri 和 requestIp
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                log.setRequestUri(request.getRequestURI());
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                // X-Forwarded-For 可能是逗号分隔的多个 IP，取第一个
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.setRequestIp(ip);
            }
        } catch (Exception ignored) {
            // 非 Web 上下文（如定时任务）不设置 requestUri/requestIp
        }

        operationLogMapper.insert(log);
    }
}
