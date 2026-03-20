package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_experience")
public class UserExperience {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private Integer expPoints;
  private Integer level;
  private String levelName;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
