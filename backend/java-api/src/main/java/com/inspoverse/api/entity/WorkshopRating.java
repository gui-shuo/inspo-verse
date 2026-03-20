package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工坊项目评分实体
 */
@Data
@TableName("workshop_rating")
public class WorkshopRating {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long projectId;
    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
