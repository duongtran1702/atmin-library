package atmin.common.config;

import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.exception.handler.JwtExceptionHandler;
import atmin.common.exception.handler.SecurityExceptionHandler;
import atmin.common.exception.handler.SpringSecurityExceptionHandler;
import atmin.common.exception.handler.StorageExceptionHandler;
import atmin.common.exception.handler.ValidationExceptionHandler;
import atmin.common.annotation.EnableAtminExceptionHandling;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AtminExceptionAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AtminExceptionAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void registersExistingHandlersAndNewOptionalHandlers() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AtminExceptionProperties.class);
            assertThat(context).hasSingleBean(CoreExceptionHandler.class);
            assertThat(context).hasSingleBean(SecurityExceptionHandler.class);
            assertThat(context).hasSingleBean(StorageExceptionHandler.class);
            assertThat(context).hasSingleBean(ValidationExceptionHandler.class);
            assertThat(context.findAnnotationOnBean(
                    "coreExceptionHandler", ControllerAdvice.class)).isNotNull();
            assertThat(context).hasBean("authenticationEntryPoint");
            assertThat(context).hasBean("accessDeniedHandler");
        });
    }

    @Test
    void supportsSpringSecurityWithoutJjwt() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.jsonwebtoken"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SecurityExceptionHandler.class);
                    assertThat(context).hasSingleBean(SpringSecurityExceptionHandler.class);
                });
    }

    @Test
    void supportsJjwtWithoutSpringSecurity() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.security"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SecurityExceptionHandler.class);
                    assertThat(context).hasSingleBean(JwtExceptionHandler.class);
                });
    }

    @Test
    void canDisableAllAutoConfiguration() {
        contextRunner
                .withPropertyValues("atmin.exceptions.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CoreExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(StorageExceptionHandler.class);
                });
    }

    @Test
    void canDisableOptionalModulesIndependently() {
        contextRunner
                .withPropertyValues(
                        "atmin.exceptions.security-enabled=false",
                        "atmin.exceptions.security-filter-enabled=false",
                        "atmin.exceptions.storage-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(CoreExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(SecurityExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(StorageExceptionHandler.class);
                    assertThat(context).doesNotHaveBean("authenticationEntryPoint");
                    assertThat(context).doesNotHaveBean("accessDeniedHandler");
                });
    }

    @Test
    void backsOffWhenApplicationProvidesCoreHandler() {
        CoreExceptionHandler custom = new CoreExceptionHandler(new AtminExceptionProperties());

        contextRunner
                .withBean("customCoreExceptionHandler", CoreExceptionHandler.class, () -> custom)
                .run(context -> assertThat(context.getBeansOfType(CoreExceptionHandler.class))
                        .containsOnlyKeys("customCoreExceptionHandler"));
    }

    @Test
    void keepsLegacyEnableAnnotationWorking() {
        new WebApplicationContextRunner()
                .withUserConfiguration(ManualConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CoreExceptionHandler.class);
                    assertThat(context).hasSingleBean(SecurityExceptionHandler.class);
                    assertThat(context).hasSingleBean(StorageExceptionHandler.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAtminExceptionHandling(
            enableSecurityHandlers = true,
            enableStorageHandlers = true)
    static class ManualConfiguration {
    }
}
