package com.deltatrade.platform.modules.listing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountListingMapper extends BaseMapper<AccountListingDO> {
}
