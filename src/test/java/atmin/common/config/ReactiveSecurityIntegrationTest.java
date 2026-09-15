package atmin.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

class ReactiveSecurityIntegrationTest {

    @Test
    void securityFilterChainReturnsAtminJsonFor401And403() {
        try (var context = new org.springframework.boot.web.context.reactive.AnnotationConfigReactiveWebApplicationContext()) {
            context.registerBean(
                    ObjectMapper.class,
                    (Supplier<ObjectMapper>) ObjectMapper::new);
            context.register(TestApplication.class);
            context.refresh();
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .apply(springSecurity())
                    .configureClient()
                    .build();

            client.get().uri("/protected")
                    .header("X-Trace-Id", "security-trace")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectHeader().valueEquals("X-Trace-Id", "security-trace")
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(401)
                    .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                    .jsonPath("$.traceId").isEqualTo("security-trace");

            client.mutateWith(mockUser().roles("USER"))
                    .get().uri("/admin")
                    .header("X-Trace-Id", "forbidden-trace")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(403)
                    .jsonPath("$.code").isEqualTo("FORBIDDEN")
                    .jsonPath("$.traceId").isEqualTo("forbidden-trace");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    @EnableWebFluxSecurity
    @Import(AtminReactiveExceptionAutoConfiguration.class)
    static class TestApplication {

        @Bean
        RouterFunction<ServerResponse> routes() {
            return route(GET("/protected"), request -> ServerResponse.ok().bodyValue("ok"))
                    .andRoute(GET("/admin"), request -> ServerResponse.ok().bodyValue("ok"));
        }

        @Bean
        SecurityWebFilterChain securityWebFilterChain(
                ServerHttpSecurity http,
                ServerAuthenticationEntryPoint entryPoint,
                ServerAccessDeniedHandler deniedHandler) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .authorizeExchange(exchange -> exchange
                            .pathMatchers("/admin").hasRole("ADMIN")
                            .anyExchange().authenticated())
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint(entryPoint)
                            .accessDeniedHandler(deniedHandler))
                    .build();
        }
    }
}
