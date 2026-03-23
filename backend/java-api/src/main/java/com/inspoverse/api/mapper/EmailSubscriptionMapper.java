package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.EmailSubscription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailSubscriptionMapper extends BaseMapper<EmailSubscription> {
}
