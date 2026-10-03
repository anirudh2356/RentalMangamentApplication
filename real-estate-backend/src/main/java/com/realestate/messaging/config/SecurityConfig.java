package com.realestate.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Allow browser CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // WebSocket
                        .requestMatchers("/ws/**").permitAll()

                        // Users
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users"
                        ).permitAll()

                        // Messaging - conversation list/create
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/conversations"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/conversations"
                        ).permitAll()

                        // Messaging - messages
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/conversations/*/messages"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/conversations/*/read"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/conversations/*/messages"
                        ).permitAll()

                        // Messaging - edit/delete messages
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/conversations/*/messages/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/conversations/*/messages/*"
                        ).permitAll()

                        // Reviews
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/properties/*/reviews"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/properties/*/reviews"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/properties/*/reviews/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/properties/*/reviews/*"
                        ).permitAll()

                        // Chatbot
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/chatbot"
                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}