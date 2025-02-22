package io.metersphere;

import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.api.jmeter.utils.JMeterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@ServletComponentScan
@EnableConfigurationProperties({
        JMeterProperties.class,
})
@PropertySource(value = {
        "classpath:/base.properties",
        "file:/opt/metersphere/conf/metersphere.properties",
}, encoding = "UTF-8", ignoreResourceNotFound = true)
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommonBeanFactory commonBeanFactory() {
        return new CommonBeanFactory();
    }
}
