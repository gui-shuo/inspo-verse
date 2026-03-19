package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛帖子Mapper
 */
@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {
}
