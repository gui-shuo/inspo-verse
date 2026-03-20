package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anime_subscriptions")
public class AnimeSubscription {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Long animeId;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
