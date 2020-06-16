package io.metersphere.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.InvocationBuilder;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.LogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class JmeterOperateService {

    public void startContainer(TestRequest testRequest) throws IOException {
        LogUtil.info("Receive start container request, test id: {}", testRequest.getTestId());
        DockerClient dockerClient = DockerClientService.connectDocker(testRequest);
        int size = testRequest.getSize();
        String testId = testRequest.getTestId();

        String containerImage = testRequest.getImage();
        String filePath = StringUtils.join(new String[]{"", "opt", "node-data", testId}, File.separator);
        String fileName = testId + ".jmx";


        //  每个测试生成一个文件夹
        FileUtils.writeStringToFile(new File(filePath + File.separator + fileName), testRequest.getFileString(), StandardCharsets.UTF_8);
        // 保存测试数据文件
        Map<String, String> testData = testRequest.getTestData();
        if (!CollectionUtils.isEmpty(testData)) {
            for (String k : testData.keySet()) {
                String v = testData.get(k);
                FileUtils.writeStringToFile(new File(filePath + File.separator + k), v, StandardCharsets.UTF_8);
            }
        }

        // 查找镜像
        searchImage(dockerClient, testRequest.getImage());

        ArrayList<String> containerIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String containerName = testId + "-" + i;
            // 创建 hostConfig
            HostConfig hostConfig = HostConfig.newHostConfig();
            String[] envs = getEnvs(testRequest);
            String containerId = DockerClientService.createContainers(dockerClient, containerName, containerImage, hostConfig, envs).getId();
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
            LogUtil.info("Container create started containerId: " + containerId);
            dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback() {
                        @Override
                        public void onComplete() {
                            // 清理文件夹
                            try {
                                FileUtils.forceDelete(new File(filePath));
                                LogUtil.info("Remove dir completed.");
                                if (DockerClientService.existContainer(dockerClient, containerId) > 0) {
                                    DockerClientService.removeContainer(dockerClient, containerId);
                                }
                                LogUtil.info("Remove container completed: " + containerId);
                            } catch (IOException e) {
                                LogUtil.error("Remove dir error: ", e);
                            }
                            LogUtil.info("completed....");
                        }
                    });
        });
    }

    private String[] getEnvs(TestRequest testRequest) {
        Map<String, String> env = testRequest.getEnv();
        return env.keySet().stream().map(k -> k + "=" + env.get(k)).toArray(String[]::new);
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


    public void stopContainer(String testId) {
        LogUtil.info("Receive stop container request, test: {}", testId);
        DockerClient dockerClient = DockerClientService.connectDocker();

        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Collections.singletonList("running"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        // container stop
        list.forEach(container -> DockerClientService.removeContainer(dockerClient, container.getId()));
    }

    public List<Container> taskStatus(String testId) {
        DockerClient dockerClient = DockerClientService.connectDocker();
        List<Container> containerList = dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        // 查询执行的状态
        return containerList;
    }

    public String logContainer(String testId) {
        LogUtil.info("Receive logs container request, test: {}", testId);
        DockerClient dockerClient = DockerClientService.connectDocker();

        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Collections.singletonList("running"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();

        StringBuilder sb = new StringBuilder();
        if (list.size() > 0) {
            try {
                dockerClient.logContainerCmd(list.get(0).getId())
                        .withFollowStream(true)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withTailAll()
                        .exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
                            @Override
                            public void onNext(Frame item) {
                                sb.append(new String(item.getPayload()).trim()).append("\n");
                            }
                        }).awaitCompletion(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LogUtil.error(e);
            }
        }
        return sb.toString();
    }
}
