package com.realestate.messaging.config;

import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                throw new UsernameNotFoundException("Invalid email or password");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole() == null ? "BUYER" : user.getRole().name())
                    .disabled(!user.isEnabled())
                    .build();
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/conversations/**",
                                "/api/properties/*/reviews",
                                "/api/properties/*/reviews/**",
                                "/api/chatbot/**"
                        )
                )

                .httpBasic(basic -> basic.disable())

                .formLogin(form -> form.disable())

                .securityContext(context ->
                        context.securityContextRepository(securityContextRepository)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        })
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
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

                        // Authentication
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/logout"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me"
                        ).authenticated()

                        // Broker profile reads and updates use the existing authenticated session.
                        .requestMatchers("/api/profile", "/api/profile/**")
                        .hasRole("BROKER")

                        // Broker settings are scoped to the authenticated session principal.
                        .requestMatchers("/api/settings", "/api/settings/**")
                        .hasRole("BROKER")

                        // Reports and exports are limited to the authenticated broker.
                        .requestMatchers("/api/reports", "/api/reports/**")
                        .hasRole("BROKER")

                        // Workspace search returns only authenticated broker-owned records.
                        .requestMatchers("/api/search", "/api/search/**")
                        .hasRole("BROKER")

                        // Property management is restricted to authenticated brokers.
                        .requestMatchers("/api/properties", "/api/properties/*")
                        .hasRole("BROKER")

                        // Lead management is restricted to authenticated brokers.
                        .requestMatchers("/api/leads", "/api/leads/*")
                        .hasRole("BROKER")

                        .requestMatchers("/api/property-matching/**")
                        .hasRole("BROKER")

                        // Site visits are restricted to authenticated brokers.
                        .requestMatchers("/api/site-visits", "/api/site-visits/*")
                        .hasRole("BROKER")

                        // Deal management is restricted to authenticated brokers.
                        .requestMatchers("/api/deals", "/api/deals/*")
                        .hasRole("BROKER")

                        // Commission details are derived from a broker's own won deals.
                        .requestMatchers("/api/commissions")
                        .hasRole("BROKER")

                        // Analytics only aggregates the authenticated broker's records.
                        .requestMatchers("/api/analytics/**")
                        .hasRole("BROKER")

                        // Follow-ups are private to the authenticated broker.
                        .requestMatchers("/api/followups", "/api/followups/*")
                        .hasRole("BROKER")

                        // Calendar data is aggregated from broker-owned appointments and follow-ups.
                        .requestMatchers("/api/calendar/**")
                        .hasRole("BROKER")

                        // Team members are visible only to their owning broker.
                        .requestMatchers("/api/team/members", "/api/team/members/*")
                        .hasRole("BROKER")

                        // Documents are private to the authenticated broker.
                        .requestMatchers("/api/documents", "/api/documents/**")
                        .hasRole("BROKER")

                        // Broker notifications are scoped to the authenticated broker.
                        .requestMatchers("/api/notifications", "/api/notifications/**")
                        .hasRole("BROKER")

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