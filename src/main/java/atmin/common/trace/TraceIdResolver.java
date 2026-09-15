package atmin.common.trace;

import atmin.common.response.ApiErrorResponse;

import java.util.UUID;
import java.util.regex.Pattern;

/** Resolves safe trace identifiers shared by MVC and reactive adapters. */
public final class TraceIdResolver {

    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private TraceIdResolver() {
    }

    /**
     * Prefer a valid request/context value, then MDC, then a generated UUID.
     */
    public static String resolve(String candidate) {
        if (isSafe(candidate)) {
            return candidate;
        }
        String mdcTraceId = ApiErrorResponse.resolveTraceId();
        if (isSafe(mdcTraceId)) {
            return mdcTraceId;
        }
        return UUID.randomUUID().toString();
    }

    /** Return whether a value is safe to expose in an HTTP response or log context. */
    public static boolean isSafe(String value) {
        return value != null && SAFE_TRACE_ID.matcher(value).matches();
    }
}
