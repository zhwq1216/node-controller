package io.metersphere.api.service;

import io.metersphere.api.config.KafkaConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProducerService {
    private KafkaTemplate kafkaTemplate;

    public String init(Map<String, Object> producerProps) {
        try {
            if (kafkaTemplate == null) {
                producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringSerializer");
                producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringSerializer");
                DefaultKafkaProducerFactory pf = new DefaultKafkaProducerFactory<>(producerProps);
                KafkaTemplate kafkaTemplate = new KafkaTemplate(pf, true);
                this.kafkaTemplate = kafkaTemplate;
            }
            return "SUCCESS";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void send(String message) {
        if (this.kafkaTemplate != null) {
            this.kafkaTemplate.send(KafkaConfig.TOPICS, message);
            this.kafkaTemplate.flush();
        }
    }
}
