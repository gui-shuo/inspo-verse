package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI聊天消息实体
 */
@Data
@TableName("ai_chat_message")
public class AIChatMessage {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long sessionId;
  private Long userId;
  private String role;
  private String content;
  private String contentMd;
  private Integer tokens;
  private Integer latencyMs;
  private Integer safetyFlag;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
