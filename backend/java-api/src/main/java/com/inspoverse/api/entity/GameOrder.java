package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 游戏购买订单实体
 * status: PENDING / PAID / EXPIRED / FAILED
 * payMethod: ALIPAY / WECHAT
 */
@Data
@TableName("game_orders")
public class GameOrder {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String orderNo;
  private Long userId;
  private Long gameId;
  private String gameTitle;
  private Integer amountCents;
  private String payMethod;
  private String status;
  private String payUrl;

  @TableField(exist = false)
  private String qrCodeDataUrl;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  private LocalDateTime expiredAt;
  private LocalDateTime paidAt;
}
