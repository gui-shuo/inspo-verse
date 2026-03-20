package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工坊项目订阅实体
 */
@Data
@TableName("workshop_subscription")
public class WorkshopSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long projectId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
