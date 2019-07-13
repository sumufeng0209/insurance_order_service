package org.java.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 订单业务接口
 * @author Haowen Tian
 */
public interface OrderService {

    /**
     * 通过任务编号查询任务数据并携带订单数据
     * @param task_id 任务编号
     * @return
     */
    public Map<String,Object> findTaskAndOrderByTaskId(String task_id);


    /**
     * 完成人工报价任务
     * @param task_id 任务编号
     * @param item_id 险种项编号数组
     * @param insured_amount 险种项保额数组
     * @param premium 险种项保费数组
     * @param order_id 订单编号
     */
    public void completeQuotationDetailsTask(String order_id,String task_id,String[] item_id,double[] insured_amount,double[] premium);


    /**
     * 完成核保员任务
     * @param map
     */
    public void completeAuditOrders(Map<String,Object> map);






}
