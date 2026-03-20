package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("anime_series")
public class AnimeSeries {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String seriesNo;
  private String title;
  private String description;
  private String coverUrl;
  private String heroUrl;
  private String authorName;
  private Long userId;
  private BigDecimal score;
  private Integer scheduleDay;
  private String updateTime;
  private String currentEpisode;
  private String status;
  private Integer isPaid;
  private Integer freeEpisodes;
  private Integer priceCents;
  private Integer totalEpisodes;
  private Integer viewCount;
  private Integer subscribeCount;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
