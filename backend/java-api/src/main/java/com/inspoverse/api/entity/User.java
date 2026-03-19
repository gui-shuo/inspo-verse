package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  private String email;
  private String phone;
  private String passwordHash;
  private String nickname;
  private String avatarUrl;
  private Integer status;
  private LocalDateTime lastLoginAt;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  @TableLogic
  private Integer isDeleted;
}
