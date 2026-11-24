package com.cityguard.backend.security;

import com.cityguard.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Service xử lý sinh, giải mã JWT Access Token và băm Refresh Token bằng SHA-256 (Schema V3).
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Getter
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception ex) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        String subject = user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : user.getEmail();

        return Jwts.builder()
                .subject(subject)
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        } else if (userId instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
            return false;
        }
    }

    public String generateRawRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Băm raw token ngẫu nhiên bằng SHA-256 ra chuỗi 64 ký tự hex viết thường.
     * Khớp hoàn toàn với CHECK constraint: ^[0-9a-f]{64}$ của bảng refresh_tokens (Schema V3).
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 không khả dụng trong môi trường runtime", e);
        }
    }
}

