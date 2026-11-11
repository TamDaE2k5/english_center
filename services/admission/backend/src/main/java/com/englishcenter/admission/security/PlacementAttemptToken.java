package com.englishcenter.admission.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.Date;

/** Token chỉ cấp quyền nộp một lượt thi, dùng khóa riêng để không thể dùng như access JWT. */
@Component
public class PlacementAttemptToken {
    private final Key key;

    /** Dẫn xuất khóa có domain riêng từ secret môi trường, không lưu token vào DB/log. */
    public PlacementAttemptToken(@Value("${jwt.secret}") String secret) {
        try {
            key = Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256")
                    .digest(("placement-attempt:" + secret).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    /** Cấp token gắn ID lượt thi và hết hạn cùng deadline server. */
    public String issue(Integer attemptId, LocalDateTime expiresAt) {
        return Jwts.builder().setSubject(attemptId.toString()).setAudience("placement-submit")
                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /** Kiểm tra chữ ký, mục đích, thời hạn và ID; dữ liệu sai trả false, không lộ token. */
    public boolean permits(String token, Integer attemptId) {
        if (token == null || attemptId == null) return false;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).requireAudience("placement-submit")
                    .build().parseClaimsJws(token).getBody();
            return attemptId.toString().equals(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) { return false; }
    }
}
