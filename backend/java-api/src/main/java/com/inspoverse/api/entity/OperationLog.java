package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long operatorUserId;
    private String module;
    private String action;
    private String requestUri;
    private String requestMethod;
    private String responseCode;
    private Integer costMs;
    private String ip;
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
