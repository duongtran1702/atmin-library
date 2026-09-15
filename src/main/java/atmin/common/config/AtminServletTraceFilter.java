package atmin.common.config;

import atmin.common.trace.TraceIdResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Propagates trace IDs for Servlet/MVC requests while safely restoring MDC. */
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AtminServletTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = AtminServletTraceFilter.class.getName() + ".traceId";

    private final AtminExceptionProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String mdcKey = properties.getTraceIdMdcKey();
        String previous = MDC.get(mdcKey);
        String incoming = request.getHeader(properties.getTraceIdHeader());
        String traceId = TraceIdResolver.resolve(incoming);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        MDC.put(mdcKey, traceId);
        if (properties.isEchoTraceIdHeader()) {
            response.setHeader(properties.getTraceIdHeader(), traceId);
        }
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            if (previous == null) {
                MDC.remove(mdcKey);
            }
            else {
                MDC.put(mdcKey, previous);
            }
        }
    }
}
