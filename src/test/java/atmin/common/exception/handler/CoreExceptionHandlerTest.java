package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.ResourceNotFoundException;
import atmin.common.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class CoreExceptionHandlerTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items/1");
    private AtminExceptionProperties properties;
    private CoreExceptionHandler handler;

    @BeforeEach
    void setUp() {
        properties = new AtminExceptionProperties();
        handler = new CoreExceptionHandler(properties);
    }

    @Test
    void keepsCustomExceptionContract() {
        var entity = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Item", "id", 1), request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("Item not found with id: '1'");
        assertThat(entity.getBody().getPath()).isEqualTo("/api/items/1");
    }

    @Test
    void doesNotLeakUnexpectedRuntimeExceptionMessage() {
        properties.setUnexpectedError("Safe public message");

        var entity = handler.handleRuntimeException(
                new RuntimeException("database password leaked"), request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("Safe public message");
    }

    @Test
    void preservesResponseStatusExceptionStatus() {
        var entity = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Slow down"), request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("Slow down");
    }

    @Test
    void returnsSafeMessageForMalformedBody() {
        properties.setMalformedRequest("JSON is invalid");

        var entity = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException(
                        "parser details", new MockHttpInputMessage(new byte[0])),
                request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("JSON is invalid");
    }
}
