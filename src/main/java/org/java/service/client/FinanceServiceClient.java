package org.java.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 财务服务客户端接口
 * @author Haowen Tian
 */
@Component
@FeignClient(value = "FINANCE-SERVICE")
public interface FinanceServiceClient {


    /**
     * 退款接口
     * @param map
     * @return
     */
    @RequestMapping("refundOrder")
    public boolean refundOrder(@RequestParam Map<String,Object> map);
}
