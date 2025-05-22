package com.example.political_chat_backend;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // 구체적인 예외 타입
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 임포트
import org.springframework.stereotype.Component;

import java.security.Key; // Key 임포트
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    private Key key;

    // 생성자 또는 @PostConstruct에서 key 초기화
    @jakarta.annotation.PostConstruct // 또는 javax.annotation.PostConstruct (Spring Boot 3 이전)
    public void init() {
        // Base64 인코딩된 문자열로부터 HMAC-SHA 키 생성
        // jwtSecret 문자열의 길이가 HMAC-SHA256 (HS256)에 충분한지 확인 필요
        // (일반적으로 256비트 = 32바이트 이상)
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Authentication 객체로부터 JWT 토큰 생성
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // 사용자 이름 (또는 ID)
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS512) // 사용할 암호화 알고리즘과 비밀키
                // .signWith(SignatureAlgorithm.HS512, jwtSecret) // key 객체 대신 직접 문자열 사용 시 (권장하지 않음, Keys.hmacShaKeyFor 권장)
                .compact();
    }

    // JWT 토큰에서 사용자 이름 추출
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
// return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject(); // 이전 방식
    }

    // JWT 토큰 유효성 검사
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            // Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken); // 이전 방식
            return true;
        } catch (SignatureException e) { // 이전에는 MalformedJwtException 이었으나 jjwt 0.11.x 부터 변경 가능성 있음, 또는 SecurityException
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
