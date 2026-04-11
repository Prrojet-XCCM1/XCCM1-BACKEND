package com.ihm.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ihm.backend.repository.jpa.UserRepository;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Active @PreAuthorize
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;

        @Value("${lti.security.frame-ancestors:self}")
        private String ltiFrameAncestorsTokens;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        CustomAccessDeniedHandler customAccessDeniedHandler) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
                this.customAccessDeniedHandler = customAccessDeniedHandler;
        }

        private String frameAncestorsCspDirective() {
                String raw = ltiFrameAncestorsTokens == null ? "self" : ltiFrameAncestorsTokens.trim();
                if (raw.isEmpty()) {
                        raw = "self";
                }
                String[] tokens = raw.split("\\s+");
                StringBuilder sb = new StringBuilder("frame-ancestors ");
                for (String t : tokens) {
                        if ("self".equalsIgnoreCase(t)) {
                                sb.append("'self' ");
                        } else {
                                sb.append(t).append(" ");
                        }
                }
                return sb.toString().trim();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // DÉSACTIVER CSRF POUR LES APIs REST
                                .csrf(csrf -> csrf.disable()) // ← Important pour les APIs REST
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.disable())
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(frameAncestorsCspDirective())))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // === SWAGGER/OPENAPI - ACCÈS PUBLIC ===
                                                .requestMatchers(
                                                                "/",
                                                                "/ws/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**",
                                                                "/webjars/**",
                                                                "/api-docs/**",
                                                                "/api-docs.yaml",
                                                                "/favicon.ico",
                                                                "/error")
                                                .permitAll()

                                                // === API AUTHENTIFICATION - ACCÈS PUBLIC ===
                                                .requestMatchers(
                                                                "/api/v1/auth/**",
                                                                "/api/auth/**",
                                                                "/api/v1/public/**",
                                                                "/api/public/**",
                                                                "/api/register",
                                                                "/api/login",
                                                                "/api/health",
                                                                "/actuator/health",
                                                                "/courses",
                                                                "/courses/**",
                                                                "/api/v1/images/**")
                                                .permitAll()

                                                // === LTI 1.3 (Moodle → XCCM1) — public ===
                                                .requestMatchers("/lti/**").permitAll()

                                                // === ADMIN - ACCÈS RESTEINT ===
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                                // === TOUTES LES AUTRES ROUTES NÉCESSITENT AUTHENTIFICATION ===
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(customAccessDeniedHandler))
                                .addFilterBefore(jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                
                // On privilégie les variables d'environnement, sinon on utilise les valeurs par défaut
                String allowedOrigins = System.getProperty("CORS_ALLOWED_ORIGINS");
                if (allowedOrigins == null) {
                        allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
                }

                if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                        configuration.setAllowedOrigins(
                                        List.of("http://localhost:3000", "http://192.168.1.177:3000", "http://192.168.1.135:3000", "https://frontend-xccm-12027.vercel.app"));
                } else {
                        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .toList();
                        configuration.setAllowedOrigins(origins);
                }
                
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Cache-Control",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService userDetailsService(
                        UserRepository repository) {
                return username -> repository.findByEmail(username)
                                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                                                "Utilisateur non trouvé"));
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }
}
