package io.metersphere.controller;


import com.github.dockerjava.api.model.Container;
import io.metersphere.controller.request.DockerLoginRequest;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.service.JmeterOperateService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("jmeter")
public class JmeterOperateController {
    @Resource
    private JmeterOperateService jmeterOperateService;

    // 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
    @PostMapping("/container/start")
    public void containerStart(@RequestBody TestRequest testRequest) {
        jmeterOperateService.startContainer(testRequest);
    }

    // 上传测试相关文件，将请求传过来的脚本、测试数据等文件，拷贝到上述容器中
    @PostMapping("/upload/task")
    public void uploadFile(String jmxString) {
        //  挂载数据到启动的容器
        //  FileUtil.saveFile(jmxString, "/User/liyuhao/test", "ceshi2.jmx");
    }

    // 启动测试任务，控制上述容器执行 jmeter 相关命令开始进行测试
    @PostMapping("/exec/task")
    public void taskPerform() {
        // 执行 jmx
        // docker run -it -v /Users/liyuhao/test:/test justb4/jmeter:latest -n -t /test/ceshi3.jmx
        // dockerClient.startContainerCmd("18894c7755b1").exec();
        // dockerClient.execCreateCmd("18894c7755b1").withAttachStdout(true).withCmd("jmeter", "-n", "-t", "/test/*.jmx");
    }

    // 停止指定测试任务，控制上述容器停止指定的 JMeter 测试
    @PostMapping("container/stop/{testId}")
    public void containerStop(@PathVariable String testId, @RequestBody DockerLoginRequest request) {
        jmeterOperateService.stopContainer(testId, request);
    }

    // 查询测试任务状态，控制上述容器执行相关命令查询 JMeter 测试状态
    @PostMapping("/task/status/{testId}")
    public List<Container> getTaskStatus(@PathVariable String testId, @RequestBody DockerLoginRequest request) {
        return jmeterOperateService.taskStatus(testId, request);
    }

}
