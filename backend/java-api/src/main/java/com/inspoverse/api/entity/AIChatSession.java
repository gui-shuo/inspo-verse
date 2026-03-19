package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI聊天会话实体
 */
@Data
@TableName("ai_chat_session")
public class AIChatSession {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String sessionNo;
  private Long userId;
  private String title;
  private String modelName;
  private String scene;
  private Integer tokenUsed;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
