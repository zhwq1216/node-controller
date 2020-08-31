package io.metersphere.node.controller;


import com.github.dockerjava.api.model.Container;
import io.metersphere.node.controller.request.TestRequest;
import io.metersphere.node.service.JmeterOperateService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("jmeter")
public class JmeterOperateController {
    @Resource
    private JmeterOperateService jmeterOperateService;

    /**
     * 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
     */
    @PostMapping("/container/start")
    public void startContainer(@RequestBody TestRequest testRequest) throws IOException {
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

    /**
     * 停止指定测试任务，控制上述容器停止指定的 JMeter 测试
     */
    @GetMapping("container/stop/{testId}")
    public void stopContainer(@PathVariable String testId) {
        jmeterOperateService.stopContainer(testId);
    }

    /**
     * 停止指定测试任务，控制上述容器停止指定的 JMeter 测试
     */
    @GetMapping("container/log/{testId}")
    public String logContainer(@PathVariable String testId) {
        return jmeterOperateService.logContainer(testId);
    }

    // 查询测试任务状态，控制上述容器执行相关命令查询 JMeter 测试状态
    @GetMapping("/task/status/{testId}")
    public List<Container> getTaskStatus(@PathVariable String testId) {
        return jmeterOperateService.taskStatus(testId);
    }

}
