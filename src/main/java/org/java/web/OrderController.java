package org.java.web;

import com.netflix.discovery.converters.Auto;
import org.java.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 订单控制器层
 * @author Haowen Tian
 */
@Controller
public class OrderController {

    /**
     * 订单业务接口
     */
    @Autowired
    private OrderService orderService;


    @Auto
    private HttpSession ses;

    /**
     * 通过任务编号查询任务详细，并跳转到人工报价页面
     * @param task_id
     * @return
     */
    @RequestMapping("showOrderDetails/{task_id}")
    public String showOrderDetails(@PathVariable("task_id") String task_id,Model model){
        Map<String, Object> order = orderService.findTaskAndOrderByTaskId(task_id);
        model.addAttribute("order",order);
        return "quotationDetails";
    }


    /**
     * 通过任务编号查询任务详细，并跳转到保单审核页面
     * @param task_id
     * @param model
     * @return
     */
        @RequestMapping("forwardAuditOrders/{task_id}")
    public String forwardAuditOrders(@PathVariable("task_id") String task_id,Model model){
        Map<String, Object> order = orderService.findTaskAndOrderByTaskId(task_id);
        model.addAttribute("order",order);
        return "auditOrders";
    }


    /**
     * 完成人工报价任务
     * @param task_id 任务编号
     * @param item_id 险种项编号数组
     * @param insured_amount 险种项保额数组
     * @param premium 险种项保费数组
     * @param order_id 订单编号
     * @return
     */
    @RequestMapping("completeQuotationDetailsTask")
    public String completeQuotationDetailsTask(String task_id,String[] item_id,double[] insured_amount,double[] premium,String order_id){
        orderService.completeQuotationDetailsTask(order_id,task_id,item_id,insured_amount,premium);
        return "";
    }


    /**
     * 完成核保员审核任务
     * @param map
     * @return
     */
    @RequestMapping("completeAuditOrder")
    public String completeAuditOrder(@RequestParam Map<String,Object> map){

        Map<String,Object> employee = (Map<String, Object>) ses.getAttribute("employee");

        String emp_id = employee.get("emp_id").toString();

        map.put("emp_id",emp_id);

        orderService.completeAuditOrders(map);

        return "";
    }
}
