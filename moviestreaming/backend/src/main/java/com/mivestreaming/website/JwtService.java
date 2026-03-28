package com.mivestreaming.website;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Tạo SecretKey từ chuỗi secret trong config */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    /**
     * Tạo JWT token cho user sau khi đăng nhập thành công.
     *
     * @param user User đã xác thực
     * @return JWT token dạng String
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                // Subject là email (định danh duy nhất)
                .subject(user.getEmail())
                // Custom claims: lưu thêm id và role
                .claim("userId", user.getId())
                .claim("role",   user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }


    /**
     * Xác thực token có hợp lệ không (chữ ký đúng & chưa hết hạn).
     *
     * @param token JWT token
     * @return true nếu hợp lệ
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token giả mạo, hết hạn hoặc malformed → từ chối
            return false;
        }
    }

    /** Lấy tất cả claims từ token */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Lấy email (subject) từ token */
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Lấy userId từ token */
    public String getUserIdFromToken(String token) {
        Object id = extractAllClaims(token).get("userId");
        if (id != null) return id.toString();
        return null;
    }

    /** Lấy role từ token */
    public String getRoleFromToken(String token) {
        return (String) extractAllClaims(token).get("role");
    }
}
