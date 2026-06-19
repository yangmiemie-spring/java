package com.yxy.monitor.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretStr;
    @Value("${jwt.expire}")
    private Long expireTime;

    // 获取加密密钥
    private SecretKey getSecretKey() {
        byte[] bytes = secretStr.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(bytes, SignatureAlgorithm.HS256.getJcaName());
    }

    // 生成token
    public String generateToken(Long userId, String username){
        long now = System.currentTimeMillis();
        Date expire = new Date(now + expireTime);
        return Jwts.builder()
                .setSubject(username)
                .setId(userId.toString())
                .setIssuedAt(new Date(now))
                .setExpiration(expire)
                .signWith(getSecretKey())
                .compact();

    }

    // 解析token
    public String getUsername(String toke){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(toke)
                .getBody();
        return claims.getSubject();
    }

    // 检验Token是否有效
    public boolean verifyToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch(Exception e){
            return false;
        }
    }
}
