package io.metersphere.controller;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequestMapping("jmeter")
public class JmeterOperateController {

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        dockerClient = DockerClientService.connectDocker();
    }

    // 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
    @GetMapping("container/start/{size}")
    public void containerStart(@PathVariable Integer size) {
        String testId = UUID.randomUUID().toString();
        String containerImage = "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2";
        ArrayList<String> containerIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String containerName = testId + i;
            String containerId = DockerClientService.createContainers(dockerClient, containerName, containerImage).getId();
            containerIdList.add(containerId);
        }

        //  ~/test/container1/*.jmx  ~/test/container2/*.jmx
        //  FileUtil.saveFile(jmxString, "/User/liyuhao/test", "ceshi2.jmx");

        //  从主机复制文件到容器 (一个容器对应一个文件夹)
        int count = 0;
        for (int i = 1; i <= containerIdList.size(); i++) {
            int index = count++ % containerIdList.size();
            dockerClient.copyArchiveToContainerCmd(containerIdList.get(index))
                    .withHostResource("/Users/liyuhao/test/test"+i)
                    .withDirChildrenOnly(true)
                    .withRemotePath("/test")
                    .exec();
        }
        containerIdList.forEach(containerId -> {DockerClientService.startContainer(dockerClient, containerId);});
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
        // docker run -it -v /Users/liyuhao/test:/test justb4/jmeter:latest -n -t /test/ceshi3.jmx
        // dockerClient.startContainerCmd("18894c7755b1").exec();
        // dockerClient.execCreateCmd("18894c7755b1").withAttachStdout(true).withCmd("jmeter", "-n", "-t", "/test/*.jmx");
    }

    // 停止指定测试任务，控制上述容器停止指定的 JMeter 测试
    @PostMapping("container/stop/{testId}")
    public void containerStop(@PathVariable String testId) {
        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(Arrays.asList(testId))
                .exec();
        // container stop
        list.forEach(container -> DockerClientService.stopContainer(dockerClient, container.getId()));
    }

    // 查询测试任务状态，控制上述容器执行相关命令查询 JMeter 测试状态
    @PostMapping("/task/status/{testId}")
    public void getTaskStatus(@PathVariable String testId) {
        dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created","restarting","running","paused","exited"))
                .withNameFilter(Arrays.asList(testId))
                .exec();
        // 查询执行的状态
    }

}
