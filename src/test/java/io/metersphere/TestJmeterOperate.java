package io.metersphere;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.InvocationBuilder;
import io.metersphere.controller.request.TestRequest;
import io.metersphere.service.JmeterOperateService;
import io.metersphere.util.DockerClientService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class TestJmeterOperate {
    @Resource
    private JmeterOperateService jmeterOperateService;

    @Test
    public void testCreateContainer() throws Exception {
        InputStream in = getClass().getResourceAsStream("/test.jmx");
        String s = IOUtils.toString(in, StandardCharsets.UTF_8);
        TestRequest testRequest = new TestRequest();
        testRequest.setImage("registry.fit2cloud.com/metersphere/jmeter-master:0.0.3");
        testRequest.setFileString(s);
        testRequest.setTestId("test-id");
        testRequest.setTestData(new HashMap<>());
        testRequest.setSize(1);
        jmeterOperateService.startContainer(testRequest);

        Thread.sleep(1000 * 1000L);
    }

    @Test
    public void testSaveFile() throws Exception {
        FileUtils.writeStringToFile(new File("/tmp/a/b/c.txt"), "test", StandardCharsets.UTF_8);
    }

    @Test
    public void testLogContainer() {
        String s = jmeterOperateService.logContainer("test-id");
        System.out.println(s);
    }

    @Test
    public void testContainerLog() throws Exception {
        DockerClient dockerClient = DockerClientService.connectDocker();
        List<Container> containerList = dockerClient.listContainersCmd()
                .withNameFilter(Arrays.asList("test-id-0"))
                .exec();

        containerList.forEach(c -> {
            try {
                dockerClient.logContainerCmd(c.getId())
                        .withFollowStream(true)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withTailAll()
                        .exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
                            @Override
                            public void onNext(Frame item) {
                                System.out.println(item.toString());
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
