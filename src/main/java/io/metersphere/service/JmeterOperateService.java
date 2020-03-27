package io.metersphere.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Container;
import io.metersphere.controller.request.DockerLoginRequest;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import io.metersphere.util.LogUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class JmeterOperateService {

    public void startContainer(TestRequest testRequest) {
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
        // pull image
        try {
            dockerClient.pullImageCmd(containerImage)
                    .exec(new PullImageResultCallback() {

                    })
                    .awaitCompletion();
        } catch (InterruptedException e) {
            LogUtil.error("Pull image error.");
            return;
        }

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


    public void stopContainer(String testId, DockerLoginRequest request) {
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

    public List<Container> taskStatus(String testId, DockerLoginRequest request) {
        DockerClient dockerClient = DockerClientService.connectDocker(request);
        List<Container> containerList = dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Arrays.asList(testId))
                .exec();
        // 查询执行的状态
        return containerList;
    }
}
