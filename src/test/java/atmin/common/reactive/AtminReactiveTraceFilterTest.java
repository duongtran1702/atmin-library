package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AtminReactiveTraceFilterTest {

    @Test
    void propagatesSafeTraceIdThroughExchangeAndReactorContext() {
        AtminExceptionProperties properties = new AtminExceptionProperties();
        AtminReactiveTraceFilter filter = new AtminReactiveTraceFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/items")
                        .header("X-Trace-Id", "request-trace-42")
                        .build());
        AtomicReference<String> contextTraceId = new AtomicReference<>();

        filter.filter(exchange, current -> Mono.deferContextual(context -> {
            contextTraceId.set(context.get(AtminReactiveTraceFilter.TRACE_ID_CONTEXT_KEY));
            return current.getResponse().setComplete();
        })).block();

        String exchangeTraceId = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        assertThat(exchangeTraceId).isEqualTo("request-trace-42");
        assertThat(contextTraceId.get()).isEqualTo("request-trace-42");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"))
                .isEqualTo("request-trace-42");
    }

    @Test
    void rejectsUnsafeAndOversizedTraceHeaders() {
        for (String unsafe : new String[] {"trace with spaces", "x".repeat(129)}) {
            AtminExceptionProperties properties = new AtminExceptionProperties();
            AtminReactiveTraceFilter filter = new AtminReactiveTraceFilter(properties);
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/items")
                            .header(properties.getTraceIdHeader(), unsafe)
                            .build());

            filter.filter(exchange, current -> current.getResponse().setComplete()).block();

            String resolved = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
            assertThat(UUID.fromString(resolved)).isNotNull();
            assertThat(resolved).isNotEqualTo(unsafe);
        }
    }

    @Test
    void supportsCustomHeaderAndCanDisableEcho() {
        AtminExceptionProperties properties = new AtminExceptionProperties();
        properties.setTraceIdHeader("X-Correlation-Id");
        properties.setEchoTraceIdHeader(false);
        AtminReactiveTraceFilter filter = new AtminReactiveTraceFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/items")
                        .header("X-Correlation-Id", "custom-trace")
                        .build());

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        String traceId = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        assertThat(traceId).isEqualTo("custom-trace");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isNull();
    }
}
