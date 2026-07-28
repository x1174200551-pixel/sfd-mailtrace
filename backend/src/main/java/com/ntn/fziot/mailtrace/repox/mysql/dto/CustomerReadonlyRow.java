package com.ntn.fziot.mailtrace.repox.mysql.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerReadonlyRow {

    private Long id;

    private String email;

    private String displayName;

    private LocalDateTime lastMailAt;

    private Long ticketCount;

    private String remark;

    private LocalDateTime createdAt;
}
