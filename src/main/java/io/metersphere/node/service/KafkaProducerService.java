package io.metersphere.node.service;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;

public class KafkaProducerService {
    private KafkaTemplate kafkaTemplate;

    public void init(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (kafkaTemplate == null) {
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.StringSerializer");
            DefaultKafkaProducerFactory pf = new DefaultKafkaProducerFactory(props);
            KafkaTemplate kafkaTemplate = new KafkaTemplate(pf, true);
            this.kafkaTemplate = kafkaTemplate;
        }
    }

    public void sendMessage(String topic, String report) {
        this.kafkaTemplate.send(topic, report);
    }
}
