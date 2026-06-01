package com.deltatrade.platform.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUserDO> {
}
