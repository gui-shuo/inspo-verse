package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.UserOAuthBinding;
import com.inspoverse.api.mapper.UserOAuthBindingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 第三方账号绑定服务
 * 支持 GitHub / Discord OAuth2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthBindingService {

  private final UserOAuthBindingMapper bindingMapper;

  @Value("${oauth.github.client-id:}")
  private String githubClientId;

  @Value("${oauth.github.client-secret:}")
  private String githubClientSecret;

  @Value("${oauth.github.redirect-uri:http://localhost:5173/oauth/github/callback}")
  private String githubRedirectUri;

  @Value("${oauth.discord.client-id:}")
  private String discordClientId;

  @Value("${oauth.discord.client-secret:}")
  private String discordClientSecret;

  @Value("${oauth.discord.redirect-uri:http://localhost:5173/oauth/discord/callback}")
  private String discordRedirectUri;

  /**
   * 获取用户所有第三方绑定
   */
  public List<UserOAuthBinding> getUserBindings(Long userId) {
    return bindingMapper.selectList(new LambdaQueryWrapper<UserOAuthBinding>()
        .eq(UserOAuthBinding::getUserId, userId));
  }

  /**
   * 获取OAuth授权跳转URL
   */
  public String getAuthorizationUrl(String provider, String state) {
    return switch (provider.toLowerCase()) {
      case "github" -> {
        if (githubClientId.isEmpty()) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GitHub OAuth未配置");
        yield "https://github.com/login/oauth/authorize?client_id=" + githubClientId
            + "&redirect_uri=" + githubRedirectUri
            + "&scope=user:email&state=" + state;
      }
      case "discord" -> {
        if (discordClientId.isEmpty()) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Discord OAuth未配置");
        yield "https://discord.com/oauth2/authorize?client_id=" + discordClientId
            + "&redirect_uri=" + java.net.URLEncoder.encode(discordRedirectUri, java.nio.charset.StandardCharsets.UTF_8)
            + "&response_type=code&scope=identify+email&state=" + state;
      }
      default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的平台: " + provider);
    };
  }

  /**
   * 通过 OAuth code 换取用户信息并绑定
   */
  public UserOAuthBinding bindOAuth(Long userId, String provider, String code) {
    // 换取 access_token
    String accessToken = exchangeCodeForToken(provider, code);
    // 获取平台用户信息
    OAuthUserInfo oauthUser = fetchUserInfo(provider, accessToken);

    // 检查是否已被其他账号绑定
    UserOAuthBinding existBinding = bindingMapper.selectOne(
        new LambdaQueryWrapper<UserOAuthBinding>()
            .eq(UserOAuthBinding::getProvider, provider)
            .eq(UserOAuthBinding::getProviderUserId, oauthUser.providerId()));
    if (existBinding != null && !existBinding.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "该" + provider + "账号已绑定其他用户");
    }

    // 更新或创建绑定
    UserOAuthBinding binding = bindingMapper.selectOne(
        new LambdaQueryWrapper<UserOAuthBinding>()
            .eq(UserOAuthBinding::getUserId, userId)
            .eq(UserOAuthBinding::getProvider, provider));

    if (binding == null) {
      binding = new UserOAuthBinding();
      binding.setUserId(userId);
      binding.setProvider(provider);
      binding.setCreatedAt(LocalDateTime.now());
    }
    binding.setProviderUserId(oauthUser.providerId());
    binding.setProviderUsername(oauthUser.username());
    binding.setAccessToken(accessToken); // 生产环境应加密存储
    binding.setUpdatedAt(LocalDateTime.now());

    if (binding.getId() == null) {
      bindingMapper.insert(binding);
    } else {
      bindingMapper.updateById(binding);
    }
    return binding;
  }

  /**
   * 解绑第三方账号
   */
  public void unbind(Long userId, String provider) {
    int rows = bindingMapper.delete(new LambdaQueryWrapper<UserOAuthBinding>()
        .eq(UserOAuthBinding::getUserId, userId)
        .eq(UserOAuthBinding::getProvider, provider));
    if (rows == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到绑定关系");
    }
  }

  // ========================= 内部工具方法 =========================

  private String exchangeCodeForToken(String provider, String code) {
    // 生产环境使用 RestTemplate / WebClient 调用 OAuth token endpoint
    // 此处为演示框架，实际需要 HTTP 调用
    log.info("OAuth token exchange: provider={}, code={}", provider, code.substring(0, Math.min(8, code.length())));
    return switch (provider.toLowerCase()) {
      case "github" -> exchangeGithubToken(code);
      case "discord" -> exchangeDiscordToken(code);
      default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的平台");
    };
  }

  private String exchangeGithubToken(String code) {
    try {
      var client = java.net.http.HttpClient.newHttpClient();
      var body = "client_id=" + githubClientId
          + "&client_secret=" + githubClientSecret
          + "&code=" + code
          + "&redirect_uri=" + java.net.URLEncoder.encode(githubRedirectUri, java.nio.charset.StandardCharsets.UTF_8);
      var request = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://github.com/login/oauth/access_token"))
          .header("Accept", "application/json")
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();
      var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      // 解析 JSON: {"access_token":"...","token_type":"bearer",...}
      String respBody = response.body();
      return extractJsonField(respBody, "access_token");
    } catch (Exception e) {
      log.error("GitHub token exchange failed", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GitHub授权失败");
    }
  }

  private String exchangeDiscordToken(String code) {
    try {
      var client = java.net.http.HttpClient.newHttpClient();
      var body = "client_id=" + discordClientId
          + "&client_secret=" + discordClientSecret
          + "&grant_type=authorization_code"
          + "&code=" + code
          + "&redirect_uri=" + java.net.URLEncoder.encode(discordRedirectUri, java.nio.charset.StandardCharsets.UTF_8);
      var request = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://discord.com/api/oauth2/token"))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();
      var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      return extractJsonField(response.body(), "access_token");
    } catch (Exception e) {
      log.error("Discord token exchange failed", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Discord授权失败");
    }
  }

  private OAuthUserInfo fetchUserInfo(String provider, String accessToken) {
    try {
      return switch (provider.toLowerCase()) {
        case "github" -> fetchGithubUser(accessToken);
        case "discord" -> fetchDiscordUser(accessToken);
        default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的平台");
      };
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("OAuth user info fetch failed: provider={}", provider, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取用户信息失败");
    }
  }

  private OAuthUserInfo fetchGithubUser(String accessToken) throws Exception {
    var client = java.net.http.HttpClient.newHttpClient();
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create("https://api.github.com/user"))
        .header("Authorization", "Bearer " + accessToken)
        .header("Accept", "application/vnd.github+json")
        .GET().build();
    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    String body = response.body();
    return new OAuthUserInfo(extractJsonField(body, "id"), extractJsonField(body, "login"));
  }

  private OAuthUserInfo fetchDiscordUser(String accessToken) throws Exception {
    var client = java.net.http.HttpClient.newHttpClient();
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create("https://discord.com/api/users/@me"))
        .header("Authorization", "Bearer " + accessToken)
        .GET().build();
    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    String body = response.body();
    String userId = extractJsonField(body, "id");
    String username = extractJsonField(body, "username");
    String discriminator = extractJsonField(body, "discriminator");
    String displayName = "0000".equals(discriminator) ? username : username + "#" + discriminator;
    return new OAuthUserInfo(userId, displayName);
  }

  /**
   * 简单 JSON 字段提取（避免引入额外依赖，生产可替换为 Jackson）
   */
  private String extractJsonField(String json, String field) {
    String key = "\"" + field + "\"";
    int idx = json.indexOf(key);
    if (idx < 0) return "";
    int colon = json.indexOf(':', idx + key.length());
    if (colon < 0) return "";
    int start = colon + 1;
    while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) start++;
    int end = start;
    char delimiter = json.charAt(colon + 1) == '"' ? '"' : ',';
    if (delimiter == '"') {
      // string value
      end = json.indexOf('"', start);
    } else {
      // number value
      end = json.indexOf(',', start);
      if (end < 0) end = json.indexOf('}', start);
    }
    return end > start ? json.substring(start, end).trim() : "";
  }

  private record OAuthUserInfo(String providerId, String username) {}
}
