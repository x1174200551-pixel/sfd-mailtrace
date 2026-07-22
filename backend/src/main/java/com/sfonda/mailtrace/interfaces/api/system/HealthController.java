package com.sfonda.mailtrace.interfaces.api.system;

import com.sfonda.mailtrace.infrastructure.basic.BasicResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统", description = "健康检查与系统信息")
@RestController
@RequestMapping("/v1/system")
public class HealthController {

    @Operation(summary = "健康检查", description = "用于确认服务已启动")
    @GetMapping("/health")
    public BasicResult<HealthVO> health() {
        HealthVO vo = new HealthVO();
        vo.setStatus("UP");
        vo.setApp("MailTrace");
        vo.setVersion("0.1.0-SNAPSHOT");
        return BasicResult.ok(vo);
    }

    @Data
    @Schema(description = "健康检查响应")
    public static class HealthVO {
        @Schema(description = "状态")
        private String status;
        @Schema(description = "应用名")
        private String app;
        @Schema(description = "版本")
        private String version;
    }
}
