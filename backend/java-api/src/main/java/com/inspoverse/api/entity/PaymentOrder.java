package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值支付订单实体
 * status: PENDING（待支付）/ PAID（已支付）/ EXPIRED（已过期）/ FAILED（支付失败）
 * payMethod: ALIPAY（支付宝）/ WECHAT（微信支付）
 */
@Data
@TableName("payment_orders")
public class PaymentOrder {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 商户订单号 */
  private String orderNo;

  /** 充值套餐 ID */
  private String packageId;

  /** 支付金额（元），精确到分 */
  private BigDecimal amount;

  /** 购买灵感点数 */
  private Integer points;

  /** 支付方式：ALIPAY / WECHAT */
  private String payMethod;

  /** 订单状态：PENDING / PAID / EXPIRED / FAILED */
  private String status;

  /**
   * 支付二维码 URL（或 mock 标识）
   * 生产环境：支付宝 Native 二维码链接 / 微信 Native 二维码链接
   * 开发模式：MOCK:{orderNo}
   */
  private String payUrl;

  /** 操作系统生成的 Base64 QR 图片（自动生成，不存 DB，供 transient 使用） */
  @TableField(exist = false)
  private String qrCodeDataUrl;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  /** 过期时间（默认 5 分钟） */
  private LocalDateTime expiredAt;

  /** 实际支付时间 */
  private LocalDateTime paidAt;
}
