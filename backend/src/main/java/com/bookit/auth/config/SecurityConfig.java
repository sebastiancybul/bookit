package com.bookit.auth.config;

import com.bookit.auth.security.ProblemDetailSecurityHandler;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.web.util.WebUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    private static final Set<String> PATHS_WITHOUT_TOKEN = Set.of(
            "/api/v1/csrf",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/logout");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandler securityHandler) {
        http
                .csrf(csrf -> csrf
                        .spa()
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityHandler)
                        .accessDeniedHandler(securityHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(securityHandler)
                        .accessDeniedHandler(securityHandler)
                        .bearerTokenResolver(cookieTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromJwt())));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private BearerTokenResolver cookieTokenResolver() {
        return request -> {
            if (PATHS_WITHOUT_TOKEN.contains(request.getRequestURI())) {
                return null;
            }
            Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN_COOKIE);
            return cookie != null ? cookie.getValue() : null;
        };
    }

    private JwtAuthenticationConverter rolesFromJwt() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
