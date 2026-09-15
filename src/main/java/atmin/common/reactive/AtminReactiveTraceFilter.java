package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.trace.TraceIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Propagates a request trace ID without relying on thread-local MDC. */
@RequiredArgsConstructor
public class AtminReactiveTraceFilter implements WebFilter, Ordered {

    public static final String TRACE_ID_ATTRIBUTE = AtminReactiveTraceFilter.class.getName() + ".traceId";
    public static final String TRACE_ID_CONTEXT_KEY = "atmin.traceId";

    private final AtminExceptionProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        String traceId = TraceIdResolver.resolve(incoming);
        exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);

        if (properties.isEchoTraceIdHeader()) {
            exchange.getResponse().beforeCommit(() -> {
                exchange.getResponse().getHeaders().set(properties.getTraceIdHeader(), traceId);
                return Mono.empty();
            });
        }

        return chain.filter(exchange)
                .contextWrite(context -> context.put(TRACE_ID_CONTEXT_KEY, traceId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }
}
