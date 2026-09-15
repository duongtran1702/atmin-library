package atmin.common.config;

import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.exception.handler.JwtExceptionHandler;
import atmin.common.exception.handler.MicroserviceExceptionHandler;
import atmin.common.exception.handler.SecurityExceptionHandler;
import atmin.common.exception.handler.SpringSecurityExceptionHandler;
import atmin.common.exception.handler.StorageExceptionHandler;
import atmin.common.exception.handler.ValidationExceptionHandler;
import atmin.common.exception.handler.WebClientExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Package-private advice types used by auto-configuration and the legacy
 * {@code @EnableAtminExceptionHandling} import selector.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "enabled", matchIfMissing = true)
class CoreExceptionHandlerAdvice extends CoreExceptionHandler {
    CoreExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnClass(name = "org.springframework.web.client.RestClientException")
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "microservice-enabled", matchIfMissing = true)
class MicroserviceExceptionHandlerAdvice extends MicroserviceExceptionHandler {
    MicroserviceExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClientException")
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "microservice-enabled", matchIfMissing = true)
class WebClientExceptionHandlerAdvice extends WebClientExceptionHandler {
    WebClientExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnClass(name = {
        "org.springframework.security.core.AuthenticationException",
        "org.springframework.security.access.AccessDeniedException",
        "io.jsonwebtoken.JwtException"
})
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
class SecurityExceptionHandlerAdvice extends SecurityExceptionHandler {
    SecurityExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnClass(name = {
        "org.springframework.security.core.AuthenticationException",
        "org.springframework.security.access.AccessDeniedException"
})
@ConditionalOnMissingClass("io.jsonwebtoken.JwtException")
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
class SpringSecurityExceptionHandlerAdvice extends SpringSecurityExceptionHandler {
    SpringSecurityExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnClass(name = "io.jsonwebtoken.JwtException")
@ConditionalOnMissingClass("org.springframework.security.core.AuthenticationException")
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
class JwtExceptionHandlerAdvice extends JwtExceptionHandler {
    JwtExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "storage-enabled", matchIfMissing = true)
class StorageExceptionHandlerAdvice extends StorageExceptionHandler {
    StorageExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
class ValidationExceptionHandlerAdvice extends ValidationExceptionHandler {
    ValidationExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}
