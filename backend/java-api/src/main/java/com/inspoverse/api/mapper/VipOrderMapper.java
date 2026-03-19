package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.VipOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP订单Mapper
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {
}
