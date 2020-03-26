package io.metersphere.controller;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import io.metersphere.controller.request.DockerLoginRequest;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("jmeter")
public class JmeterOperateController {

    // 初始化测试任务，根据需求启动若干个 JMeter Engine 容器
    @PostMapping("/container/start")
    public void containerStart(@RequestBody TestRequest testRequest) {
        DockerClient dockerClient = DockerClientService.connectDocker(testRequest);
        int size = testRequest.getSize();
        String testId = testRequest.getTestId();

        String containerImage = testRequest.getImage();
        String filePath = "/tmp/" + testId;
        String fileName = testRequest.getTestId() + ".jmx";


        List<Container> list = dockerClient.listContainersCmd().withShowAll(true).withNameFilter(Arrays.asList(testId)).exec();
        if (!list.isEmpty()) {
            list.forEach(cId -> DockerClientService.removeContainer(dockerClient, cId.getId()));
        }

        //  每个测试生成一个文件夹
        FileUtil.saveFile(testRequest.getFileString(), filePath, fileName);
        // 保存测试数据文件
        testRequest.getTestData().forEach((k, v) -> {
            FileUtil.saveFile(v, filePath, k);
        });

        ArrayList<String> containerIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String containerName = testId + i;
            String containerId = DockerClientService.createContainers(dockerClient, containerName, containerImage).getId();
            //  从主机复制文件到容器
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withHostResource(filePath)
                    .withDirChildrenOnly(true)
                    .withRemotePath("/test")
                    .exec();
            containerIdList.add(containerId);
        }

        containerIdList.forEach(containerId -> {
            DockerClientService.startContainer(dockerClient, containerId);
        });
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
        DockerClient dockerClient = DockerClientService.connectDocker(request);

        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("running"))
                .withNameFilter(Arrays.asList(testId))
                .exec();
        // container stop
        list.forEach(container -> DockerClientService.stopContainer(dockerClient, container.getId()));
    }

    // 查询测试任务状态，控制上述容器执行相关命令查询 JMeter 测试状态
    @PostMapping("/task/status/{testId}")
    public List<Container> getTaskStatus(@PathVariable String testId, @RequestBody DockerLoginRequest request) {
        DockerClient dockerClient = DockerClientService.connectDocker(request);
        List<Container> containerList = dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Arrays.asList(testId))
                .exec();
        // 查询执行的状态
        return containerList;
    }

}
