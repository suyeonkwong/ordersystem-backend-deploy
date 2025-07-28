package com.example.ordersystem.common.auth;

import com.example.ordersystem.member.domain.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.expirationAt}")
    private int expirationAt;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;
    private Key secret_at_key;

    /// 스프링 빈이 만들어지는 시점에 빈이 만들어진 직후에 아래 메서드가 바로 실행
    @PostConstruct
    public void makeKey() {
        secret_at_key = new SecretKeySpec(Base64.getDecoder().decode(secretKeyAt), SignatureAlgorithm.HS512.getJcaName());
    }

    public String createAtToken(Member member) {
        String email = member.getEmail();
        String role = member.getRole().toString();
        /// claims는 페이로드(사용자 정보)
        Claims claims = Jwts.claims().setSubject(email);
        /// 주된 키값을 제외한 나머지 사용자정보는 put 사용하여 key : value 세팅
        claims.put("role", role);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt * 60 * 1000L))    /// 30분
                /// secret키를 통해 signiture 생성
                .signWith(secret_at_key)
                .compact();

    }

    public String createRtToken(Member member) {
        /// 유효기간이 긴 rt 토큰 생성
        /// rt 토큰을 redis에 저장
        return null;
    }
}
