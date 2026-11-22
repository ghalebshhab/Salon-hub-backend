package com.salon.backend.Services.Auth.Security;


import com.salon.backend.Entities.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.time.Instant;

@Service

public class JwtService {

    private final String secretKey;

    public JwtService(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    /** Claim that links the access token to its server-side sign-in session. */
    public static final String SESSION_CLAIM = "sid";

    /** A short-lived access token tied to one sign-in session. */
    public String generateAccessToken(User user, String sessionId, java.time.Duration ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(Long.toString(user.getId()))
                .claim(SESSION_CLAIM, sessionId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttl.toMillis()))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSessionId(String token) {
        return extractAllClaims(token).get(SESSION_CLAIM, String.class);
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public java.util.Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public Instant extractIssuedAt(String token) {
        Date issuedAt = extractAllClaims(token).getIssuedAt();
        return issuedAt == null ? null : issuedAt.toInstant();
    }


    public boolean isTokenValid(String token, Long userId) {
        return extractUserId(token).equals(userId) && !isTokenExpired(token);
    }

    public boolean isTokenValid(
            String token,
            Long userId,
            Instant credentialsUpdatedAt
    ) {
        if (!isTokenValid(token, userId)) {
            return false;
        }
        if (credentialsUpdatedAt == null) {
            return true;
        }
        Instant issuedAt = extractIssuedAt(token);
        return issuedAt != null && !issuedAt.isBefore(credentialsUpdatedAt);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
