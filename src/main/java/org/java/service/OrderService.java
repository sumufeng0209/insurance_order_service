package org.java.service;

import com.sun.javafx.collections.MappingChange;
import org.springframework.stereotype.Service;

import java.util.List;
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


    /**
     * 完成主管签字任务
     * @param map
     */
    public void completeConfirmSign(Map<String,Object> map);


    /**
     * 通过订单编号查询订单申请退款信息
     * @param order_id
     * @return
     */
    public Map<String,Object> findRefundApplyByOrderId(String order_id);


    /**
     * 完成财务审核
     * @param map
     */
    public void completeRefundApplyAudit(Map<String,Object> map);


    /**
     * 通过保单编号查询该保单所购买的险种项信息
     * @param policy_id
     * @return
     */
    public List<Map<String,Object>> findInsuranceItemByPolicyId(String policy_id);


    /**
     * 多条件分页查询保单信息
     * @param map
     * @return
     */
    public Map<String,Object> findInsurancePolicyByCondition(Map<String,Object> map);


    /**
     * 通过员工编号查询个人报价任务列表
     * @param emp_id
     * @return
     */
    public Map<String,Object> findManualQuotationTask(String emp_id,int pageIndex,int pageSize);


    /**
     * 通过员工编号查询个人核保任务列表
     * @param emp_id
     * @return
     */
    public Map<String,Object> findPolicyAuditTask(String emp_id,int pageIndex,int pageSize);


     /**
      *  通过员工编号查询个人确认签字任务列表
     * @param emp_id
     * @return
     */
    public Map<String,Object> findConfirmSignTask(String emp_id,int pageIndex,int pageSize);


    /**
     *  通过员工编号查询个人退款处理任务列表
     * @param emp_id
     * @return
     */
    public Map<String,Object> findRefundHandleTask(String emp_id,int pageIndex,int pageSize);
}
