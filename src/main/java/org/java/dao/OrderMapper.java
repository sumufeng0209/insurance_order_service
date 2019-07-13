package org.java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单数据访问接口
 * @author Haowen Tian
 */
@Mapper
@Component
public interface OrderMapper {


    /**
     * 通过流程实例编号查询订单信息
     * @param instance_id
     * @return
     */
    public Map<String,Object> findOrderByInstanceId(@Param("instance_id") String instance_id);


    /**
     * 根据投保订单编号修改订单状态
     * @param order_id
     * @param status
     */
    public void updateOrderStatusByOrderId(@Param("order_id") String order_id,@Param("status") int status);
}
