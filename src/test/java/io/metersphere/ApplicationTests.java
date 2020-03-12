package io.metersphere;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import io.metersphere.util.DockerClientService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

//@SpringBootTest
class ApplicationTests {

    private static DockerClient dockerClient;

    @BeforeAll
    static void before() {
        String usrHome = System.getProperty("user.home");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(usrHome + File.separator + ".docker")
                .build();
        dockerClient = DockerClientBuilder.getInstance(config).build();
    }

    @Test
    void createContainer() throws Exception {

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(PortBinding.parse("9999:27017"))
                .withBinds(Bind.parse("/tmp/db:/data/db"));

        dockerClient.pullImageCmd("mongo:3.6")
                .exec(new PullImageResultCallback() {

                })
                .awaitCompletion();

        CreateContainerResponse container
                = dockerClient.createContainerCmd("mongo:3.6")
                .withCmd("--bind_ip_all")
                .withName("mongo")
                .withHostName("baeldung")
                .withEnv("MONGO_LATEST_VERSION=3.6")
                .withHostConfig(hostConfig)
                .exec();

    }

    @Test
    public void testListContainers() {
        List<Container> containers = dockerClient.listContainersCmd()
//                .withShowSize(true)
//                .withShowAll(true)
//                .withStatusFilter(Arrays.asList("exited"))
                .exec();
        containers.forEach(container -> {
            System.out.println(container.getNames());
        });
    }

    @Test
    public void testImages() {
        List<Image> images = dockerClient.listImagesCmd()
                .withShowAll(true)
                .exec();
        images.forEach(image -> {
            System.out.println(image);
        });
    }

    @Test
    public void test() {
        DockerClientService clientService = new DockerClientService();
        DockerClient dockerClient = clientService.connectDocker();
        CreateContainerResponse containers = clientService.createContainers(dockerClient, "jmeter", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
        clientService.startContainer(dockerClient, containers.getId());
    }

}
