package org.nevmock.digivise.infrastructure.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.nevmock.digivise.infrastructure.adapter.security.JwtTokenProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = getTokenFromHeader(request)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            val username = jwtTokenProvider.getUsernameFromJWT(token)
            val authentication = UsernamePasswordAuthenticationToken(username, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        }
        filterChain.doFilter(request, response)
    }

    private fun getTokenFromHeader(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (!bearer.isNullOrBlank() && bearer.startsWith("Bearer ")) {
            bearer.substring(7)
        } else {
            null
        }
    }
}