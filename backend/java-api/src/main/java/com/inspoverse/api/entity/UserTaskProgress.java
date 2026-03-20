package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_task_progress")
public class UserTaskProgress {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String taskCode;
  private LocalDate taskDate;
  private Integer progress;
  private Integer completed;
  private Integer rewarded;
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
}
