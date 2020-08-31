package io.metersphere.node;

import io.metersphere.node.config.JmeterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@EnableConfigurationProperties({
        JmeterProperties.class
})
@PropertySource(value = {"file:/opt/metersphere/conf/metersphere.properties"}, encoding = "UTF-8", ignoreResourceNotFound = true)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
