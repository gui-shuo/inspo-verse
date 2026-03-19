package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP订单实体
 */
@Data
@TableName("vip_orders")
public class VipOrder {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String orderNo;
  private Long userId;
  private Long vipPlanId;
  private Integer amountCents;
  private String payChannel;
  private Integer payStatus;
  private LocalDateTime paidAt;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
