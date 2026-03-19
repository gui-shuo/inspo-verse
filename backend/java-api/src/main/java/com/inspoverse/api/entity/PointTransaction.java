package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 灵感点数流水实体
 */
@Data
@TableName("point_transactions")
public class PointTransaction {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Integer amount;       // 正为收入，负为支出
  private String type;          // EARN_SIGNIN / EARN_RECHARGE / EARN_VIP_GIFT / SPEND_AI / SPEND_REDEEM
  private String description;
  private Integer balanceAfter;
  private String refId;         // 关联业务单号

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
