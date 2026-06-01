package com.deltatrade.platform.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.deltatrade.platform.modules.im.model.ImMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessageDO> {
}
