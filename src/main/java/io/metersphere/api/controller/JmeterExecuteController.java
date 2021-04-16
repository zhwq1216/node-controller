package io.metersphere.api.controller;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.controller.request.RunRequest;
import io.metersphere.api.service.JmeterExecuteService;
import io.metersphere.node.util.LogUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/jmeter")
public class JmeterExecuteController {

    @Resource
    private JmeterExecuteService jmeterExecuteService;

    @PostMapping(value = "/api/run", consumes = {"multipart/form-data"})
    public String apiRun(@RequestParam(value = "files") MultipartFile[] bodyFiles, @RequestParam(value = "jarFiles") MultipartFile[] jarFiles, String request) {
        LogUtil.info("接收到测试请求 start ");
        RunRequest runRequest = JSON.parseObject(request, RunRequest.class);
        return jmeterExecuteService.run(runRequest, bodyFiles, jarFiles);
    }

    @GetMapping("/status")
    public String getStatus() {
        return "OK";
    }

}
