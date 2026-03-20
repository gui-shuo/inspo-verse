package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("daily_tasks")
public class DailyTask {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String taskCode;
  private String taskName;
  private String description;
  private Integer rewardPoints;
  private Integer rewardExp;
  private Integer dailyLimit;
  private String taskType;
  private String routePath;
  private Integer sortOrder;
  private Integer status;
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
