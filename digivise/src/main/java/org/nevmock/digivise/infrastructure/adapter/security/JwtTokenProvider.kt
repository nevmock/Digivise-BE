package org.nevmock.digivise.infrastructure.adapter.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expirationInMs: Long = 0

    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationInMs)

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getUsernameFromJWT(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
    }

    fun getExpirationDateFromJWT(token: String): Date {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .expiration
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            true
        } catch (ex: ExpiredJwtException) {
            println("Token expired: ${ex.message}")
            false
        } catch (ex: UnsupportedJwtException) {
            println("Unsupported token: ${ex.message}")
            false
        } catch (ex: MalformedJwtException) {
            println("Malformed token: ${ex.message}")
            false
        } catch (ex: SignatureException) {
            println("Invalid signature: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            println("Token is null or empty: ${ex.message}")
            false
        }
    }
}