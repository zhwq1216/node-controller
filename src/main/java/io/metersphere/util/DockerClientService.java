package io.metersphere.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import io.metersphere.controller.request.DockerLoginRequest;

public class DockerClientService {

    /**
     * 连接docker服务器
     *
     * @return
     */
    public static DockerClient connectDocker() {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        Info info = dockerClient.infoCmd().exec();
        LogUtil.info("docker的环境信息如下：=================");
        LogUtil.info(info);
        return dockerClient;
    }

    public static DockerClient connectDocker(DockerLoginRequest request) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUrl(request.getRegistry())
                .withRegistryUsername(request.getUsername())
                .withRegistryPassword(request.getPassword())
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
        Info info = dockerClient.infoCmd().exec();
        LogUtil.info("docker的环境信息如下：=================");
        LogUtil.info(info);
        return dockerClient;
    }

    /**
     * 创建容器
     *
     * @param client
     * @return
     */
    public static CreateContainerResponse createContainers(DockerClient client, String containerName, String imageName) {
        CreateContainerResponse container = client.createContainerCmd(imageName)
                .withName(containerName)
                .exec();
        return container;
    }


    /**
     * 启动容器
     *
     * @param client
     * @param containerId
     */
    public static void startContainer(DockerClient client, String containerId) {
        client.startContainerCmd(containerId).exec();
    }

    /**
     * 停止容器
     *
     * @param client
     * @param containerId
     */
    public static void stopContainer(DockerClient client, String containerId) {
        client.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     *
     * @param client
     * @param containerId
     */
    public static void removeContainer(DockerClient client, String containerId) {
        client.removeContainerCmd(containerId).exec();
    }

}
