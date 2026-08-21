package com.ntn.fziot.mailtrace.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${mailtrace.security.permit-all:false}")
    private boolean permitAll;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (permitAll) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                                    AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                                    AntPathRequestMatcher.antMatcher("/swagger-ui.html"),
                                    AntPathRequestMatcher.antMatcher("/v1/auth/login"),
                                    AntPathRequestMatcher.antMatcher("/v1/customer-tickets/**"),
                                    AntPathRequestMatcher.antMatcher("/v1/system/health")
                            ).permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        objectMapper.writeValue(response.getWriter(), BasicResult.fail(40102, "登录状态已失效，请重新登录"));
                    }))
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
