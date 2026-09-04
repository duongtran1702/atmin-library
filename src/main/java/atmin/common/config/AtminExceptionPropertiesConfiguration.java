package atmin.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that registers AtminExceptionProperties as a ConfigurationProperties bean.
 * This prevents duplicate bean registration issues when both autoconfiguration
 * and manual @EnableAtminExceptionHandling are active.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AtminExceptionProperties.class)
public class AtminExceptionPropertiesConfiguration {
}
