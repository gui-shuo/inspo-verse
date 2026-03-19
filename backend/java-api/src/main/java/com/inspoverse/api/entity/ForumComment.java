package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛评论实体
 */
@Data
@TableName("forum_comment")
public class ForumComment {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long postId;
  private Long userId;
  private Long parentCommentId;
  private Long replyToUserId;
  private String content;
  private Integer likeCount;
  private Integer status;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
