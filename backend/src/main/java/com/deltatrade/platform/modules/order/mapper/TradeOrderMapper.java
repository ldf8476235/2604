package com.deltatrade.platform.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrderDO> {
}
