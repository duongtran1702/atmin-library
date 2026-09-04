package atmin.common.response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorResponseTest {

    @AfterEach
    void resetMdc() {
        MDC.clear();
        ApiErrorResponse.setMdcKey("traceId");
    }

    @Test
    void resolvesTraceIdFromConfiguredMdcKey() {
        ApiErrorResponse.setMdcKey("correlationId");
        MDC.put("correlationId", "trace-123");

        ApiErrorResponse response = ApiErrorResponse.notFound("Missing", "/items/1");

        assertThat(response.getTraceId()).isEqualTo("trace-123");
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void generatesValidUuidWhenMdcIsEmpty() {
        String traceId = ApiErrorResponse.resolveTraceId();

        assertThat(UUID.fromString(traceId)).isNotNull();
    }

    @Test
    void supportsConfiguredValidationMessageWithoutBreakingOldFactory() {
        ApiErrorResponse configured = ApiErrorResponse.validationError(
                "/items", "Invalid request", Map.of("name", "required"));
        ApiErrorResponse legacy = ApiErrorResponse.validationError(
                "/items", Map.of("name", "required"));

        assertThat(configured.getMessage()).isEqualTo("Invalid request");
        assertThat(configured.getErrors()).containsEntry("name", "required");
        assertThat(legacy.getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void supportsSpringHttpStatusCodeFactory() {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNPROCESSABLE_CONTENT, "Invalid state", "/items");

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(response.getError()).isEqualTo("Unprocessable Content");
    }
}
