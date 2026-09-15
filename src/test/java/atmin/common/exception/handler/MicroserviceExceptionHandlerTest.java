package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.CircuitBreakerOpenException;
import atmin.common.exception.DownstreamServiceException;
import atmin.common.exception.GatewayTimeoutException;
import atmin.common.exception.RateLimitExceededException;
import atmin.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class MicroserviceExceptionHandlerTest {

    private final AtminExceptionProperties properties = new AtminExceptionProperties();
    private final MicroserviceExceptionHandler handler = new MicroserviceExceptionHandler(properties);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");

    @Test
    void mapsDownstreamFailureToBadGateway() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDownstreamServiceException(
                new DownstreamServiceException("payment-service", 500), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamServiceError());
        assertThat(response.getBody().getService()).isNull();
    }

    @Test
    void mapsGatewayTimeoutTo504() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGatewayTimeoutException(
                GatewayTimeoutException.forService("inventory-service"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void includesRetryAfterForRateLimit() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRateLimitExceededException(
                new RateLimitExceededException("Too many requests", 30), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
    }

    @Test
    void mapsOpenCircuitTo503() {
        ResponseEntity<ApiErrorResponse> response = handler.handleCircuitBreakerOpenException(
                CircuitBreakerOpenException.forService("catalog-service"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void recognizesNestedTransportTimeout() {
        ResourceAccessException exception = new ResourceAccessException(
                "request failed", new SocketTimeoutException("read timed out"));

        ResponseEntity<ApiErrorResponse> response = handler.handleResourceAccessException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamTimeout());
    }

    @Test
    void mapsOtherTransportFailuresTo503() {
        ResourceAccessException exception = new ResourceAccessException(
                "request failed", new ConnectException("connection refused"));

        ResponseEntity<ApiErrorResponse> response = handler.handleResourceAccessException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamServiceUnavailable());
    }

    @Test
    void mapsDirectUnknownHostTo503() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDirectNetworkFailure(
                new UnknownHostException("inventory-service"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamServiceUnavailable());
        assertThat(response.getBody().getMessage()).doesNotContain("inventory-service");
    }

    @Test
    void mapsDirectSocketTimeoutTo504() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDirectNetworkTimeout(
                new SocketTimeoutException("internal address"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(properties.getDownstreamTimeout());
        assertThat(response.getBody().getMessage()).doesNotContain("internal address");
    }
}
