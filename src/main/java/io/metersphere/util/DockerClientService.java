package io.metersphere.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

public class DockerClientService {

    final String PATH = "/Users/liyuhao/test";

    /**
     * 连接docker服务器
     * @return
     */
    public DockerClient connectDocker(){
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
    public CreateContainerResponse createContainers(DockerClient client, String containerName, String imageName){
        Ports portBindings = new Ports();
        Volume volume = new Volume("/test");
        CreateContainerResponse container = client.createContainerCmd(imageName)
                .withVolumes(volume)
                .withName(containerName)
                .withHostConfig(newHostConfig().withPortBindings(portBindings).withBinds(new Bind(PATH, volume)))
                .exec();
        return container;
    }


    /**
     * 启动容器
     * @param client
     * @param containerId
     */
    public void startContainer(DockerClient client,String containerId){
        client.startContainerCmd(containerId).exec();
    }

    /**
     * 停止容器
     * @param client
     * @param containerId
     */
    public void stopContainer(DockerClient client,String containerId){
        client.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     * @param client
     * @param containerId
     */
    public void removeContainer(DockerClient client,String containerId){
        client.removeContainerCmd(containerId).exec();
    }

}
