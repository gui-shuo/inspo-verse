package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 热门游戏实体
 */
@Data
@TableName("games")
public class Game {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String gameNo;
  private Long userId;
  private String title;
  private String genre;
  private String description;
  private String coverUrl;
  private String heroUrl;
  private String gameUrl;
  private String tags;
  private String developer;
  private String releaseDate;
  private BigDecimal rating;
  private Integer ratingCount;
  private Integer playCount;
  private Integer favoriteCount;
  private Integer isPaid;
  private Integer priceCents;
  private Integer trialMinutes;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
