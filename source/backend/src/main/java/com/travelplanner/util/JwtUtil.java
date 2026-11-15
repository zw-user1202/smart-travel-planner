package com.travelplanner.util;

import com.travelplanner.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppProperties props;

    public String generate(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getJwtExpireHours() * 3600_000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, props.getJwtSecret())
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().setSigningKey(props.getJwtSecret()).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser().setSigningKey(props.getJwtSecret()).parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }
}
