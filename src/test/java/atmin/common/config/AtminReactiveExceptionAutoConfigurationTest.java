package atmin.common.config;

import atmin.common.exception.handler.CoreExceptionHandler;
import atmin.common.reactive.AtminReactiveTraceFilter;
import atmin.common.reactive.ReactiveCoreExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AtminReactiveExceptionAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            AtminExceptionAutoConfiguration.class,
                            AtminReactiveExceptionAutoConfiguration.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void selectsReactiveAdapterWithoutServletHandlers() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ReactiveCoreExceptionHandler.class);
            assertThat(context).hasSingleBean(AtminReactiveTraceFilter.class);
            assertThat(context).doesNotHaveBean(CoreExceptionHandler.class);
        });
    }

    @Test
    void reactiveAdapterCanBeDisabled() {
        contextRunner
                .withPropertyValues("atmin.exceptions.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ReactiveCoreExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(AtminReactiveTraceFilter.class);
                });
    }

    @Test
    void reactiveSecurityHandlersBackOffForApplicationBeans() {
        ServerAuthenticationEntryPoint entryPoint = (exchange, error) -> Mono.empty();
        ServerAccessDeniedHandler deniedHandler = (exchange, error) -> Mono.empty();

        contextRunner
                .withBean(ServerAuthenticationEntryPoint.class, () -> entryPoint)
                .withBean(ServerAccessDeniedHandler.class, () -> deniedHandler)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServerAuthenticationEntryPoint.class);
                    assertThat(context).hasSingleBean(ServerAccessDeniedHandler.class);
                    assertThat(context.getBean(ServerAuthenticationEntryPoint.class)).isSameAs(entryPoint);
                    assertThat(context.getBean(ServerAccessDeniedHandler.class)).isSameAs(deniedHandler);
                });
    }
}
