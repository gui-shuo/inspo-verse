package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.AIChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI会话Mapper
 */
@Mapper
public interface AIChatSessionMapper extends BaseMapper<AIChatSession> {
}
