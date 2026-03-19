package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP套餐实体
 */
@Data
@TableName("vip_plans")
public class VipPlan {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String planCode;
  private String planName;
  private Integer priceCents;
  private Integer durationDays;
  private Integer level;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
