package atmin.common.annotation;

import atmin.common.config.AtminExceptionHandlerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Atmin exception handling in your Spring Boot application.
 *
 * <p>Place this annotation on your main {@code @SpringBootApplication} class
 * or any {@code @Configuration} class to activate the exception handlers.</p>
 *
 * <p><strong>Basic usage (core handlers only):</strong></p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAtminExceptionHandling
 * public class MyApplication { }
 * </pre>
 *
 * <p><strong>With security handlers:</strong></p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAtminExceptionHandling(enableSecurityHandlers = true)
 * public class MySecureApplication { }
 * </pre>
 *
 * <p><strong>With all handlers:</strong></p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAtminExceptionHandling(enableSecurityHandlers = true, enableStorageHandlers = true)
 * public class MyFullApplication { }
 * </pre>
 *
 * <p><strong>Note:</strong> If you prefer auto-configuration without annotations,
 * the core handlers will be registered automatically via Spring Boot auto-configuration
 * when this library is on the classpath. Use this annotation only when you need
 * fine-grained control over which handler modules are active.</p>
 *
 * @see AtminExceptionHandlerRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AtminExceptionHandlerRegistrar.class)
public @interface EnableAtminExceptionHandling {

    /**
     * Whether to enable security-related exception handlers
     * (JWT, AuthenticationException, AccessDeniedException).
     * <p>Requires Spring Security and jjwt on the classpath.</p>
     *
     * @return true to enable security handlers, false otherwise
     */
    boolean enableSecurityHandlers() default false;

    /**
     * Whether to enable storage-related exception handlers
     * (MaxUploadSizeExceededException, CloudStorageException).
     *
     * @return true to enable storage handlers, false otherwise
     */
    boolean enableStorageHandlers() default false;
}
