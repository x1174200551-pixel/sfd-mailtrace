package com.sfonda.mailtrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MailtraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailtraceApplication.class, args);
    }
}
