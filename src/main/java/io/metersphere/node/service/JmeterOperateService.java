package io.metersphere.node.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.InvocationBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.metersphere.node.controller.request.TestRequest;
import io.metersphere.node.util.CompressUtils;
import io.metersphere.node.util.DockerClientService;
import io.metersphere.node.util.K8sPodClientService;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Service
public class JmeterOperateService {
    @Resource
    private KafkaProducerService kafkaProducerService;
    @Resource
    private DockerClientService dockerClientService;

    @Resource
    private K8sPodClientService k8sPodClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void startContainer(TestRequest testRequest) {

        if (k8sPodClientService.isInK8sCluster()) {
            k8sPodClientService.startTestPod(testRequest);
            return;
        }
        Map<String, String> env = testRequest.getEnv();
        String testId = env.get("TEST_ID");
        LoggerUtil.info("Receive start container request, test id: " + testId);
        String bootstrapServers = env.get("BOOTSTRAP_SERVERS");
        // 检查kafka连通性
        checkKafka(bootstrapServers);
        // 初始化kafka
        kafkaProducerService.init(bootstrapServers);

        DockerClient dockerClient = dockerClientService.connectDocker();

        String containerImage = testRequest.getImage();

        // 查找镜像
        searchImage(dockerClient, testRequest.getImage());
        // 检查容器是否存在
        checkContainerExists(dockerClient, testId);
        // 启动测试
        startContainer(testRequest, dockerClient, testId, containerImage);
    }

    private void startContainer(TestRequest testRequest, DockerClient dockerClient, String testId, String containerImage) {
        String topic = testRequest.getEnv().getOrDefault("LOG_TOPIC", "JMETER_LOGS");
        String reportId = testRequest.getEnv().get("REPORT_ID");


        String testContainerId = dockerClientService.createContainers(dockerClient, testRequest, testId, containerImage).getId();
        dockerClientService.startContainer(dockerClient, testContainerId);
        LoggerUtil.info("Container create started testContainerId: " + testContainerId);

        String reportContainerId = dockerClientService.createReportContainers(dockerClient, testRequest, testId, containerImage).getId();
        dockerClientService.startContainer(dockerClient, reportContainerId);
        LoggerUtil.info("Container create started reportContainerId: " + reportContainerId);


        removeContainer(dockerClient, testContainerId);

        removeContainer(dockerClient, reportContainerId);

        dockerClient.logContainerCmd(testContainerId)
                .withFollowStream(true)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll()
                .exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
                    @Override
                    public void onNext(Frame item) {
                        String log = new String(item.getPayload()).trim();
                        String oomMessage = "There is insufficient memory for the Java Runtime Environment to continue.";
                        String oomMessage2 = "java.lang.OutOfMemoryError";
                        if (StringUtils.contains(log, oomMessage) || StringUtils.contains(log, oomMessage2)) {
                            LoggerUtil.info("handle out of memory error.");
                            // oom 退出
                            String[] contents = new String[]{reportId, "none", "0", oomMessage};
                            String message = StringUtils.join(contents, " ");
                            kafkaProducerService.sendMessage(topic, message);
                            stopContainer(testId);
                        }
                        LoggerUtil.info(log);
                    }
                });
    }

    private void removeContainer(DockerClient dockerClient, String containerId) {
        dockerClient.waitContainerCmd(containerId)
                .exec(new WaitContainerResultCallback() {
                    @Override
                    public void onComplete() {
                        // 清理文件夹
                        try {
                            if (dockerClientService.existContainer(dockerClient, containerId) > 0) {
//                                copyTestResources(dockerClient, testContainerId, reportId, resourceIndex);
                                dockerClientService.removeContainer(dockerClient, containerId);
                            }
                            LoggerUtil.info("Remove container completed: " + containerId);
                        } catch (Exception e) {
                            LoggerUtil.error("Remove container error: ", e);
                        }
                        LoggerUtil.info("completed....");
                    }
                });
    }

    private void copyTestResources(DockerClient dockerClient, String containerId, String reportId, String resourceIndex) throws IOException {
        InputStream testIn = dockerClient
                .copyArchiveFromContainerCmd(containerId, "/test/")
                .exec();
        testIn.available();
        String pathname = reportId + "_" + resourceIndex;
        File dir = new File(pathname);

        FileUtils.forceMkdir(dir);
        File testDir = new File(pathname + "/test.tar");
        FileUtils.copyInputStreamToFile(testIn, testDir);


        InputStream jtlIn = dockerClient
                .copyArchiveFromContainerCmd(containerId, "/jmeter-log/" + reportId + "_" + resourceIndex + ".jtl")
                .exec();
        jtlIn.available();
        File jtl = new File(pathname + "/report.jtl.tar");
        FileUtils.copyInputStreamToFile(jtlIn, jtl);

        InputStream logIn = dockerClient
                .copyArchiveFromContainerCmd(containerId, "/jmeter-log/jmeter.log")
                .exec();
        logIn.available();
        File log = new File(pathname + "/jmeter.log.tar");
        FileUtils.copyInputStreamToFile(logIn, log);

        InputStream generateLogIn = dockerClient
                .copyArchiveFromContainerCmd(containerId, "/jmeter-log/generate-report.log")
                .exec();
        generateLogIn.available();
        File generateLog = new File(pathname + "/generate-report.log.tar");
        FileUtils.copyInputStreamToFile(generateLogIn, generateLog);

        try (
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(reportId + "_" + resourceIndex + ".zip"))
        ) {
            CompressUtils.zipDirectory(dir, zipOutputStream, "");
            FileUtils.forceDelete(dir);
        }
    }

    private void checkContainerExists(DockerClient dockerClient, String testId) {
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(container -> dockerClientService.removeContainer(dockerClient, container.getId()));
        }
    }

    private void checkKafka(String bootstrapServers) {
        String[] servers = StringUtils.split(bootstrapServers, ",");
        try {
            for (String s : servers) {
                String[] ipAndPort = s.split(":");
                //1,建立tcp
                String ip = ipAndPort[0];
                int port = Integer.parseInt(ipAndPort[1]);
                try (
                        Socket soc = new Socket()
                ) {
                    soc.connect(new InetSocketAddress(ip, port), 1000); // 1s timeout
                }
            }
        } catch (Exception e) {
            LoggerUtil.error(e);
            throw new RuntimeException("Failed to connect to Kafka");
        }
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
            throw new RuntimeException("Image Not Found: " + imageName);
        }
    }


    public void stopContainer(String testId) {
        LoggerUtil.info("Receive stop container request, test: " + testId);
        if (k8sPodClientService.isInK8sCluster()) {
            k8sPodClientService.stopTestPod(testId);
            return;
        }
        DockerClient dockerClient = dockerClientService.connectDocker();

        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Collections.singletonList("running"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        // container stop
        list.forEach(container -> dockerClientService.removeContainer(dockerClient, container.getId()));
    }

    public List<Container> taskStatus(String testId) {
        LoggerUtil.info("获取任务状态, testId="+testId);
        if(k8sPodClientService.isInK8sCluster()) {
            V1Pod v1Pod = k8sPodClientService.podStatus(testId);

            if(v1Pod==null){
                return Collections.emptyList();
            }
            String statusJson = "[{\"status\":\"running\"}]";
            try{
                final List<Container> containers = objectMapper
                    .readValue(statusJson, new TypeReference<List<Container>>() {
                    });
                return containers;
            }catch (Exception ex) {
                LoggerUtil.error("获取任务状态转化json对象异常, testId="+testId, ex);
                return Collections.emptyList();
            }
        }


        DockerClient dockerClient = dockerClientService.connectDocker();
        List<Container> containerList = dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Collections.singletonList(testId))
                .exec();
        // 查询执行的状态
        return containerList;
    }

    public String logContainer(String testId) {
        LoggerUtil.info("Receive logs container request, test: " + testId);
        if(k8sPodClientService.isInK8sCluster()) {
            return k8sPodClientService.podLog(testId);
        }
        DockerClient dockerClient = dockerClientService.connectDocker();

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
                LoggerUtil.error(e);
            }
        }
        return sb.toString();
    }
}
