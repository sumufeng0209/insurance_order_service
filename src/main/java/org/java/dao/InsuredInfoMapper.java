package org.java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 被保人信息数据接口
 * @author Haowen Tian
 */
@Mapper
@Component
public interface InsuredInfoMapper {


    /**
     * 通过订单编号查询被保人信息
     * @param order_id
     * @return
     */
    public Map<String,Object> findInsuredInfoByOrder(@Param("order_id") String order_id);
}
