package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.ForumComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛评论Mapper
 */
@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumComment> {
}
