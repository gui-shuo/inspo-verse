package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anime_orders")
public class AnimeOrder {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String orderNo;
  private Long userId;
  private Long animeId;
  private String animeTitle;
  private Integer amountCents;
  private String payMethod;
  private String status;
  private String payUrl;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  private LocalDateTime expiredAt;
  private LocalDateTime paidAt;

  @TableLogic
  private Integer isDeleted;

  /** Base64 QR 图片，不存 DB */
  @TableField(exist = false)
  private String qrCodeDataUrl;
}
