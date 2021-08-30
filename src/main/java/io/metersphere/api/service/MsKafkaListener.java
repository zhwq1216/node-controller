package io.metersphere.api.service;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.config.FixedTask;
import io.metersphere.api.controller.request.RunRequest;
import io.metersphere.api.jmeter.JMeterService;
import io.metersphere.api.jmeter.utils.FileUtils;
import io.metersphere.api.jmeter.utils.MSException;
import io.metersphere.api.service.utils.ZipSpider;
import io.metersphere.node.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.NewDriver;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;

@Service
public class MsKafkaListener {
    public final static String EXEC_TOPIC = "ms-automation-exec-topic";

    public static final String CONSUME_ID = "ms-api-automation-consume";
    @Resource
    private JMeterService jMeterService;

    private static InputStream getStrToStream(String sInputString) {
        if (StringUtils.isNotEmpty(sInputString)) {
            try {
                ByteArrayInputStream tInputStringStream = new ByteArrayInputStream(sInputString.getBytes());
                return tInputStringStream;
            } catch (Exception ex) {
                ex.printStackTrace();
                MSException.throwException("生成脚本异常");
            }
        }
        return null;
    }

    private void loadJar(String path) {
        try {
            NewDriver.addPath(path);
        } catch (MalformedURLException e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException(e.getMessage());
        }
    }

    @KafkaListener(id = CONSUME_ID, topics = EXEC_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<?, String> record, Acknowledgment ack) {
        LogUtil.info("接收到执行执行请求开始处理");
        try {
            if (record.value() != null) {
                RunRequest request = JSON.parseObject(record.value(), RunRequest.class);
                // 生成执行脚本
                InputStream inputSource = getStrToStream(request.getJmx());
                Object scriptWrapper = SaveService.loadElement(inputSource);
                HashTree testPlan = JMeterService.getHashTree(scriptWrapper);
                //加载附件
                ZipSpider.downloadFiles(request.getUrl(), testPlan);
                if (StringUtils.isNotEmpty(FixedTask.url)) {
                    File file = ZipSpider.downloadFile(FixedTask.url, FileUtils.JAR_FILE_DIR);
                    if (file != null) {
                        ZipSpider.unzip(file.getPath(), FileUtils.JAR_FILE_DIR);
                        this.loadJar(FileUtils.JAR_FILE_DIR);
                    }
                }
                // 开始执行
                jMeterService.run(request, testPlan);
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
        } finally {
            ack.acknowledge();
        }
        LogUtil.info("执行执行请求处理结束");
    }

}
