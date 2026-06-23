package atmin.common.config;

import atmin.common.annotation.EnableAtminExceptionHandling;
import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.exception.handler.SecurityExceptionHandler;
import atmin.common.exception.handler.StorageExceptionHandler;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
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
 * <p><strong>Design note:</strong> This registrar imports the inner wrapper classes
 * from {@link AtminExceptionAutoConfiguration} which are annotated with
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
        imports.add(AtminExceptionAutoConfiguration.CoreExceptionHandlerAdvice.class.getName());
        imports.add(AtminExceptionPropertiesConfiguration.class.getName());

        if (attributes != null) {
            // Security handlers (JWT + Spring Security)
            if (attributes.getBoolean("enableSecurityHandlers")) {
                if (isClassPresent("org.springframework.security.core.AuthenticationException")
                        && isClassPresent("io.jsonwebtoken.JwtException")
                        && isClassPresent("org.springframework.security.access.AccessDeniedException")) {
                    imports.add(AtminExceptionAutoConfiguration.SecurityHandlerConfiguration.SecurityExceptionHandlerAdvice.class.getName());
                }
                if (isClassPresent("org.springframework.security.web.AuthenticationEntryPoint")) {
                    imports.add(SecurityExceptionConfig.class.getName());
                }
            }

            // Storage handlers (file upload + cloud storage)
            if (attributes.getBoolean("enableStorageHandlers")) {
                imports.add(AtminExceptionAutoConfiguration.StorageHandlerConfiguration.StorageExceptionHandlerAdvice.class.getName());
            }
        }

        return imports.toArray(new String[0]);
    }

    /**
     * Check if a class is present on the classpath.
     * Used to safely skip handler registration when optional dependencies are missing.
     */
    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
