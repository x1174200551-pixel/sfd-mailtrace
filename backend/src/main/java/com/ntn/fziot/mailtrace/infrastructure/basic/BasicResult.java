package com.ntn.fziot.mailtrace.infrastructure.basic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "统一响应")
public class BasicResult<T> {

    @Schema(description = "业务码，0 表示成功")
    private int code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "链路追踪 ID")
    private String traceId;

    @Schema(description = "服务端时间")
    private OffsetDateTime serverTime;

    public static <T> BasicResult<T> ok(T data) {
        BasicResult<T> result = new BasicResult<>();
        result.setCode(0);
        result.setMessage("ok");
        result.setData(data);
        result.setServerTime(OffsetDateTime.now());
        return result;
    }

    public static BasicResult<Void> ok() {
        return ok(null);
    }

    public static <T> BasicResult<T> fail(int code, String message) {
        BasicResult<T> result = new BasicResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setServerTime(OffsetDateTime.now());
        return result;
    }
}
