package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.AIChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI消息Mapper
 */
@Mapper
public interface AIChatMessageMapper extends BaseMapper<AIChatMessage> {
}
