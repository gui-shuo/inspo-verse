package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 第三方OAuth绑定实体
 */
@Data
@TableName("user_oauth_bindings")
public class UserOAuthBinding {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String provider;          // github / discord / google
  private String providerUserId;
  private String providerUsername;
  private String accessToken;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
