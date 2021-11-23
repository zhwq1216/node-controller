package io.metersphere.api.service;
import com.alibaba.fastjson.JSON;
import io.metersphere.api.config.KafkaConfig;
import io.metersphere.api.controller.request.RunRequest;
import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.node.util.LogUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

public class MsKafkaListener {

    public static final String CONSUME_ID = "ms-api-automation-consume";

    private JmeterExecuteService jmeterExecuteService;

    @KafkaListener(id = CONSUME_ID, topics = KafkaConfig.EXEC_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<?, String> record, Acknowledgment ack) {
        LogUtil.info("接收到执行执行请求开始处理");
        if (jmeterExecuteService == null) {
            jmeterExecuteService = CommonBeanFactory.getBean(JmeterExecuteService.class);
        }
        try {
            if (record.value() != null) {
                RunRequest request = JSON.parseObject(record.value(), RunRequest.class);
                jmeterExecuteService.runStart(request);
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
        } finally {
            ack.acknowledge();
        }
        LogUtil.info("执行执行请求处理结束");
    }
}
