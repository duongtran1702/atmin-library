package atmin.common.config;

import atmin.common.reactive.AtminReactiveTraceFilter;
import atmin.common.reactive.AtminGatewayErrorWebExceptionHandler;
import atmin.common.reactive.ReactiveCoreExceptionHandler;
import atmin.common.reactive.ReactiveJwtExceptionHandler;
import atmin.common.reactive.ReactiveValidationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import tools.jackson.databind.ObjectMapper;

/** Auto-configuration for Spring WebFlux applications. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ServerWebExchange.class, ControllerAdvice.class})
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(AtminExceptionProperties.class)
public class AtminReactiveExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReactiveCoreExceptionHandler.class)
    ReactiveCoreExceptionHandlerAdvice reactiveCoreExceptionHandler(AtminExceptionProperties properties) {
        return new ReactiveCoreExceptionHandlerAdvice(properties);
    }

    @Bean
    @ConditionalOnMissingBean(AtminReactiveTraceFilter.class)
    AtminReactiveTraceFilter atminReactiveTraceFilter(AtminExceptionProperties properties) {
        return new AtminReactiveTraceFilter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
    static class ValidationConfiguration {
        @Bean
        @ConditionalOnMissingBean(ReactiveValidationExceptionHandler.class)
        ReactiveValidationExceptionHandlerAdvice reactiveValidationExceptionHandler(
                AtminExceptionProperties properties) {
            return new ReactiveValidationExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.jsonwebtoken.JwtException")
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
    static class JwtConfiguration {
        @Bean
        @ConditionalOnMissingBean(ReactiveJwtExceptionHandler.class)
        ReactiveJwtExceptionHandlerAdvice reactiveJwtExceptionHandler(AtminExceptionProperties properties) {
            return new ReactiveJwtExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.security.web.server.ServerAuthenticationEntryPoint")
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-filter-enabled", matchIfMissing = true)
    @Import(ReactiveSecurityExceptionConfig.class)
    static class SecurityConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "org.springframework.cloud.gateway.route.Route",
            "org.springframework.boot.webflux.error.ErrorWebExceptionHandler"
    })
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "gateway-enabled", matchIfMissing = true)
    @ConditionalOnBean(ObjectMapper.class)
    static class GatewayConfiguration {
        @Bean(name = "atminGatewayErrorWebExceptionHandler")
        @ConditionalOnMissingBean(AtminGatewayErrorWebExceptionHandler.class)
        AtminGatewayErrorWebExceptionHandler atminGatewayErrorWebExceptionHandler(
                ObjectMapper objectMapper, AtminExceptionProperties properties) {
            return new AtminGatewayErrorWebExceptionHandler(objectMapper, properties);
        }
    }
}
