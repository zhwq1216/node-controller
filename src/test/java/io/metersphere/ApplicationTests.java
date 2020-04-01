package io.metersphere;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import io.metersphere.util.DockerClientService;
import io.metersphere.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//@SpringBootTest
class ApplicationTests {

    private static DockerClient dockerClient;

    @BeforeAll
    static void before() {
        String usrHome = System.getProperty("user.home");

        /*DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(usrHome + File.separator + ".docker")
                .build();*/
        dockerClient = DockerClientBuilder.getInstance().build();
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
        //CreateContainerResponse containers = DockerClientService.createContainers(dockerClient, "jmeter4", "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2");
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
        FileUtil.saveFile(content, "/Users/liyuhao/test/test0", "ceshi3.jmx");
        // DockerClientService.startContainer(dockerClient, containers.getId());
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

    @Test
    void testDoTask() {

    }

    // 从本地上传资源到容器
    @Test
    void copyArchiveToContainerCmd() {
        dockerClient.copyArchiveToContainerCmd("18894c7755b1")
                .withHostResource("/Users/liyuhao/test").withRemotePath("/test").exec();
    }

    // 从容器中下载资源到本地
    @Test
    Object copyArchiveFromContainerCmd(DockerClient dockerClient, String containerID, String local, String remote) {
        try {
            InputStream input = dockerClient
                    .copyArchiveFromContainerCmd(containerID, remote)
                    .exec();
            int index;
            byte[] bytes = new byte[1024];
            FileOutputStream downloadFile = new FileOutputStream(local);
            while ((index = input.read(bytes)) != -1) {
                downloadFile.write(bytes, 0, index);
                downloadFile.flush();
            }
            input.close();
            downloadFile.close();
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void taskPerform() {
        // 执行 jmx
        // docker run -it -v /Users/liyuhao/test:/test justb4/jmeter:latest -n -t /test/ceshi3.jmx
        dockerClient.copyArchiveToContainerCmd("18894c7755b106f33b9a8072944d3f8165f3cb131402901c5e4edc3d2975614e")
                .withHostResource("/Users/liyuhao/test/ceshi.jmx").withRemotePath("/test").exec();
        dockerClient.startContainerCmd("18894c7755b106f33b9a8072944d3f8165f3cb131402901c5e4edc3d2975614e").exec();
//        dockerClient.execCreateCmd("18894c7755b106f33b9a8072944d3f8165f3cb131402901c5e4edc3d2975614e").withAttachStdout(true)
//                .withCmd("jmeter", "-n", "-t", "/test/ceshi.jmx").exec();
    }

    @Test
    public void containerStart1() {
        int size = 2;
        String testId = UUID.randomUUID().toString();
        String containerImage = "registry.fit2cloud.com/metersphere/jmeter-master:0.0.2";
        String filePath = "/Users/liyuhao/test";
        String fileName = "ceshi.jmx";
        String content1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.2.1\">\n" +
                "  <hashTree>\n" +
                "    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan\" enabled=\"true\">\n" +
                "      <stringProp name=\"TestPlan.comments\"></stringProp>\n" +
                "      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n" +
                "      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n" +
                "      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n" +
                "      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n" +
                "        <collectionProp name=\"Arguments.arguments\"/>\n" +
                "      </elementProp>\n" +
                "      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n" +
                "    </TestPlan>\n" +
                "    <hashTree>\n" +
                "      <ThreadGroup guiclass=\"ThreadGroupGui\" testclass=\"ThreadGroup\" testname=\"Thread Group\" enabled=\"true\">\n" +
                "        <stringProp name=\"ThreadGroup.on_sample_error\">continue</stringProp>\n" +
                "        <elementProp name=\"ThreadGroup.main_controller\" elementType=\"LoopController\" guiclass=\"LoopControlPanel\" testclass=\"LoopController\" testname=\"Loop Controller\" enabled=\"true\">\n" +
                "          <boolProp name=\"LoopController.continue_forever\">false</boolProp>\n" +
                "          <stringProp name=\"LoopController.loops\">10000</stringProp>\n" +
                "        </elementProp>\n" +
                "        <stringProp name=\"ThreadGroup.num_threads\">1</stringProp>\n" +
                "        <stringProp name=\"ThreadGroup.ramp_time\">1</stringProp>\n" +
                "        <boolProp name=\"ThreadGroup.scheduler\">false</boolProp>\n" +
                "        <stringProp name=\"ThreadGroup.duration\"></stringProp>\n" +
                "        <stringProp name=\"ThreadGroup.delay\"></stringProp>\n" +
                "        <boolProp name=\"ThreadGroup.same_user_on_next_iteration\">true</boolProp>\n" +
                "      </ThreadGroup>\n" +
                "      <hashTree>\n" +
                "        <HTTPSamplerProxy guiclass=\"HttpTestSampleGui\" testclass=\"HTTPSamplerProxy\" testname=\"HTTP Request\" enabled=\"true\">\n" +
                "          <elementProp name=\"HTTPsampler.Arguments\" elementType=\"Arguments\" guiclass=\"HTTPArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n" +
                "            <collectionProp name=\"Arguments.arguments\"/>\n" +
                "          </elementProp>\n" +
                "          <stringProp name=\"HTTPSampler.domain\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.port\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.protocol\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.contentEncoding\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.path\">http://www.baidu.com</stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.method\">GET</stringProp>\n" +
                "          <boolProp name=\"HTTPSampler.follow_redirects\">true</boolProp>\n" +
                "          <boolProp name=\"HTTPSampler.auto_redirects\">false</boolProp>\n" +
                "          <boolProp name=\"HTTPSampler.use_keepalive\">true</boolProp>\n" +
                "          <boolProp name=\"HTTPSampler.DO_MULTIPART_POST\">false</boolProp>\n" +
                "          <stringProp name=\"HTTPSampler.embedded_url_re\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.connect_timeout\"></stringProp>\n" +
                "          <stringProp name=\"HTTPSampler.response_timeout\"></stringProp>\n" +
                "        </HTTPSamplerProxy>\n" +
                "        <hashTree/>\n" +
                "      </hashTree>\n" +
                "    </hashTree>\n" +
                "  </hashTree>\n" +
                "</jmeterTestPlan>";

        FileUtil.saveFile(content1, filePath, fileName);

        ArrayList<String> containerIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String containerName = testId + i;
            String containerId = DockerClientService.createContainers(dockerClient, containerName, containerImage).getId();
            //  从主机复制文件到容器
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withHostResource(filePath)
                    .withDirChildrenOnly(false)
                    .withRemotePath("/")
                    .exec();
            containerIdList.add(containerId);
        }

        containerIdList.forEach(containerId -> {
            DockerClientService.startContainer(dockerClient, containerId);
        });
    }

    @Test
    public void getTaskStatus() {
        List<Container> list = dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("created", "restarting", "running", "paused", "exited"))
                .withNameFilter(Arrays.asList("6f66f8e2-ae6b-4865-95d2-6cb3b16845b7"))
                .exec();
        list.forEach(s -> {
            System.out.println(s);
        });
        // 查询执行的状态
    }


    @Test
    public void containerStop() {
        // container filter
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withStatusFilter(Arrays.asList("running"))
                .withNameFilter(Arrays.asList("3dda5fea-1005-4b1b-9d5e-b2c841e55b8d"))
                .exec();
        // container stop
        list.forEach(container -> DockerClientService.stopContainer(dockerClient, container.getId()));
    }

    @Test
    public void searchImage() {
        List<Image> imageList = dockerClient.listImagesCmd().exec();
        List<Image> collect = imageList.stream().filter(image -> {
            String[] repoTags = image.getRepoTags();
            if (repoTags == null) {
                return false;
            }
            for (String repoTag : repoTags) {
                if (repoTag.equals("registry.fit2cloud.com/metersphere/jmeter-master:0.0.2")) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        System.out.println(collect.size());
    }
}
