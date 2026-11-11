package com.englishcenter.admission.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Thành phần chịu trách nhiệm tạo, ký số và xác thực các JSON Web Token (JWT).
 * Hỗ trợ mô hình Dual Token:
 * - Access Token: Thời hạn ngắn (15 phút), dùng để xác thực request liên tục.
 * - Refresh Token: Thời hạn dài (7 ngày), dùng để làm mới Access Token và quản lý phiên.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:super_secret_jwt_key_english_center_2026_change_in_production}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    /**
     * Tạo khóa ký bí mật HMAC-SHA từ chuỗi bí mật, bảo đảm kích thước tối thiểu 256 bits (32 bytes).
     *
     * @return Đối tượng Key dùng để ký và giải mã JWT
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Cấp phát Access Token mới cho người dùng đã xác thực thành công.
     *
     * @param userId   ID người dùng
     * @param email    Email người dùng
     * @param role     Vai trò hệ thống (ADMIN, STAFF, TEACHER, STUDENT)
     * @param fullName Họ và tên đầy đủ
     * @return Chuỗi JWT Access Token
     */
    public String generateAccessToken(Integer userId, String email, String role, String fullName) {
        long now = System.currentTimeMillis();
        long exp = now + (accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("user_id", userId)
                .claim("email", email)
                .claim("role", role)
                .claim("full_name", fullName)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Cấp phát Refresh Token mới với ID duy nhất (jti).
     *
     * @param userId ID người dùng
     * @param jti    Mã định danh duy nhất của token (UUID)
     * @return Chuỗi JWT Refresh Token
     */
    public String generateRefreshToken(Integer userId, String jti) {
        long now = System.currentTimeMillis();
        long exp = now + (refreshTokenExpirationDays * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti != null ? jti : UUID.randomUUID().toString())
                .claim("token_type", "REFRESH")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Kiểm tra tính hợp lệ của token (chữ ký hợp lệ, chưa hết hạn, đúng định dạng).
     *
     * @param token Chuỗi JWT cần kiểm tra
     * @return true nếu token hoàn toàn hợp lệ, false nếu không hợp lệ hoặc đã hết hạn
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Chữ ký JWT không hợp lệ");
        } catch (MalformedJwtException ex) {
            log.warn("Cấu trúc JWT không đúng định dạng");
        } catch (ExpiredJwtException ex) {
            log.warn("JWT đã hết hạn sử dụng");
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT không được hỗ trợ");
        } catch (IllegalArgumentException ex) {
            log.warn("Chuỗi JWT rỗng hoặc không có giá trị");
        }
        return false;
    }

    /**
     * Trích xuất toàn bộ Claims từ token đã xác thực.
     *
     * @param token Chuỗi JWT
     * @return Đối tượng Claims chứa dữ liệu payload
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Trích xuất ID người dùng từ claim sub của token.
     *
     * @param token Chuỗi JWT
     * @return ID người dùng dạng Integer
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String sub = claims.getSubject();
        if (sub != null) {
            return Integer.parseInt(sub);
        }
        Object userIdObj = claims.get("user_id");
        if (userIdObj instanceof Number num) {
            return num.intValue();
        }
        return null;
    }

    /**
     * Trích xuất email từ claims của token.
     *
     * @param token Chuỗi JWT
     * @return Địa chỉ email người dùng
     */
    public String getEmailFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Trích xuất vai trò (role) từ claims của token.
     *
     * @param token Chuỗi JWT
     * @return Tên vai trò (ADMIN, STAFF, TEACHER, STUDENT)
     */
    public String getRoleFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Trích xuất họ tên từ claims của token.
     *
     * @param token Chuỗi JWT
     * @return Họ và tên đầy đủ
     */
    public String getFullNameFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("full_name", String.class);
    }

    /**
     * Trích xuất JWT ID (jti) của refresh token.
     *
     * @param token Chuỗi JWT
     * @return Chuỗi UUID của jti
     */
    public String getJtiFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getId();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }
}
