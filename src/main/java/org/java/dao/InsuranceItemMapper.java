package org.java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

/**
 * 险种项数据接口
 * @author Haowen Tian
 */
@Mapper
@Component
public interface InsuranceItemMapper {


    /**
     * 修改指定险种项的保额和保费
     * @param items_id 险种项编号
     * @param insured_amount 险种项保额
     * @param premium 险种项保费
     */
    public void updateInsuranceItemPriceByItemId(@Param("item_id") String items_id,@Param("insured_amount") double insured_amount,@Param("premium") double premium);
}
