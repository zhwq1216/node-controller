package io.metersphere;

import io.metersphere.controller.request.TestRequest;
import io.metersphere.service.JmeterOperateService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

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
    }

    @Test
    public void testSaveFile() throws Exception {
        FileUtils.writeStringToFile(new File("/tmp/a/b/c.txt"), "test", StandardCharsets.UTF_8);
    }
}
