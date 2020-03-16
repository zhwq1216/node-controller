package io.metersphere.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerClientService {

    /**
     * 连接docker服务器
     * @return
     */
    public static DockerClient connectDocker(){
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        Info info = dockerClient.infoCmd().exec();
        System.out.println("docker的环境信息如下：=================");
        System.out.println(info);
        return dockerClient;
    }

    /**
     * 创建容器
     * @param client
     * @return
     */
    public static CreateContainerResponse createContainers(DockerClient client, String containerName, String imageName){
        CreateContainerResponse container = client.createContainerCmd(imageName)
                .withName(containerName)
                .exec();
        return container;
    }


    /**
     * 启动容器
     * @param client
     * @param containerId
     */
    public static void startContainer(DockerClient client,String containerId){
        client.startContainerCmd(containerId).exec();
    }

    /**
     * 停止容器
     * @param client
     * @param containerId
     */
    public static void stopContainer(DockerClient client,String containerId){
        client.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     * @param client
     * @param containerId
     */
    public static void removeContainer(DockerClient client,String containerId){
        client.removeContainerCmd(containerId).exec();
    }

}
