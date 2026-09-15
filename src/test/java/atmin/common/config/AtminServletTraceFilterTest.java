package atmin.common.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AtminServletTraceFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesIncomingTraceAndRestoresPreviousMdc() throws ServletException, IOException {
        AtminExceptionProperties properties = new AtminExceptionProperties();
        AtminServletTraceFilter filter = new AtminServletTraceFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        request.addHeader(properties.getTraceIdHeader(), "trace-from-gateway");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(properties.getTraceIdMdcKey(), "parent-trace");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(AtminServletTraceFilter.TRACE_ID_ATTRIBUTE))
                .isEqualTo("trace-from-gateway");
        assertThat(response.getHeader(properties.getTraceIdHeader())).isEqualTo("trace-from-gateway");
        assertThat(MDC.get(properties.getTraceIdMdcKey())).isEqualTo("parent-trace");
    }

    @Test
    void rejectsUnsafeIncomingTraceAndDoesNotEchoWhenDisabled()
            throws ServletException, IOException {
        AtminExceptionProperties properties = new AtminExceptionProperties();
        properties.setEchoTraceIdHeader(false);
        AtminServletTraceFilter filter = new AtminServletTraceFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        request.addHeader(properties.getTraceIdHeader(), "trace with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String resolved = (String) request.getAttribute(AtminServletTraceFilter.TRACE_ID_ATTRIBUTE);
        assertThat(UUID.fromString(resolved)).isNotNull();
        assertThat(response.getHeader(properties.getTraceIdHeader())).isNull();
    }
}
