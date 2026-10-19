package com.mrdev.config;

import com.mrdev.common.ratelimit.RateLimitingFilter;
import com.mrdev.common.security.SecurityHeadersFilter;
import com.mrdev.modules.auth.security.JwtAuthenticationFilter;
import com.mrdev.modules.auth.security.OAuth2AuthenticationFailureHandler;
import com.mrdev.modules.auth.security.OAuth2AuthenticationSuccessHandler;
import com.mrdev.modules.auth.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final com.mrdev.common.observability.CorrelationIdFilter correlationIdFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final com.mrdev.common.security.OriginValidationFilter originValidationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,https://courses.mrsgemaseny.com,https://mr-dev-courses.vercel.app}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error", "/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/courses", "/v1/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/projects", "/v1/projects/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/certificates/verify/**", "/v1/certificates/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invites/**").permitAll()
                        .requestMatchers("/v1/telegram/status", "/v1/telegram/webhook").permitAll()
                        .requestMatchers("/v1/auth/me").authenticated()
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                 .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(originValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new java.util.ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());

        if (!origins.contains("https://courses.mrsgemaseny.com")) {
            origins.add("https://courses.mrsgemaseny.com");
        }
        if (!origins.contains("https://mr-dev-courses.vercel.app")) {
            origins.add("https://mr-dev-courses.vercel.app");
        }
        origins.add("https://*.vercel.app");
        origins.add("https://*.mrsgemaseny.com");
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");

        configuration.setAllowedOriginPatterns(origins.stream().distinct().toList());

        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedMethods(methods);

        if ("*".equals(allowedHeaders.trim())) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList());
        }

        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public com.mrdev.modules.auth.security.HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new com.mrdev.modules.auth.security.HttpCookieOAuth2AuthorizationRequestRepository();
    }
}

