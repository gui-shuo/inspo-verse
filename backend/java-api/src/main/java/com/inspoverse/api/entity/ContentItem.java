package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 内容实体
 */
@Data
@TableName("content_items")
public class ContentItem {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String contentNo;
  private Long userId;
  private String category;
  private String title;
  private String description;
  private String coverUrl;
  private String tag;
  private Integer likeCount;
  private Integer commentCount;
  private Integer viewCount;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
