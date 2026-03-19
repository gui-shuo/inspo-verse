package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户创作实体
 */
@Data
@TableName("user_creations")
public class UserCreation {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String title;
  private String description;
  private String fileUrl;
  private String coverUrl;
  private String fileType;
  private Long fileSize;
  private Integer visibility;  // 0=私密 1=公开
  private Integer status;      // 1=正常 0=已删除

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
