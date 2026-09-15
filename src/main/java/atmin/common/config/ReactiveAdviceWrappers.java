package atmin.common.config;

import atmin.common.reactive.ReactiveCoreExceptionHandler;
import atmin.common.reactive.ReactiveJwtExceptionHandler;
import atmin.common.reactive.ReactiveValidationExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "enabled", matchIfMissing = true)
class ReactiveCoreExceptionHandlerAdvice extends ReactiveCoreExceptionHandler {
    ReactiveCoreExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
class ReactiveValidationExceptionHandlerAdvice extends ReactiveValidationExceptionHandler {
    ReactiveValidationExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnClass(name = "io.jsonwebtoken.JwtException")
@ConditionalOnProperty(prefix = "atmin.exceptions", name = "security-enabled", matchIfMissing = true)
class ReactiveJwtExceptionHandlerAdvice extends ReactiveJwtExceptionHandler {
    ReactiveJwtExceptionHandlerAdvice(AtminExceptionProperties properties) {
        super(properties);
    }
}
