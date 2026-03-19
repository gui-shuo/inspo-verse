package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户灵感点数钱包实体
 */
@Data
@TableName("user_points")
public class UserPoints {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Integer balance;
  private Integer totalEarned;
  private Integer totalSpent;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
