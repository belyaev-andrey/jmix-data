package io.jmix.data.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = "io.jmix.data")
@PropertySource("classpath:jmix-data.properties")
public class JmixConfiguration {
}
