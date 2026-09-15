package atmin.common.config;

import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.exception.handler.JwtExceptionHandler;
import atmin.common.exception.handler.MicroserviceExceptionHandler;
import atmin.common.exception.handler.SecurityExceptionHandler;
import atmin.common.exception.handler.SpringSecurityExceptionHandler;
import atmin.common.exception.handler.StorageExceptionHandler;
import atmin.common.exception.handler.ValidationExceptionHandler;
import atmin.common.exception.handler.WebClientExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Auto-configures Atmin's servlet exception handlers.
 *
 * <p>Every optional integration is isolated in a nested configuration so the
 * library remains safe when Spring Security, JJWT, or Bean Validation are not
 * present in the consuming application.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ControllerAdvice.class)
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(AtminExceptionProperties.class)
public class AtminExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AtminServletTraceFilter.class)
    AtminServletTraceFilter atminServletTraceFilter(AtminExceptionProperties properties) {
        return new AtminServletTraceFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(CoreExceptionHandler.class)
    CoreExceptionHandlerAdvice coreExceptionHandler(AtminExceptionProperties properties) {
        return new CoreExceptionHandlerAdvice(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.client.RestClientException")
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "microservice-enabled", matchIfMissing = true)
    static class MicroserviceHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(MicroserviceExceptionHandler.class)
        MicroserviceExceptionHandlerAdvice microserviceExceptionHandler(AtminExceptionProperties properties) {
            return new MicroserviceExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClientException")
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "microservice-enabled", matchIfMissing = true)
    static class WebClientHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(WebClientExceptionHandler.class)
        WebClientExceptionHandlerAdvice webClientExceptionHandler(AtminExceptionProperties properties) {
            return new WebClientExceptionHandlerAdvice(properties);
        }
    }

    /** Uses the original combined handler when both Security and JJWT exist. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
    @ConditionalOnClass(name = {
            "org.springframework.security.core.AuthenticationException",
            "org.springframework.security.access.AccessDeniedException",
            "io.jsonwebtoken.JwtException"
    })
    static class SecurityHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(SecurityExceptionHandler.class)
        SecurityExceptionHandlerAdvice securityExceptionHandler(AtminExceptionProperties properties) {
            return new SecurityExceptionHandlerAdvice(properties);
        }
    }

    /** Supports Spring Security applications that do not use JJWT. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
    @ConditionalOnClass(name = {
            "org.springframework.security.core.AuthenticationException",
            "org.springframework.security.access.AccessDeniedException"
    })
    @ConditionalOnMissingClass("io.jsonwebtoken.JwtException")
    static class SecurityOnlyHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(SpringSecurityExceptionHandler.class)
        SpringSecurityExceptionHandlerAdvice springSecurityExceptionHandler(AtminExceptionProperties properties) {
            return new SpringSecurityExceptionHandlerAdvice(properties);
        }
    }

    /** Supports JJWT applications that do not use Spring Security. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
    @ConditionalOnClass(name = "io.jsonwebtoken.JwtException")
    @ConditionalOnMissingClass("org.springframework.security.core.AuthenticationException")
    static class JwtOnlyHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(JwtExceptionHandler.class)
        JwtExceptionHandlerAdvice jwtExceptionHandler(AtminExceptionProperties properties) {
            return new JwtExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "storage-enabled", matchIfMissing = true)
    static class StorageHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(StorageExceptionHandler.class)
        StorageExceptionHandlerAdvice storageExceptionHandler(AtminExceptionProperties properties) {
            return new StorageExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
    static class ValidationHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(ValidationExceptionHandler.class)
        ValidationExceptionHandlerAdvice validationExceptionHandler(AtminExceptionProperties properties) {
            return new ValidationExceptionHandlerAdvice(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-filter-enabled", matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.security.web.AuthenticationEntryPoint")
    @Import(SecurityExceptionConfig.class)
    static class SecurityFilterConfiguration {
    }
}
