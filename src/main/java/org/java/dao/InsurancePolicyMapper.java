package org.java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 保单数据接口
 * @author Haowen Tian
 */
@Mapper
@Component
public interface InsurancePolicyMapper {


    /**
     * 向表单表中插入一条数据
     * @param map
     */
    public void insertInsurancePolicy(Map<String,Object> map);


    /**
     * 根据保单编号修改电子保单文件
     * @param policy_id
     * @param policy_document
     */
    public void updateInsurancePolicyDocument(@Param("policy_id") String policy_id,@Param("policy_document") byte[] policy_document);


    /**
     * 多条件分页查询保单信息
     * @return
     */
    public List<Map<String,Object>> findInsurancePolicyByCondition(Map<String,Object> map);

    /**
     * 多条件查询保单数量
     * @return
     */
    public int findInsurancePolicyCountByCondition(Map<String,Object> map);



}
