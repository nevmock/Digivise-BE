package org.nevmock.digivise.application.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JWTUtils {
    private static String SECRET_KEY = "";
    private static long EXPIRATION_TIME = 0;

    private static Key key;

    public JWTUtils() {
        SECRET_KEY = System.getenv("JWT_SECRET");
        EXPIRATION_TIME = Long.parseLong(System.getenv("JWT_EXPIRATION"));
        key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Invalid token: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("Invalid signature: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
        }
        return false;
    }
}