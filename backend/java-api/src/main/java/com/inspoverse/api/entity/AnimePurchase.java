package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anime_purchases")
public class AnimePurchase {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Long animeId;
  private String orderNo;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
