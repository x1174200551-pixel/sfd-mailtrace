package com.sfonda.mailtrace.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.sfonda.mailtrace.repox.mysql.mapper")
public class MybatisPlusConfig {
}
