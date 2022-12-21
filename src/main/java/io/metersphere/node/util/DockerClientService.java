package io.metersphere.node.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.metersphere.node.controller.request.TestRequest;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DockerClientService {

    @Value("${jmeter.cpu.set:}")
    private String jmeterCores;
    @Value("${report.cpu.set:}")
    private String reportCores;
    @Value("${split.file.size:500000}")
    private long splitFileSize;

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(jmeterCores) || StringUtils.isBlank(jmeterCores)) {
            int cores = Runtime.getRuntime().availableProcessors();
            if (cores >= 4) {
                jmeterCores = "1-" + (cores - 1);
                reportCores = "0";
            }
            if (cores >= 8) {
                jmeterCores = "2-" + (cores - 1);
                reportCores = "0-1";
            }
        }
    }

    /**
     * 连接docker服务器
     *
     * @return
     */
    public DockerClient connectDocker() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * 创建容器
     *
     * @param client
     * @return
     */
    public CreateContainerResponse createContainers(DockerClient client, TestRequest request, String testId, String imageName) {

        // 创建 hostConfig
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode("host")
                .withCpusetCpus(jmeterCores);

        CreateContainerResponse container = client.createContainerCmd(imageName)
                .withName(testId)
                .withHostConfig(hostConfig)
                .withEnv(getEnvs(request))
                .withVolumes(new Volume("/test"), new Volume("/jmeter-log"))
                .exec();
        return container;
    }

    public CreateContainerResponse createReportContainers(DockerClient client, TestRequest request, String testId, String imageName) {
        // 创建 hostConfig
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode("host")
                .withCpusetCpus(reportCores)
                .withVolumesFrom(new VolumesFrom(testId));
        CreateContainerResponse container = client.createContainerCmd(imageName)
                .withName(testId + "_report")
                .withHostConfig(hostConfig)
                .withEntrypoint("/bin/sh", "-c", "/generate-report.sh")
                .withEnv(getEnvs(request))
                .exec();
        return container;
    }


    /**
     * 启动容器
     *
     * @param client
     * @param containerId
     */
    public void startContainer(DockerClient client, String containerId) {
        client.startContainerCmd(containerId).exec();
    }

    /**
     * 停止容器
     *
     * @param client
     * @param containerId
     */
    public void stopContainer(DockerClient client, String containerId) {
        client.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     *
     * @param client
     * @param containerId
     */
    public void removeContainer(DockerClient client, String containerId) {
        client.removeContainerCmd(containerId)
                .withForce(true)
                .withRemoveVolumes(true)
                .exec();
    }

    /**
     * 容器是否存在
     *
     * @param client
     * @param containerId
     */
    public int existContainer(DockerClient client, String containerId) {
        List<Container> list = client.listContainersCmd()
                .withShowAll(true)
                .withIdFilter(Collections.singleton(containerId))
                .exec();
        return list.size();
    }

    private String[] getEnvs(TestRequest testRequest) {
        Map<String, String> env = testRequest.getEnv();
        env.put("SPLIT_FILE_SIZE", String.valueOf(splitFileSize));
        return env.keySet().stream().map(k -> k + "=" + env.get(k)).toArray(String[]::new);
    }
}
