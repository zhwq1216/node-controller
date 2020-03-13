package io.metersphere;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                .withShowSize(true)
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("exited"))
                .exec();
        containers.forEach(container -> {
            System.out.println(container);
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
    public void doTestUpload() {
        DockerClient dockerClient = DockerClientService.connectDocker();
        CreateContainerResponse containers = DockerClientService.createContainers(dockerClient, "jmeter4", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.2.1\">\n" +
                "  <hashTree>\n" +
                "    <ThreadGroup guiclass=\"ThreadGroupGui\" testclass=\"ThreadGroup\" testname=\"Group1\" enabled=\"true\">\n" +
                "      <intProp name=\"ThreadGroup.num_threads\">1</intProp>\n" +
                "      <intProp name=\"ThreadGroup.ramp_time\">1</intProp>\n" +
                "      <longProp name=\"ThreadGroup.delay\">0</longProp>\n" +
                "      <longProp name=\"ThreadGroup.duration\">0</longProp>\n" +
                "      <stringProp name=\"ThreadGroup.on_sample_error\">continue</stringProp>\n" +
                "      <boolProp name=\"ThreadGroup.scheduler\">false</boolProp>\n" +
                "      <elementProp name=\"ThreadGroup.main_controller\" elementType=\"LoopController\" guiclass=\"LoopControlPanel\" testclass=\"LoopController\" testname=\"Loop Controller\" enabled=\"true\">\n" +
                "        <boolProp name=\"LoopController.continue_forever\">false</boolProp>\n" +
                "        <stringProp name=\"LoopController.loops\">1</stringProp>\n" +
                "      </elementProp>\n" +
                "    </ThreadGroup>\n" +
                "    <hashTree>\n" +
                "      <HTTPSamplerProxy guiclass=\"HttpTestSampleGui\" testname=\"https://www.baidu.com/\">\n" +
                "        <elementProp name=\"HTTPsampler.Arguments\" elementType=\"Arguments\">\n" +
                "          <collectionProp name=\"Arguments.arguments\"/>\n" +
                "        </elementProp>\n" +
                "        <stringProp name=\"HTTPSampler.domain\">www.baidu.com</stringProp>\n" +
                "        <intProp name=\"HTTPSampler.port\">-1</intProp>\n" +
                "        <elementProp name=\"HTTPSampler.header_manager\" elementType=\"HeaderManager\">\n" +
                "          <collectionProp name=\"HeaderManager.headers\">\n" +
                "            <elementProp name=\"Upgrade-Insecure-Requests\" elementType=\"Header\">\n" +
                "              <stringProp name=\"Header.name\">Upgrade-Insecure-Requests</stringProp>\n" +
                "              <stringProp name=\"Header.value\">1</stringProp>\n" +
                "            </elementProp>\n" +
                "            <elementProp name=\"User-Agent\" elementType=\"Header\">\n" +
                "              <stringProp name=\"Header.name\">User-Agent</stringProp>\n" +
                "              <stringProp name=\"Header.value\">Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36</stringProp>\n" +
                "            </elementProp>\n" +
                "            <elementProp name=\"Sec-Fetch-User\" elementType=\"Header\">\n" +
                "              <stringProp name=\"Header.name\">Sec-Fetch-User</stringProp>\n" +
                "              <stringProp name=\"Header.value\">?1</stringProp>\n" +
                "            </elementProp>\n" +
                "            <elementProp name=\"Accept\" elementType=\"Header\">\n" +
                "              <stringProp name=\"Header.name\">Accept</stringProp>\n" +
                "              <stringProp name=\"Header.value\">text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9</stringProp>\n" +
                "            </elementProp>\n" +
                "          </collectionProp>\n" +
                "        </elementProp>\n" +
                "        <stringProp name=\"HTTPSampler.protocol\">https</stringProp>\n" +
                "        <stringProp name=\"HTTPSampler.path\">/</stringProp>\n" +
                "        <stringProp name=\"HTTPSampler.method\">GET</stringProp>\n" +
                "      </HTTPSamplerProxy>\n" +
                "      <hashTree/>\n" +
                "    </hashTree>\n" +
                "    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan\" enabled=\"true\">\n" +
                "      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n" +
                "      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n" +
                "      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n" +
                "      <stringProp name=\"TestPlan.comments\"></stringProp>\n" +
                "      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n" +
                "      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\">\n" +
                "        <collectionProp name=\"Arguments.arguments\"/>\n" +
                "      </elementProp>\n" +
                "    </TestPlan>\n" +
                "    <hashTree/>\n" +
                "  </hashTree>\n" +
                "</jmeterTestPlan>\n";
        FileUtil.saveFile(content, "/Users/liyuhao/test", "ceshi3.jmx");
        DockerClientService.startContainer(dockerClient, containers.getId());
    }

    @Test
    public void containerStart() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowSize(true)
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("exited"))
                .exec();
        List<Container> collect = containers.stream()
                .filter(container -> container.getImage().equals("registry.fit2cloud.com/metersphere/jmeter-master:0.0.2"))
                .collect(Collectors.toList());
        //
        if (!collect.isEmpty()) {
            DockerClientService.startContainer(dockerClient, collect.get(0).getId());
        } else {
            CreateContainerResponse newContainers = DockerClientService.createContainers(dockerClient, "jmeter", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
            DockerClientService.startContainer(dockerClient, newContainers.getId());
        }

    }

}
