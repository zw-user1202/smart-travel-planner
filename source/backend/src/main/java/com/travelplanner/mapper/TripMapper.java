package com.travelplanner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplanner.entity.Trip;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TripMapper extends BaseMapper<Trip> {
}
