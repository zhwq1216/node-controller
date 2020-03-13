package io.metersphere.controller;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import io.metersphere.util.DockerClientService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("jmeter")
public class JmeterOperateController {

    // 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
    @PostMapping("container/start")
    public void containerStart() {
        DockerClient client = DockerClientService.connectDocker();
        CreateContainerResponse containers = DockerClientService.createContainers(client, "jmeter", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
        DockerClientService.startContainer(client, containers.getId());
    }

    // 上传测试相关文件，将请求传过来的脚本、测试数据等文件，拷贝到上述容器中
    @PostMapping("/upload/task")
    public void uploadFile() {
        // 挂载数据到启动的容器
    }

    // 启动测试任务，控制上述容器执行 jmeter 相关命令开始进行测试
    @PostMapping("/exec/task")
    public void taskPerform() {
        // 执行 jmx

    }

    // 停止指定测试任务，控制上述容器停止指定的 JMeter 测试
    @PostMapping("container/stop")
    public void containerStop() {
        // 停止执行 task
    }

    // 查询测试任务状态，控制上述容器执行相关命令查询 JMeter 测试状态
    @PostMapping("/task/status")
    public void getTaskStatus() {
        // 查询执行的状态
    }

}
