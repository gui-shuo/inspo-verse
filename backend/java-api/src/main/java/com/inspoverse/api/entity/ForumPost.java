package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子实体
 */
@Data
@TableName("forum_post")
public class ForumPost {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String postNo;
  private Long userId;
  private String category;
  private String title;
  private String content;
  private String tags;
  private Integer viewCount;
  private Integer likeCount;
  private Integer commentCount;
  private Integer isTop;
  private Integer isEssence;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
