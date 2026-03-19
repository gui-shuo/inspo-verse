package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP会员有效期实体
 */
@Data
@TableName("vip_memberships")
public class VipMembership {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Integer vipLevel;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String sourceOrderNo;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
