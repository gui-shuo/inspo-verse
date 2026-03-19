package com.inspoverse.api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration-ms}")
  private long expirationMs;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshExpirationMs;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 生成访问令牌
   */
  public String generateAccessToken(Long userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    return createToken(claims, expirationMs);
  }

  /**
   * 生成刷新令牌
   */
  public String generateRefreshToken(Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("type", "refresh");
    return createToken(claims, refreshExpirationMs);
  }

  private String createToken(Map<String, Object> claims, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .claims(claims)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * 解析令牌
   */
  public Claims parseToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * 从令牌中获取用户ID
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = parseToken(token);
    return claims.get("userId", Long.class);
  }

  /**
   * 从令牌中获取用户名
   */
  public String getUsernameFromToken(String token) {
    Claims claims = parseToken(token);
    return claims.get("username", String.class);
  }

  /**
   * 验证令牌是否有效
   */
  public boolean validateToken(String token) {
    try {
      Claims claims = parseToken(token);
      return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
      return false;
    }
  }
}
