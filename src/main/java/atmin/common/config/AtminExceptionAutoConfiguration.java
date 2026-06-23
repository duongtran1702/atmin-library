package atmin.common.config;

import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.exception.handler.SecurityExceptionHandler;
import atmin.common.exception.handler.StorageExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Spring Boot Auto-Configuration for Atmin exception handlers.
 *
 * <p>This auto-configuration is activated automatically when the library
 * is on the classpath. It registers exception handlers based on available
 * dependencies:</p>
 *
 * <ul>
 *   <li><strong>CoreExceptionHandler</strong> — always registered in web applications</li>
 *   <li><strong>SecurityExceptionHandler</strong> — only if Spring Security + JWT are on classpath</li>
 *   <li><strong>StorageExceptionHandler</strong> — only if Spring Web (multipart) is on classpath</li>
 * </ul>
 *
 * <p><strong>Design note:</strong> The handler classes (CoreExceptionHandler,
 * SecurityExceptionHandler, StorageExceptionHandler) intentionally do NOT have
 * {@code @RestControllerAdvice} or {@code @Component} annotations. This prevents
 * them from being auto-discovered by component scanning when a consumer project
 * uses a base package that overlaps with {@code atmin.*}. Instead, they are registered
 * exclusively through this auto-configuration using wrapper classes annotated with
 * {@code @ControllerAdvice}.</p>
 *
 * <p><strong>Important:</strong> Each inner wrapper class has its own
 * {@code @ConditionalOnClass} annotation to prevent component scanning from
 * loading them when required dependencies are missing. This is necessary because
 * component scanning bypasses conditions on the enclosing configuration class.</p>
 *
 * <p>If you prefer manual control, use {@link atmin.common.annotation.EnableAtminExceptionHandling}
 * annotation instead, and exclude this auto-configuration:</p>
 * <pre>
 * &#64;SpringBootApplication(exclude = AtminExceptionAutoConfiguration.class)
 * &#64;EnableAtminExceptionHandling(enableSecurityHandlers = true)
 * public class MyApp { }
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AtminExceptionProperties.class)
public class AtminExceptionAutoConfiguration {

    /**
     * Core exception handlers — always active in web applications.
     * Wrapped with @ControllerAdvice so Spring recognizes @ExceptionHandler methods.
     * No @ConditionalOnClass needed here since CoreExceptionHandler only uses
     * standard Spring Web classes which are always present in a web application.
     */
    @ControllerAdvice
    static class CoreExceptionHandlerAdvice extends CoreExceptionHandler {
        public CoreExceptionHandlerAdvice(AtminExceptionProperties properties) {
            super(properties);
        }
    }

    @Bean
    @ConditionalOnMissingBean(CoreExceptionHandler.class)
    public CoreExceptionHandler coreExceptionHandler(AtminExceptionProperties properties) {
        return new CoreExceptionHandlerAdvice(properties);
    }

    /**
     * Security exception handlers — only if Spring Security AND JWT are available.
     */
    @Configuration
    @ConditionalOnClass(name = {
            "org.springframework.security.core.AuthenticationException",
            "io.jsonwebtoken.JwtException",
            "org.springframework.security.access.AccessDeniedException"
    })
    static class SecurityHandlerConfiguration {

        /**
         * Has its own @ConditionalOnClass to prevent component scanning from
         * loading this class when Spring Security / JWT are not on classpath.
         * Component scanning bypasses conditions on enclosing classes.
         */
        @ControllerAdvice
        @ConditionalOnClass(name = {
                "org.springframework.security.core.AuthenticationException",
                "io.jsonwebtoken.JwtException",
                "org.springframework.security.access.AccessDeniedException"
        })
        static class SecurityExceptionHandlerAdvice extends SecurityExceptionHandler {
            public SecurityExceptionHandlerAdvice(AtminExceptionProperties properties) {
                super(properties);
            }
        }

        @Bean
        @ConditionalOnMissingBean(SecurityExceptionHandler.class)
        public SecurityExceptionHandler securityExceptionHandler(AtminExceptionProperties properties) {
            return new SecurityExceptionHandlerAdvice(properties);
        }
    }

    /**
     * Storage exception handlers — always available since MaxUploadSizeExceededException
     * is part of spring-web.
     */
    @Configuration
    static class StorageHandlerConfiguration {

        @ControllerAdvice
        static class StorageExceptionHandlerAdvice extends StorageExceptionHandler {
            public StorageExceptionHandlerAdvice(AtminExceptionProperties properties) {
                super(properties);
            }
        }

        @Bean
        @ConditionalOnMissingBean(StorageExceptionHandler.class)
        public StorageExceptionHandler storageExceptionHandler(AtminExceptionProperties properties) {
            return new StorageExceptionHandlerAdvice(properties);
        }
    }

    /**
     * Security filter exceptions configuration — only if Spring Security Web is available.
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.security.web.AuthenticationEntryPoint")
    @Import(SecurityExceptionConfig.class)
    static class SecurityFilterConfiguration {
    }
}
