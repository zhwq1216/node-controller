package io.metersphere.api.controller;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.module.JvmInfo;
import io.metersphere.api.service.JmeterExecuteService;
import io.metersphere.api.service.JvmService;
import io.metersphere.dto.JmeterRunRequestDTO;
import io.metersphere.jmeter.LocalRunner;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/jmeter")
public class JmeterExecuteController {

    @Resource
    private JmeterExecuteService jmeterExecuteService;

    @PostMapping(value = "/api/start")
    public String apiStartRun(@RequestBody JmeterRunRequestDTO runRequest) {
        System.out.println("接收到测试请求： " + JSON.toJSONString(runRequest));
        return jmeterExecuteService.runStart(runRequest);
    }

    @GetMapping("/status")
    public String getStatus() {
        return "OK";
    }


    @GetMapping("/getJvmInfo")
    public JvmInfo getJvmInfo() {
        return JvmService.jvmInfo();
    }

    @GetMapping("/getRunning/{key}")
    public Integer getRunning(@PathVariable String key) {
        return jmeterExecuteService.getRunningTasks(key);
    }

    @GetMapping("/stop")
    public void stop(@RequestBody List<String> keys) {
        new LocalRunner().stop(keys);
    }

}
