package atmin.common.config;

import atmin.common.reactive.AtminReactiveTraceFilter;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.AtminErrorCode;
import atmin.common.trace.TraceIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Produces the Atmin JSON contract for failures in the reactive security chain. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ServerAuthenticationEntryPoint.class, ServerAccessDeniedHandler.class})
@ConditionalOnBean(ObjectMapper.class)
@RequiredArgsConstructor
public class ReactiveSecurityExceptionConfig {

    private final ObjectMapper objectMapper;
    private final AtminExceptionProperties properties;

    @Bean
    @ConditionalOnMissingBean(ServerAuthenticationEntryPoint.class)
    public ServerAuthenticationEntryPoint serverAuthenticationEntryPoint() {
        return (exchange, exception) -> write(
                exchange, HttpStatus.UNAUTHORIZED, properties.getSecurityUnauthorized(),
                AtminErrorCode.UNAUTHORIZED);
    }

    @Bean
    @ConditionalOnMissingBean(ServerAccessDeniedHandler.class)
    public ServerAccessDeniedHandler serverAccessDeniedHandler() {
        return (exchange, exception) -> write(
                exchange, HttpStatus.FORBIDDEN, properties.getSecurityAccessDenied(),
                AtminErrorCode.FORBIDDEN);
    }

    private Mono<Void> write(
            ServerWebExchange exchange, HttpStatus status, String message, AtminErrorCode code) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        Object attribute = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        String candidate = attribute instanceof String string ? string
                : exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        String traceId = TraceIdResolver.resolve(candidate);
        ApiErrorResponse body = ApiErrorResponse.of(
                status, message, exchange.getRequest().getPath().value(), traceId,
                code, false, null);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            if (properties.isEchoTraceIdHeader()) {
                exchange.getResponse().getHeaders().set(properties.getTraceIdHeader(), traceId);
            }
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        catch (Exception serializationFailure) {
            return Mono.error(serializationFailure);
        }
    }
}
