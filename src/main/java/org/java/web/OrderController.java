package org.java.web;

import com.netflix.discovery.converters.Auto;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.sun.javafx.collections.MappingChange;
import org.java.dao.OrderMapper;
import org.java.service.OrderService;
import org.java.service.client.FinanceServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器层
 * @author Haowen Tian
 */
@Controller
public class OrderController {


    @Autowired
    private FinanceServiceClient financeServiceClient;


    @Autowired
    private ServletContext servletContext;

    /**
     * 订单业务接口
     */
    @Autowired
    private OrderService orderService;


    @Autowired
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
        ses.setAttribute("certificates",order.get("certificates"));
        model.addAttribute("task_type","auditOrders");//任务类型：审核订单
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

//        String emp_id = employee.get("emp_id").toString();
//
//        map.put("emp_id",emp_id);

        orderService.completeAuditOrders(map);

        return "";
    }


    /**
     * 通过证件类型显示对应的证件图片
     * @param type
     */
    @RequestMapping("showImg/{type}")
    public void showImg(@PathVariable("type")String type, HttpSession ses, HttpServletResponse res) throws IOException {
        Map<String,Object> certificates = (Map<String, Object>) ses.getAttribute("certificates");
        byte[] bytes = (byte[]) certificates.get(type);
        OutputStream out = res.getOutputStream();
        out.write(bytes,0,bytes.length);
        out.close();
    }


    /**
     * 转发到主管确认签字页面
     * @param task_id
     * @param model
     * @return
     */
    @RequestMapping("forwardAuditOrderConfirm/{task_id}")
    public String forwardAuditOrderConfirm(@PathVariable("task_id") String task_id,Model model){
        Map<String, Object> order = orderService.findTaskAndOrderByTaskId(task_id);
        order.put("task_id",task_id);
        model.addAttribute("order",order);
        ses.setAttribute("certificates",order.get("certificates"));
        model.addAttribute("task_type","confirm");//任务类型：主管签字确认
        return "auditOrders";
    }


    /**
     * 确认签字
     * @param map
     * @return
     */
    @RequestMapping("confirmSign")
    public String confirmSign(@RequestParam Map<String,Object> map) throws FileNotFoundException {
        //获得document的路径
        String serverpath= ResourceUtils.getURL("classpath:static").getPath().replace("%20"," ").replace('/', '\\');
        serverpath=serverpath.substring(1);//从路径字符串中取出工程路径
        map.put("docuemntPath",serverpath+"\\document");
        orderService.completeConfirmSign(map);
        return "";
    }


    /**
     * 转发到退款订单审核页面
     * @param task_id
     * @return
     */
    @RequestMapping("forwardRefundOrderAudit/{task_id}")
    public String forwardRefundOrderAudit(@PathVariable("task_id") String task_id,Model model){
        Map<String, Object> order = orderService.findTaskAndOrderByTaskId(task_id);
        model.addAttribute("order",order);
        //获得订单编号
        String order_id = order.get("order_id").toString();
        Map<String, Object> refundApply = orderService.findRefundApplyByOrderId(order_id);
        model.addAttribute("refundApply",refundApply);
        ses.setAttribute("certificates",order.get("certificates"));
        return "refundOrderAudit";
    }



    @HystrixCommand(fallbackMethod = "completeRefundApplyAuditFallback")
    @RequestMapping("completeRefundApplyAudit")
    @ResponseBody
    public boolean completeRefundApplyAudit(@RequestParam Map<String,Object> map){
        boolean result =  financeServiceClient.refundOrder(map);
        //如果退款成功就修改业务数据
        if(result){
            orderService.completeRefundApplyAudit(map);
        }
        return result;
    }

    public boolean completeRefundApplyAuditFallback(@RequestParam Map<String,Object> map){
        return false;
    }



    /**
     * 通过保单编号查询该保单所购买的险种项信息
     * @param policy_id
     * @return
     */
    @RequestMapping("findInsuranceItemByPolicyId")
    @ResponseBody
    public List<Map<String,Object>> findInsuranceItemByPolicyId(String policy_id){
        return orderService.findInsuranceItemByPolicyId(policy_id);
    }


    /**
     * 转发到人工报价列表页面
     * @return
     */
    @RequestMapping("forwardManualQuotationList")
    public String forwardManualQuotationList(){
        return "manualQuotationList";
    }

    /**
     * 查询个人报价任务列表
     * @param session
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @RequestMapping("findManualQuotationTask")
    @ResponseBody
    public Map<String,Object> findManualQuotationTask(HttpSession session,int pageIndex,int pageSize){
        Map<String,Object> emp = (Map<String, Object>) session.getAttribute("emp");
        String  emp_id = emp.get("emp_id").toString();
        Map<String, Object> tasks = orderService.findManualQuotationTask(emp_id, pageIndex, pageSize);
        return tasks;
    }

    /**
     * 查询个人核保任务列表
     * @param session
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @RequestMapping("findPolicyAuditTask")
    @ResponseBody
    public Map<String,Object> findPolicyAuditTask(HttpSession session,int pageIndex,int pageSize){
        Map<String,Object> emp = (Map<String, Object>) session.getAttribute("emp");
        String  emp_id = emp.get("emp_id").toString();
        Map<String, Object> tasks = orderService.findPolicyAuditTask(emp_id, pageIndex, pageSize);
        return tasks;
    }

    /**
     * 查询个人确认签字任务列表
     * @param session
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @RequestMapping("findConfirmSignTask")
    @ResponseBody
    public Map<String,Object> findConfirmSignTask(HttpSession session,int pageIndex,int pageSize){
        Map<String,Object> emp = (Map<String, Object>) session.getAttribute("emp");
        String  emp_id = emp.get("emp_id").toString();
        Map<String, Object> tasks = orderService.findConfirmSignTask(emp_id, pageIndex, pageSize);
        return tasks;
    }


    /**
     * 查询个人退款任务列表
     * @param session
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @RequestMapping("findRefundHandleTask")
    @ResponseBody
    public Map<String,Object> findRefundHandleTask(HttpSession session,int pageIndex,int pageSize){
        Map<String,Object> emp = (Map<String, Object>) session.getAttribute("emp");
        String  emp_id = emp.get("emp_id").toString();
        Map<String, Object> tasks = orderService.findRefundHandleTask(emp_id, pageIndex, pageSize);
        return tasks;
    }




    /**
     * 转发到审核保单列表页面
     * @return
     */
    @RequestMapping("forwardPolicyAuditList")
    public String forwardPolicyAuditList(){
        return "policyAuditList";
    }

    /**
     * 转发到确认签字列表页面
     * @return
     */
    @RequestMapping("forwardConfirmSignList")
    public String forwardConfirmSignList(){
        return "confirmSignList";
    }


    /**
     * 转发到退款处理列表页面
     * @return
     */
    @RequestMapping("forwardRefundHandleList")
    public String forwardRefundHandleList(){
        return "refundHandleList";
    }


    /**
     * 多条件查询保单信息
     * @param map：
     *           pageIndex:当前页
     *           pageSize:页码大小
     *           policy_number:保单号
     *           car_number:车牌号
     *           insured_name:被保人姓名
     *
     * @return
     */
    @RequestMapping("findInsurancePolicyByCondition")
    @ResponseBody
    public Map<String,Object> findInsurancePolicyByCondition(@RequestParam Map<String,Object> map){
        return orderService.findInsurancePolicyByCondition(map);
    }

}
