package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_role_rel")
public class UserRoleRel {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long roleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
