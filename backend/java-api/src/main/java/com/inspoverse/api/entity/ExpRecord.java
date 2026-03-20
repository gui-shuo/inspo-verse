package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exp_records")
public class ExpRecord {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private Integer amount;
  private String source;
  private String description;
  private String refId;
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
