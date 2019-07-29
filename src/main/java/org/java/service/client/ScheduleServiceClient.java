package org.java.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 调度服务客户端接口
 */
@Component
@FeignClient(value = "SCHEDULE-SERVICE")
public interface ScheduleServiceClient {


    @RequestMapping("taskAutoDispatch/{processInstId}")
    public List<String> taskAutoDispatch(@PathVariable("processInstId") String processInstId);
}
