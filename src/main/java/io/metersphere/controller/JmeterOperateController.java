package io.metersphere.controller;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("jmeter")
public class JmeterOperateController {

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        dockerClient = DockerClientService.connectDocker();
    }

    // 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
    @GetMapping("container/start")
    public void containerStart() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowSize(true)
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("exited"))
                .exec();
        List<Container> collect = containers.stream()
                .filter(container -> container.getImage().equals("registry.fit2cloud.com/metersphere/jmeter-master:0.0.2"))
                .collect(Collectors.toList());
        //
        if (!collect.isEmpty()) {
            DockerClientService.startContainer(dockerClient, collect.get(0).getId());
        } else {
            CreateContainerResponse newContainers = DockerClientService.createContainers(dockerClient, "jmeter", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
            DockerClientService.startContainer(dockerClient, newContainers.getId());
        }
    }

    // 上传测试相关文件，将请求传过来的脚本、测试数据等文件，拷贝到上述容器中
    @PostMapping("/upload/task")
    public void uploadFile(String jmxString) {
        // 挂载数据到启动的容器
        FileUtil.saveFile(jmxString, "/User/liyuhao/test", "ceshi2.jmx");
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
