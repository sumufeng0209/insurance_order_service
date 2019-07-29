package org.java.service.impl;

import com.eaio.uuid.UUID;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.Times;
import org.java.dao.InsuranceItemMapper;
import org.java.dao.InsurancePolicyMapper;
import org.java.dao.OrderMapper;
import org.java.dao.OrderVerifyMapper;
import org.java.service.OrderService;
import org.java.service.client.ScheduleServiceClient;
import org.java.util.PolicyNumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

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
     * 保单数据接口
     */
    @Autowired
    private InsurancePolicyMapper insurancePolicyMapper;


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


    @Autowired
    private ScheduleServiceClient scheduleServiceeClient;


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

    @Override
    public void completeAuditOrders(Map<String, Object> map) {
        String instance_id = completeAuditOrdersReturn(map);

        if(instance_id!=null && !instance_id.equals("")){
            //代表核保通过
            //自动分配主管签字任务
            scheduleServiceeClient.taskAutoDispatch(instance_id);
        }
    }

    @Transactional
    @Override
    public String completeAuditOrdersReturn(Map<String, Object> map) {

        //获取审核结果：1代表通过，0代表不通过
        String certificatorAudit = map.get("certificatorAudit").toString();



        //设置审核编号
        map.put("verify_id",new UUID().toString());

        //设置审核时间
        map.put("verify_time",new Timestamp(System.currentTimeMillis()));

        //向审核表中插入一条数据
        orderVerifyMapper.insertOrderVerify(map);

        //完成任务，并设置流程变量
        Map<String,Object> variables = new HashMap<>();
        variables.put("certificatorAudit",certificatorAudit);

        //获得流程实例
        Task task = taskService.createTaskQuery().taskId(map.get("task_id").toString()).singleResult();
        System.out.println("前任务："+task.getName());
        //获得流程实例
        String instance_id = task.getProcessInstanceId();
        //完成任务
        taskService.complete(map.get("task_id").toString(),variables);

        if(certificatorAudit.equals("1")){
            //通过修改订单状态为交易成功：6
            orderMapper.updateOrderStatusByOrderId(map.get("order_id").toString(),6);

            return instance_id;
        }else{
            //不通过修改订单状态为审核未通过，重新完善资料：5
            orderMapper.updateOrderStatusByOrderId(map.get("order_id").toString(),5);

            return null;
        }
    }


    @Transactional
    @Override
    public void completeConfirmSign(Map<String, Object> map) {
        String task_id = map.get("task_id").toString();
        //创建任务查询接口并设置条件
        TaskQuery query = taskService.createTaskQuery();
        query.taskId(task_id);
        Task task = query.singleResult();
        //获得流程实例编号
        String instanceId = task.getProcessInstanceId();

        //完成任务
        taskService.complete(task_id);

        //通过流程实例编号查询订单数据
        Map<String, Object> order = orderMapper.findOrderByInstanceId(instanceId);

        //设置车辆编号
        Map<String,Object> car = (Map<String, Object>) order.get("car");
        map.put("car_id",car.get("car_id"));

        //设置被保人编号
        Map<String,Object> insured = (Map<String, Object>) order.get("insured");
        map.put("insured_id",insured.get("insured_id").toString());

        //设置投保人编号
        Map<String,Object> policy_holder = (Map<String, Object>) order.get("policy_holder");
        map.put("policy_holder_id",policy_holder.get("holder_id").toString());


        String jqPolicyId = new UUID().toString();

        //判断订单里面的保单信息
        if(order.get("is_pay_jiaoQiangXian").toString().equals("1")){
            //生成交强险保单
            //设置保单编号
            map.put("policy_id",jqPolicyId);

            //生成保单号
            map.put("policy_number", PolicyNumberUtils.makePolicyNumber());

            //设置保单起始时间
            map.put("start_time",order.get("jiaoQiangXian_start_date"));

            //设置保单到期时间为一年后
            Timestamp time = (Timestamp) ((Timestamp) order.get("jiaoQiangXian_start_date")).clone();
            time.setYear(time.getYear()+1);
            map.put("stop_time",time);

            //设置保单类型为交强险：1
            map.put("policy_type",1);

            //插入数据
            insurancePolicyMapper.insertInsurancePolicy(map);
        }


        String syPolicyId = new UUID().toString();
        if(order.get("is_pay_shangYeXian").toString().equals("1")){
            //生成商业险保单
            //设置保单编号
            map.put("policy_id",syPolicyId);

            //生成保单号
            map.put("policy_number",PolicyNumberUtils.makePolicyNumber());

            //设置保单起始时间
            map.put("start_time",order.get("shangYeXian_start_date"));

            //设置保单到期时间为一年后
            Timestamp time = (Timestamp) ((Timestamp) order.get("jiaoQiangXian_start_date")).clone();
            time.setYear(time.getYear()+1);
            map.put("stop_time",time);

            //设置保单类型为商业险：2
            map.put("policy_type",2);

            //插入数据
            insurancePolicyMapper.insertInsurancePolicy(map);
        }



        //生成电子保单

        //模板路径
        String modelPath = map.get("docuemntPath").toString()+"\\电子保单模板.pdf";
        //生成文件新路径
        String newFilePath = map.get("docuemntPath").toString()+"\\"+order.get("order_id").toString()+".pdf";

        PdfReader pdfReader = null;
        FileOutputStream out = null;
        ByteArrayOutputStream bos = null;
        PdfStamper stamper = null;

        try{
            //输出流
            out = new FileOutputStream(newFilePath);
            //读取模板
            pdfReader = new PdfReader(modelPath);
            bos = new ByteArrayOutputStream();
            stamper = new PdfStamper(pdfReader,bos);
            //获得表单
            AcroFields form = stamper.getAcroFields();

            //获得表单中的所有表单key
            Set<String> keys = form.getFields().keySet();


//            Map<String, Object> order = orderMapper.findOrderByOrderId("0c58d270-a892-11e9-8959-00e04d36d16c");
//
//            Map<String,Object> car = (Map<String, Object>) order.get("car");
//
//            Map<String,Object> policy_holder = (Map<String, Object>) order.get("policy_holder");
//
//            Map<String,Object> insured = (Map<String, Object>) order.get("insured");

            order.putAll(car);

            order.putAll(policy_holder);

            order.putAll(insured);


            //循环设置属性名相同的字段
            for(String key:keys){
                if(order.get(key)!=null){
                    form.setField(key,order.get(key).toString());
                }
            }


            //获得保险起始日期
            Timestamp start_time = (Timestamp) order.get("jiaoQiangXian_start_date");

            //设置保险起始年份
            form.setField("start_year",start_time.getYear()+1900+"");

            //设置保险起始月份
            form.setField("start_month",start_time.getMonth()+"");

            //设置保险起始日
            form.setField("start_date",start_time.getDate()+"");

            //设置保险到期年份
            form.setField("stop_year",start_time.getYear()+1901+"");

            //设置保险到期月份
            form.setField("stop_month",start_time.getMonth()+1+"");

            //设置保险到期日
            form.setField("stop_date",start_time.getDate()+"");

            double baoe_sum = 0;
            double jiaoqiangxianbaoe_sum = 0;
            List<Map<String,Object>> items = (List<Map<String, Object>>) order.get("insuranceItems");
            for (Map<String,Object> item: items) {
                String insurance_name = (String) item.get("insurance_name");
                baoe_sum += (double) item.get("insured_amount");
                if("交强险".equals(insurance_name)){
                    jiaoqiangxianbaoe_sum = (double) item.get("insured_amount");
                    //设置交强险保费
                    form.setField("jiaoqinagxianbaof",item.get("premium").toString());
                    //设置交强险保额
                    form.setField("jiaoqiangxianbaoe",item.get("insured_amount").toString());
                }else if("车辆损失险".equals(insurance_name)){
                    form.setField("cheliangsunshixian","Yes");
                    form.setField("cheliangsunshixianbaoe",item.get("insured_amount").toString());
                    form.setField("cheliangsunshixianbaof",item.get("premium").toString());
                }else if("第三方责任险".equals(insurance_name)){
                    form.setField("disanfangzerenxian","Yes");
                    form.setField("disanfangzerenxianbaoe",item.get("insured_amount").toString());
                    form.setField("disanfangzerenxianbaof",item.get("premium").toString());
                }else if("全车盗抢险".equals(insurance_name)){
                    form.setField("quanchedaoqiangxian","Yes");
                    form.setField("quanchedaoqiangxianbaoe",item.get("insured_amount").toString());
                    form.setField("quanchedaoqiangxianbaof",item.get("premium").toString());
                }else if("司机座位责任险".equals(insurance_name)){
                    form.setField("sijizuoweizerenxian","Yes");
                    form.setField("sijizuoweizerenxianbaoe",item.get("insured_amount").toString());
                    form.setField("sijizuoweizerenxianbaof",item.get("premium").toString());
                }else if("乘客座位责任险".equals(insurance_name)){
                    form.setField("chengkezuoweizerenxian","Yes");
                    form.setField("chengkezuoweizerenxianbaoe",item.get("insured_amount").toString());
                    form.setField("chengkezuoweizerenxianbaof",item.get("premium").toString());
                }else if("玻璃单独破碎险".equals(insurance_name)){
                    form.setField("bolidanduposuixian","Yes");
                    form.setField("bolidanduposuixianbaoe",item.get("insured_amount").toString());
                    form.setField("bolidanduposuixianbaof",item.get("premium").toString());
                }else if("车身划痕险".equals(insurance_name)){
                    form.setField("cheshenhuahenxian","Yes");
                    form.setField("cheshenhuahenxianbaoe",item.get("insured_amount").toString());
                    form.setField("cheshenhuahenxianbaof",item.get("premium").toString());
                }else if("涉水损失险".equals(insurance_name)){
                    form.setField("sheshuisunshixian","Yes");
                    form.setField("sheshuisunshixianbaoe",item.get("insured_amount").toString());
                    form.setField("sheshuisunshixianbaof",item.get("premium").toString());
                }else if("车辆自燃险".equals(insurance_name)){
                    form.setField("cheliangziranxian","Yes");
                    form.setField("cheliangziranxianbaoe",item.get("insured_amount").toString());
                    form.setField("cheliangziranxianbaof",item.get("premium").toString());
                }
            }



            //设置商业险保额小计
            form.setField("shangyexianbaoesum",baoe_sum-jiaoqiangxianbaoe_sum+" 元");

            //设置商业险保费小计
            form.setField("shangyexianbaofeisum",order.get("shangYeXian_premium").toString()+" 元");

            //设置总保额
            form.setField("zongbaoe",baoe_sum+" 元");

            //设置总保费
            form.setField("zongbaofei",order.get("premium_sum").toString()+" 元");



            //设置好后，pdf不能编辑
            stamper.setFormFlattening(true);

            Document document = new Document();

            PdfCopy pdfCopy = new PdfCopy(document,out);

            document.open();

            PdfImportedPage importedPage = pdfCopy.getImportedPage(pdfReader,1);

            pdfCopy.addPage(importedPage);

            document.close();

            InputStream in = new FileInputStream(newFilePath);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];

            int len;

            while((len=in.read(bytes))!=-1){
                o.write(bytes,0,len);
            }


            //判断是否购买交强险
            if(order.get("is_pay_jiaoQiangXian").toString().equals("1")){
                insurancePolicyMapper.updateInsurancePolicyDocument(jqPolicyId,o.toByteArray());
            }


            //判断是否购买商业险
            if(order.get("is_pay_shangYeXian").toString().equals("1")){
                insurancePolicyMapper.updateInsurancePolicyDocument(syPolicyId,o.toByteArray());
            }

            //删除文件
            FileUtils.deleteQuietly(new File(newFilePath));

            o.close();


        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            try{
                stamper.close();
                bos.close();
                out.close();
                pdfReader.close();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

    }

    @Override
    public Map<String, Object> findRefundApplyByOrderId(String order_id) {
        return orderMapper.findRefundApplyByOrderId(order_id);
    }

    @Transactional
    @Override
    public void completeRefundApplyAudit(Map<String, Object> map) {
        //推进一步工作流
        taskService.complete(map.get("task_id").toString());

        //修改订单状态为退款成功：9
        orderMapper.updateOrderStatusByOrderId(map.get("order_id").toString(),9);
    }


    @Override
    public List<Map<String, Object>> findInsuranceItemByPolicyId(String policy_id) {
        return insuranceItemMapper.findInsuranceItemByPolicyId(policy_id);
    }

    @Override
    public Map<String, Object> findInsurancePolicyByCondition(Map<String, Object> map) {
        map.put("pageIndex",Integer.parseInt(map.get("pageIndex").toString()));
        map.put("pageSize",Integer.parseInt(map.get("pageSize").toString()));
        Map<String,Object> data = new HashMap<>();
        List<Map<String, Object>> list = insurancePolicyMapper.findInsurancePolicyByCondition(map);
        data.put("pageIndex",map.get("pageIndex"));
        int count = insurancePolicyMapper.findInsurancePolicyCountByCondition(map);
        data.put("count",count);
        int pageSize = Integer.parseInt(map.get("pageSize").toString());
        data.put("pageCount",(count+pageSize-1)/pageSize);
        data.put("data",list);
        data.put("code",0);
        data.put("msg","");
        return data;
    }



    @Override
    public Map<String, Object> findManualQuotationTask(String emp_id,int pageIndex,int pageSize) {
        Map<String,Object> map = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        TaskQuery query = taskService.createTaskQuery();
        query.taskDefinitionKey("manualQuotation");
        query.taskAssignee(emp_id);
        List<Task> tasks = query.listPage((pageIndex - 1) * pageSize, pageSize);
        for (Task task:tasks) {
            Map<String,Object> m = new HashMap<>();
            m.put("task_id",task.getId());
            m.put("task_name","保单询价");
            //通过任务编号查询业务数据
            Map<String, Object> order = findTaskAndOrderByTaskId(task.getId());
            m.put("order",order);
            list.add(m);
        }
        map.put("count",query.list().size());
        map.put("code",0);
        map.put("msg","");
        map.put("data",list);
        return map;
    }

    @Override
    public Map<String, Object> findPolicyAuditTask(String emp_id, int pageIndex, int pageSize) {
        Map<String,Object> map = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        TaskQuery query = taskService.createTaskQuery();
        query.taskDefinitionKey("certificatorAudit");
        query.taskAssignee(emp_id);
        List<Task> tasks = query.listPage((pageIndex - 1) * pageSize, pageSize);
        for (Task task:tasks) {
            Map<String,Object> m = new HashMap<>();
            m.put("task_id",task.getId());
            m.put("task_name","审核保单");
            //通过任务编号查询业务数据
            Map<String, Object> order = findTaskAndOrderByTaskId(task.getId());
            m.put("order",order);
            list.add(m);
        }
        map.put("count",query.list().size());
        map.put("code",0);
        map.put("msg","");
        map.put("data",list);
        return map;
    }


    @Override
    public Map<String, Object> findConfirmSignTask(String emp_id, int pageIndex, int pageSize) {
        Map<String,Object> map = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        TaskQuery query = taskService.createTaskQuery();
        query.taskDefinitionKey("signatureConfirmation");
        query.taskAssignee(emp_id);
        List<Task> tasks = query.listPage((pageIndex - 1) * pageSize, pageSize);
        for (Task task:tasks) {
            Map<String,Object> m = new HashMap<>();
            m.put("task_id",task.getId());
            m.put("task_name","确认签字");
            //通过任务编号查询业务数据
            Map<String, Object> order = findTaskAndOrderByTaskId(task.getId());
            m.put("order",order);
            list.add(m);
        }
        map.put("count",query.list().size());
        map.put("code",0);
        map.put("msg","");
        map.put("data",list);
        return map;
    }

    @Override
    public Map<String, Object> findRefundHandleTask(String emp_id, int pageIndex, int pageSize) {
        Map<String,Object> map = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();
        TaskQuery query = taskService.createTaskQuery();
        query.taskDefinitionKey("financialAudit");
        query.taskAssignee(emp_id);
        List<Task> tasks = query.listPage((pageIndex - 1) * pageSize, pageSize);
        for (Task task:tasks) {
            Map<String,Object> m = new HashMap<>();
            m.put("task_id",task.getId());
            m.put("task_name","退款处理");
            //通过任务编号查询业务数据
            Map<String, Object> order = findTaskAndOrderByTaskId(task.getId());
            m.put("order",order);
            //通过编号查询退款申请数据
            Map<String, Object> refundApplyInfo = orderMapper.findRefundApplyByOrderId(order.get("order_id").toString());
            m.put("refundApplyInfo",refundApplyInfo);
            list.add(m);
        }
        map.put("count",query.list().size());
        map.put("code",0);
        map.put("msg","");
        map.put("data",list);
        return map;
    }
}
