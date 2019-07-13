package org.java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单审核数据接口
 * @author Haowen Tian
 */
@Mapper
@Component
public interface OrderVerifyMapper {


    /**
     * 向订单审核表中插入一条数据
     * @param map
     */
    public void insertOrderVerify(Map<String,Object> map);
}
