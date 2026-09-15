package atmin.common.response;

/** Stable machine-readable codes for optional enriched error responses. */
public enum AtminErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    PAYLOAD_TOO_LARGE,
    UNAUTHORIZED,
    FORBIDDEN,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    RATE_LIMIT_EXCEEDED,
    DOWNSTREAM_BAD_RESPONSE,
    DOWNSTREAM_UNAVAILABLE,
    DOWNSTREAM_TIMEOUT,
    CIRCUIT_BREAKER_OPEN,
    INTERNAL_ERROR
}
