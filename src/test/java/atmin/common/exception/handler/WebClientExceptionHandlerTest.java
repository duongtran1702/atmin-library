package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientExceptionHandlerTest {

    private final AtminExceptionProperties properties = new AtminExceptionProperties();
    private final WebClientExceptionHandler handler = new WebClientExceptionHandler(properties);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog");

    @Test
    void hidesDownstreamResponseBody() {
        WebClientResponseException exception = WebClientResponseException.create(
                500,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                "database password leaked".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleWebClientResponseException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamServiceError());
        assertThat(response.getBody().getMessage()).doesNotContain("password");
    }

    @Test
    void recognizesWebClientTimeout() {
        WebClientRequestException exception = new WebClientRequestException(
                new SocketTimeoutException("read timed out"),
                HttpMethod.GET,
                URI.create("http://inventory-service/items/1"),
                HttpHeaders.EMPTY);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleWebClientRequestException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamTimeout());
    }

    @Test
    void preservesSelectedClientStatusAndSafeRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "30");
        WebClientResponseException exception = WebClientResponseException.create(
                429, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleWebClientResponseException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(
                atmin.common.response.AtminErrorCode.RATE_LIMIT_EXCEEDED);
        assertThat(response.getBody().getRetryable()).isTrue();
    }
}
