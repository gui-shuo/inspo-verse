package com.inspoverse.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspoverse.api.entity.DailyTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyTaskMapper extends BaseMapper<DailyTask> {
}
