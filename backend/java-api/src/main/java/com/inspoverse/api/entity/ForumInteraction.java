package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛互动实体（点赞/收藏/举报）
 */
@Data
@TableName("forum_interaction")
public class ForumInteraction {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String targetType;
  private Long targetId;
  private String actionType;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
