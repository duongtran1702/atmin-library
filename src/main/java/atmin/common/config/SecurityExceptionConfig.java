package atmin.common.config;

import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * Configuration for Spring Security filter-level exception entry point and access denied handlers.
 * It writes JSON responses in the format of ApiErrorResponse.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({AuthenticationEntryPoint.class, AccessDeniedHandler.class})
@ConditionalOnBean(ObjectMapper.class)
@RequiredArgsConstructor
public class SecurityExceptionConfig {

    private final ObjectMapper objectMapper;
    private final AtminExceptionProperties properties;

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                    .message(properties.getSecurityUnauthorized())
                    .path(request.getRequestURI())
                    .traceId(ApiErrorResponse.resolveTraceId())
                    .build();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.FORBIDDEN.value())
                    .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                    .message(properties.getSecurityAccessDenied())
                    .path(request.getRequestURI())
                    .traceId(ApiErrorResponse.resolveTraceId())
                    .build();

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
}
