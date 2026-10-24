package com.patientcard.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.patientcard.security.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { cors ->
                val origins = appProperties.cors.origins()
                if (origins.isEmpty()) cors.disable() else cors.configurationSource(corsConfigurationSource(origins))
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/app/auth/register",
                        "/api/v1/app/auth/login",
                        "/api/v1/app/auth/refresh",
                        "/api/v1/app/auth/logout",
                    ).permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/info",
                    ).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { handling ->
                handling.authenticationEntryPoint { _, response, _ ->
                    writeSecurityError(response, 401, "UNAUTHORIZED", "Wymagane uwierzytelnienie.")
                }
                handling.accessDeniedHandler { _, response, _ ->
                    writeSecurityError(response, 403, "FORBIDDEN", "Brak uprawnień do tego zasobu.")
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun writeSecurityError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            mapOf(
                "type" to "about:blank",
                "title" to if (status == 401) "Unauthorized" else "Forbidden",
                "status" to status,
                "code" to code,
                "detail" to message,
            ),
        )
    }

    private fun corsConfigurationSource(origins: List<String>): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = origins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
        configuration.exposedHeaders = listOf("Content-Disposition")
        configuration.allowCredentials = false
        configuration.maxAge = 3600
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }
}
