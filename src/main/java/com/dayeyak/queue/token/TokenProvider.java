package com.dayeyak.queue.token;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secretKey ;
    private final long waitingTokenExpirationTime = 9000000;
    private final long activeTokenExpirationTime = 9000000;

    public String createWaitingToken(String userId) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + waitingTokenExpirationTime);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID createActiveToken(String userId, String waitingToken) {

        //유저 확인
        if (!userId.equals(getUserIdFromWaitingToken(waitingToken))) {
            throw new RuntimeException("유저 일치가 일치하지 않음.");
        }
        //waiting token 만료되었는지 확인
        if (isExpired(waitingToken)) {
            //만료됐으면 버리기
            throw new RuntimeException("토큰 만료됨. 페이지 나간 유저");
        }
        //만료 안됐으면 token 기반으로 새로운 token 생성
        UUID uuid = UUID.nameUUIDFromBytes(waitingToken.getBytes());
        return uuid;
    }

    public String getUserIdFromWaitingToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private boolean isExpired(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token); // 유효하면 그냥 통과
            return false;              // 만료 아님
        } catch (ExpiredJwtException e) {
            return true;               // 만료됨
        } catch (JwtException e) {
            // 서명 불일치, 포맷 오류 등
            throw e;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
