package atmin.common.config;

import atmin.common.annotation.EnableAtminExceptionHandling;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Import selector that reads the attributes of {@link EnableAtminExceptionHandling}
 * and decides which exception handler classes to register as Spring beans.
 *
 * <p>This class is not meant to be used directly. It is invoked automatically
 * when {@code @EnableAtminExceptionHandling} is placed on a configuration class.</p>
 *
 * <p><strong>Design note:</strong> This registrar imports package-private wrapper
 * classes which are annotated with
 * {@code @ControllerAdvice}. This ensures {@code @ExceptionHandler} methods are
 * recognized by Spring without putting {@code @Component} on the handler classes
 * themselves (which would cause unwanted component scanning).</p>
 *
 * @see EnableAtminExceptionHandling
 */
public class AtminExceptionHandlerRegistrar implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(
                        EnableAtminExceptionHandling.class.getName(), false)
        );

        List<String> imports = new ArrayList<>();

        // Core handlers are always imported — use the ControllerAdvice wrapper
        imports.add(CoreExceptionHandlerAdvice.class.getName());
        imports.add(MicroserviceExceptionHandlerAdvice.class.getName());
        if (isClassPresent("org.springframework.web.reactive.function.client.WebClientException")) {
            imports.add(WebClientExceptionHandlerAdvice.class.getName());
        }
        imports.add(AtminExceptionPropertiesConfiguration.class.getName());

        if (attributes != null) {
            // Security handlers (JWT + Spring Security)
            if (attributes.getBoolean("enableSecurityHandlers")) {
                boolean securityPresent = isClassPresent("org.springframework.security.core.AuthenticationException")
                        && isClassPresent("org.springframework.security.access.AccessDeniedException");
                boolean jwtPresent = isClassPresent("io.jsonwebtoken.JwtException");

                if (securityPresent && jwtPresent) {
                    imports.add(SecurityExceptionHandlerAdvice.class.getName());
                }
                else if (securityPresent) {
                    imports.add(SpringSecurityExceptionHandlerAdvice.class.getName());
                }
                else if (jwtPresent) {
                    imports.add(JwtExceptionHandlerAdvice.class.getName());
                }
                if (isClassPresent("org.springframework.security.web.AuthenticationEntryPoint")) {
                    imports.add(SecurityExceptionConfig.class.getName());
                }
            }

            // Storage handlers (file upload + cloud storage)
            if (attributes.getBoolean("enableStorageHandlers")) {
                imports.add(StorageExceptionHandlerAdvice.class.getName());
            }
        }

        if (isClassPresent("jakarta.validation.ConstraintViolationException")) {
            imports.add(ValidationExceptionHandlerAdvice.class.getName());
        }

        return imports.toArray(new String[0]);
    }

    /**
     * Check if a class is present on the classpath.
     * Used to safely skip handler registration when optional dependencies are missing.
     */
    private boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, getClass().getClassLoader());
    }
}
