package org.java.service.impl;

import com.eaio.uuid.UUID;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.java.dao.InsuranceItemMapper;
import org.java.dao.OrderMapper;
import org.java.dao.OrderVerifyMapper;
import org.java.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单业务接口实现类
 * @author Haowen Tian
 */
@Service
public class OrderServiceImpl implements OrderService {


    /**
     * 订单数据访问接口
     */
    @Autowired
    private OrderMapper orderMapper;


    /**
     * 险种项数据接口
     */
    @Autowired
    private InsuranceItemMapper insuranceItemMapper;


    /**
     * 订单审核数据接口
     */
    @Autowired
    private OrderVerifyMapper orderVerifyMapper;


    /**
     * 工作流实例查询接口
     */
    @Autowired
    private RuntimeService runtimeService;


    /**
     * 工作流任务查询接口
     */
    @Autowired
    private TaskService taskService;


    @Override
    public Map<String, Object> findTaskAndOrderByTaskId(String task_id) {
        //创建任务查询接口
        TaskQuery query = taskService.createTaskQuery();

        //设置任务查询条件
        query.taskId(task_id);

        //查询任务对象
        Task task = query.singleResult();

        //获得流程实例编号
        String instanceId = task.getProcessInstanceId();

        //通过流程实例编号查询订单信息
        Map<String, Object> order = orderMapper.findOrderByInstanceId(instanceId);

        //将任务数据封装到流程实例中
        order.put("task_id",task.getId());
        order.put("task_name",task.getName());

        return order;
    }

    @Transactional
    @Override
    public void completeQuotationDetailsTask(String order_id,String task_id, String[] item_id, double[] insured_amount, double[] premium) {
        //循环修改险种项的保额和保费
        for(int i=0;i<item_id.length;i++){
            insuranceItemMapper.updateInsuranceItemPriceByItemId(item_id[i],insured_amount[i],premium[i]);
        }

        //修改订单状态为待完善：2
        orderMapper.updateOrderStatusByOrderId(order_id,2);

        //推进一步工作流
        taskService.complete(task_id);

    }

    @Transactional
    @Override
    public void completeAuditOrders(Map<String, Object> map) {
        //获取审核结果：1代表通过，0代表不通过
        String certificatorAudit = map.get("certificatorAudit").toString();

        if(certificatorAudit.equals("1")){
            //通过修改订单状态为交易成功：6
            orderMapper.updateOrderStatusByOrderId(map.get("order_id").toString(),6);
        }else{
            //不通过修改订单状态为审核未通过，重新完善资料：5
            orderMapper.updateOrderStatusByOrderId(map.get("order_id").toString(),5);
        }

        //设置审核编号
        map.put("verify_id",new UUID().toString());

        //设置审核时间
        map.put("verify_time",new Timestamp(System.currentTimeMillis()));

        //向审核表中插入一条数据
        orderVerifyMapper.insertOrderVerify(map);

        //完成任务，并设置流程变量
        Map<String,Object> variables = new HashMap<>();
        variables.put("task_id",map.get("task_id").toString());
        taskService.complete(map.get("task_id").toString(),variables);
    }
}
