package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import tools.jackson.databind.ObjectMapper;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class AtminGatewayErrorWebExceptionHandlerTest {

    private final AtminExceptionProperties properties = new AtminExceptionProperties();
    private final AtminGatewayErrorWebExceptionHandler handler =
            new AtminGatewayErrorWebExceptionHandler(new ObjectMapper(), properties);

    @Test
    void sanitizesGatewayDnsFailure() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new UnknownHostException("private-service.local")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains(properties.getDownstreamServiceUnavailable());
        assertThat(body).doesNotContain("private-service.local");
    }

    @Test
    void keepsRetryAfterForGatewayRateLimit() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new RateLimitExceededException("Too many requests", 20)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("20");
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());
    }
}
