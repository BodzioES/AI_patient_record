package com.patientcard.security

import com.patientcard.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val email: String,
)

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val claims = jwtService.parseAccessToken(header.removePrefix("Bearer ").trim())
            if (claims != null && SecurityContextHolder.getContext().authentication == null) {
                val user = userRepository.findById(claims.userId).orElse(null)
                if (user != null && user.enabled) {
                    val principal = UserPrincipal(user.id!!, user.email)
                    val authentication = UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        emptyList(),
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
