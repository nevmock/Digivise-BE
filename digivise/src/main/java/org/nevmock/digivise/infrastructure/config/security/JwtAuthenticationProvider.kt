package org.nevmock.digivise.infrastructure.config.security

import org.nevmock.digivise.domain.port.out.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationProvider(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val rawPassword = authentication.credentials.toString()

        val user = userRepository.findByUsername(username)
            .orElseThrow({ BadCredentialsException("User not found") })

        val a = passwordEncoder.matches(rawPassword, user.password)

        if (!a) {
            throw BadCredentialsException("Invalid credentials")
        }

        return UsernamePasswordAuthenticationToken(username, null, emptyList())
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}