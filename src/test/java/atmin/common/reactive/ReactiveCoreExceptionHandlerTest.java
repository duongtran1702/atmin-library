package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.RateLimitExceededException;
import atmin.common.exception.ResourceNotFoundException;
import atmin.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveCoreExceptionHandlerTest {

    private final AtminExceptionProperties properties = new AtminExceptionProperties();
    private final ReactiveCoreExceptionHandler handler = new ReactiveCoreExceptionHandler(properties);

    @Test
    void keepsCustomNotFoundContractAndRequestTraceId() {
        MockServerWebExchange exchange = exchange("trace-reactive-123");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Order", "id", 42), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isEqualTo("trace-reactive-123");
    }

    @Test
    void returnsSafeMessageForReactiveInputError() {
        MockServerWebExchange exchange = exchange(null);

        ResponseEntity<ApiErrorResponse> response = handler.handleWebInput(
                new ServerWebInputException("decoder internals"), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getMalformedRequest());
    }

    @Test
    void doesNotLeakUnexpectedReactiveException() {
        MockServerWebExchange exchange = exchange(null);

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("database password leaked"), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getUnexpectedError());
    }

    @Test
    void addsRetryAfterInReactiveRateLimitResponse() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRateLimit(
                new RateLimitExceededException("Too many requests", 45), exchange(null)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("45");
    }

    @Test
    void preservesSafeDownstreamRateLimitForReactiveClients() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "15");
        WebClientResponseException exception = WebClientResponseException.create(
                429, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleWebClientResponse(exception, exchange(null)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("15");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRetryable()).isTrue();
    }

    private MockServerWebExchange exchange(String traceId) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/orders/42");
        if (traceId != null) {
            request.header(properties.getTraceIdHeader(), traceId);
        }
        return MockServerWebExchange.from(request.build());
    }
}
