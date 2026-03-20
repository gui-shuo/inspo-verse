package com.inspoverse.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创意工坊项目实体
 */
@Data
@TableName("workshop_project")
public class WorkshopProject {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectNo;
    private Long userId;
    private String title;
    private String description;
    private String coverUrl;
    private String tags;
    private String version;
    private String fileUrl;
    private String fileSize;
    private String category;
    private Integer visibility;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer downloadCount;
    private Integer ratingSum;
    private Integer ratingCount;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
