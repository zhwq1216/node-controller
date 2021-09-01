package io.metersphere.api.controller;

import io.metersphere.api.service.MsKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("consumer")
public class ConsumerController {
    /**
     * 应用程序上行文
     */
    @Resource
    ApplicationContext context;
    /**
     * 监听器容器工厂
     */
    @Resource
    ConcurrentKafkaListenerContainerFactory<Object, Object> containerFactory;
    /**
     * 所有@KafkaListener这个注解所标注的方法都会被注册在这里面中
     */
    @Resource
    KafkaListenerEndpointRegistry registry;

    private MsKafkaListener listener;

    /**
     * 创建消费者分组
     */
    @PostMapping("/create")
    public void create(@RequestBody Map<String, Object> consumerProps) {
        try {
            if (consumerProps != null && consumerProps.size() > 0 && listener == null) {
                consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer");
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer");
                // 设置监听器容器工厂
                containerFactory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps));
                // 获取监听类实例
                listener = context.getBean(MsKafkaListener.class);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 停止所有消费监听
     */
    @GetMapping("/stop")
    public void stop() {
        registry.getListenerContainers().forEach(container -> {
            container.stop();
        });
    }

    /**
     * 获取监听类实例
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MsKafkaListener listener() {
        return new MsKafkaListener();
    }
}
