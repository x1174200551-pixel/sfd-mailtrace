package com.ntn.fziot.mailtrace.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mailtraceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MailTrace API")
                        .description("邮迹工单系统接口文档")
                        .version("0.1.0"));
    }
}
