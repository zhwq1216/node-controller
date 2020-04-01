package io.metersphere.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import io.metersphere.controller.request.DockerLoginRequest;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import io.metersphere.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JmeterOperateService {

    public void startContainer(TestRequest testRequest) {
        DockerClient dockerClient = DockerClientService.connectDocker(testRequest);
        int size = testRequest.getSize();
        String testId = testRequest.getTestId();

        String containerImage = testRequest.getImage();
        String filePath = "/tmp/" + testId;
        String fileName = testRequest.getTestId() + ".jmx";


        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        LogUtil.info("container size: " + list.size());
        if (!list.isEmpty()) {
            list.forEach(cId -> DockerClientService.removeContainer(dockerClient, cId.getId()));
        }

        //  每个测试生成一个文件夹
        FileUtil.saveFile(testRequest.getFileString(), filePath, fileName);
        // 保存测试数据文件
        testRequest.getTestData().forEach((k, v) -> {
            FileUtil.saveFile(v, filePath, k);
        });

        // 查找镜像
        searchImage(dockerClient, testRequest.getImage());

        ArrayList<String> containerIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String containerName = testId + "-" + i;
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

    private void searchImage(DockerClient dockerClient, String imageName) {
        // image
        List<Image> imageList = dockerClient.listImagesCmd().exec();
        if (CollectionUtils.isEmpty(imageList)) {
            throw new RuntimeException("Image List is empty");
        }
        List<Image> collect = imageList.stream().filter(image -> {
            String[] repoTags = image.getRepoTags();
            if (repoTags == null) {
                return false;
            }
            for (String repoTag : repoTags) {
                if (repoTag.equals(imageName)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

        if (collect.size() == 0) {
            throw new RuntimeException("Image Not Found.");
        }
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
