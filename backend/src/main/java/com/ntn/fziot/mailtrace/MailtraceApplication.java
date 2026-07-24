package com.ntn.fziot.mailtrace;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class MailtraceApplication {

    @PostConstruct
    public void init() {
        log.info("JVM 默认时区: {}, 当前时间: {}", TimeZone.getDefault().getDisplayName(),
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new java.util.Date()));
    }

    public static void main(String[] args) {
        // 强制设置时区为东八区，必须在 Spring 启动前执行
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(MailtraceApplication.class, args);
    }
}
