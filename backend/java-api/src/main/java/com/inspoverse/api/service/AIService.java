package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.AIChatMessage;
import com.inspoverse.api.entity.AIChatSession;
import com.inspoverse.api.mapper.AIChatMessageMapper;
import com.inspoverse.api.mapper.AIChatSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 服务 — 连接 Python AI 服务（LangChain + DashScope）
 */
@Slf4j
@Service
public class AIService {
  private final AIChatSessionMapper sessionMapper;
  private final AIChatMessageMapper messageMapper;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final ExecutorService sseExecutor;

  @Value("${ai.python-service.base-url:http://localhost:9000}")
  private String pythonServiceBaseUrl;

  @Value("${ai.python-service.internal-sign-key:}")
  private String internalSignKey;

  @Value("${ai.python-service.connect-timeout-ms:5000}")
  private int connectTimeoutMs;

  @Value("${ai.python-service.read-timeout-ms:120000}")
  private int readTimeoutMs;

  public AIService(AIChatSessionMapper sessionMapper, AIChatMessageMapper messageMapper) {
    this.sessionMapper = sessionMapper;
    this.messageMapper = messageMapper;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(5000))
        .build();
    this.sseExecutor = Executors.newCachedThreadPool();
  }

  /**
   * 创建会话
   */
  public AIChatSession createSession(Long userId, String scene, String modelName) {
    AIChatSession session = new AIChatSession();
    session.setSessionNo("AI-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    session.setUserId(userId);
    session.setTitle("新对话");
    session.setModelName(modelName != null ? modelName : "creative");
    session.setScene(scene != null ? scene : "general");
    session.setTokenUsed(0);
    session.setCreatedAt(LocalDateTime.now());
    session.setUpdatedAt(LocalDateTime.now());
    session.setIsDeleted(0);

    sessionMapper.insert(session);
    return session;
  }

  /**
   * 获取用户会话列表
   */
  public List<AIChatSession> getUserSessions(Long userId) {
    return sessionMapper.selectList(new LambdaQueryWrapper<AIChatSession>()
        .eq(AIChatSession::getUserId, userId)
        .eq(AIChatSession::getIsDeleted, 0)
        .orderByDesc(AIChatSession::getUpdatedAt));
  }

  /**
   * 获取会话消息历史
   */
  public List<AIChatMessage> getSessionMessages(Long sessionId) {
    return messageMapper.selectList(new LambdaQueryWrapper<AIChatMessage>()
        .eq(AIChatMessage::getSessionId, sessionId)
        .eq(AIChatMessage::getIsDeleted, 0)
        .orderByAsc(AIChatMessage::getCreatedAt));
  }

  /**
   * 发送消息并获取 AI 流式回复（SSE）
   */
  public SseEmitter sendMessageStream(Long userId, Long sessionId, String content) {
    // 验证会话
    AIChatSession session = sessionMapper.selectById(sessionId);
    if (session == null || session.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
    }
    if (!session.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
    }

    // 保存用户消息
    AIChatMessage userMsg = new AIChatMessage();
    userMsg.setSessionId(sessionId);
    userMsg.setUserId(userId);
    userMsg.setRole("user");
    userMsg.setContent(content);
    userMsg.setTokens(content.length());
    userMsg.setSafetyFlag(0);
    userMsg.setCreatedAt(LocalDateTime.now());
    userMsg.setUpdatedAt(LocalDateTime.now());
    userMsg.setIsDeleted(0);
    messageMapper.insert(userMsg);

    // 获取历史消息作为上下文
    List<AIChatMessage> history = getSessionMessages(sessionId);

    // 构建 SSE Emitter（2 分钟超时）
    SseEmitter emitter = new SseEmitter(readTimeoutMs * 1L);

    sseExecutor.submit(() -> {
      long startTime = System.currentTimeMillis();
      StringBuilder fullResponse = new StringBuilder();

      try {
        // 构建调用 Python AI 服务的消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        for (AIChatMessage msg : history) {
          messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        Map<String, Object> requestBody = Map.of(
            "sessionId", session.getSessionNo(),
            "model", session.getModelName(),
            "messages", messages
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(pythonServiceBaseUrl + "/ai-stream/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(readTimeoutMs))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        if (internalSignKey != null && !internalSignKey.isEmpty()) {
          reqBuilder.header("x-internal-sign", internalSignKey);
        }

        HttpResponse<InputStream> response = httpClient.send(
            reqBuilder.build(),
            HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
          emitter.send(SseEmitter.event().name("error").data("{\"message\":\"AI 服务响应异常: " + response.statusCode() + "\"}"));
          emitter.complete();
          return;
        }

        // Parse SSE stream from Python service
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
          String line;
          String currentEvent = "";

          while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: ")) {
              currentEvent = line.substring(7).trim();
            } else if (line.startsWith("data: ")) {
              String data = line.substring(6);

              // Forward event to frontend
              emitter.send(SseEmitter.event().name(currentEvent).data(data));

              // Collect full response
              if ("token".equals(currentEvent)) {
                try {
                  JsonNode node = objectMapper.readTree(data);
                  if (node.has("delta")) {
                    fullResponse.append(node.get("delta").asText());
                  }
                } catch (Exception ignored) {}
              }
            }
          }
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        // Save AI response message
        if (!fullResponse.isEmpty()) {
          AIChatMessage aiMsg = new AIChatMessage();
          aiMsg.setSessionId(sessionId);
          aiMsg.setUserId(userId);
          aiMsg.setRole("assistant");
          aiMsg.setContent(fullResponse.toString());
          aiMsg.setTokens(fullResponse.length());
          aiMsg.setLatencyMs((int) latencyMs);
          aiMsg.setSafetyFlag(0);
          aiMsg.setCreatedAt(LocalDateTime.now());
          aiMsg.setUpdatedAt(LocalDateTime.now());
          aiMsg.setIsDeleted(0);
          messageMapper.insert(aiMsg);

          // Update session token count
          session.setTokenUsed(session.getTokenUsed() + content.length() + fullResponse.length());
          session.setUpdatedAt(LocalDateTime.now());

          // Auto-generate title from first user message
          if ("新对话".equals(session.getTitle())) {
            String title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
            session.setTitle(title);
          }

          sessionMapper.updateById(session);
        }

        emitter.complete();

      } catch (Exception e) {
        log.error("SSE streaming error for session {}: {}", sessionId, e.getMessage(), e);
        try {
          emitter.send(SseEmitter.event().name("error").data("{\"message\":\"AI 服务异常: " + e.getMessage() + "\"}"));
          emitter.complete();
        } catch (Exception ex) {
          emitter.completeWithError(ex);
        }
      }
    });

    emitter.onTimeout(emitter::complete);
    emitter.onError(e -> log.warn("SSE emitter error: {}", e.getMessage()));

    return emitter;
  }

  /**
   * 发送消息（非流式，兼容旧接口）
   */
  @Transactional
  public AIChatMessage sendMessage(Long userId, Long sessionId, String content) {
    AIChatSession session = sessionMapper.selectById(sessionId);
    if (session == null || session.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
    }
    if (!session.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
    }

    // Save user message
    AIChatMessage userMsg = new AIChatMessage();
    userMsg.setSessionId(sessionId);
    userMsg.setUserId(userId);
    userMsg.setRole("user");
    userMsg.setContent(content);
    userMsg.setTokens(content.length());
    userMsg.setSafetyFlag(0);
    userMsg.setCreatedAt(LocalDateTime.now());
    userMsg.setUpdatedAt(LocalDateTime.now());
    userMsg.setIsDeleted(0);
    messageMapper.insert(userMsg);

    // Call Python AI service synchronously (collect full response)
    List<AIChatMessage> history = getSessionMessages(sessionId);
    String aiResponseContent = callPythonAISync(session, history);

    // Save AI response
    AIChatMessage aiMsg = new AIChatMessage();
    aiMsg.setSessionId(sessionId);
    aiMsg.setUserId(userId);
    aiMsg.setRole("assistant");
    aiMsg.setContent(aiResponseContent);
    aiMsg.setTokens(aiResponseContent.length());
    aiMsg.setLatencyMs(0);
    aiMsg.setSafetyFlag(0);
    aiMsg.setCreatedAt(LocalDateTime.now());
    aiMsg.setUpdatedAt(LocalDateTime.now());
    aiMsg.setIsDeleted(0);
    messageMapper.insert(aiMsg);

    // Update session
    session.setTokenUsed(session.getTokenUsed() + content.length() + aiResponseContent.length());
    session.setUpdatedAt(LocalDateTime.now());
    if ("新对话".equals(session.getTitle())) {
      session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
    }
    sessionMapper.updateById(session);

    return aiMsg;
  }

  /**
   * Call Python AI service synchronously (for non-streaming endpoint)
   */
  private String callPythonAISync(AIChatSession session, List<AIChatMessage> history) {
    try {
      List<Map<String, String>> messages = new ArrayList<>();
      for (AIChatMessage msg : history) {
        messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
      }

      Map<String, Object> requestBody = Map.of(
          "sessionId", session.getSessionNo(),
          "model", session.getModelName(),
          "messages", messages
      );

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
          .uri(URI.create(pythonServiceBaseUrl + "/ai-stream/v1/chat/completions"))
          .header("Content-Type", "application/json")
          .timeout(Duration.ofMillis(readTimeoutMs))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

      if (internalSignKey != null && !internalSignKey.isEmpty()) {
        reqBuilder.header("x-internal-sign", internalSignKey);
      }

      HttpResponse<InputStream> response = httpClient.send(
          reqBuilder.build(),
          HttpResponse.BodyHandlers.ofInputStream()
      );

      StringBuilder fullResponse = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("data: ")) {
            String data = line.substring(6);
            try {
              JsonNode node = objectMapper.readTree(data);
              if (node.has("delta")) {
                fullResponse.append(node.get("delta").asText());
              }
            } catch (Exception ignored) {}
          }
        }
      }

      return fullResponse.isEmpty()
          ? "抱歉，AI 服务暂时无法响应，请稍后重试。"
          : fullResponse.toString();

    } catch (Exception e) {
      log.error("Sync AI call error: {}", e.getMessage(), e);
      return "抱歉，AI 服务暂时无法响应，请稍后重试。";
    }
  }

  /**
   * 转发文件到 Python AI 服务提取内容
   */
  public Map<String, Object> extractFileContent(byte[] fileBytes, String filename) {
    try {
      String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      String header = "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
          "Content-Type: application/octet-stream\r\n\r\n";
      baos.write(header.getBytes(StandardCharsets.UTF_8));
      baos.write(fileBytes);
      baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

      HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
          .uri(URI.create(pythonServiceBaseUrl + "/ai-stream/v1/file/extract"))
          .header("Content-Type", "multipart/form-data; boundary=" + boundary)
          .timeout(Duration.ofMillis(30000))
          .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()));

      if (internalSignKey != null && !internalSignKey.isEmpty()) {
        reqBuilder.header("x-internal-sign", internalSignKey);
      }

      HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("data")) {
          JsonNode data = root.get("data");
          Map<String, Object> result = new HashMap<>();
          result.put("text", data.has("text") ? data.get("text").asText() : "");
          result.put("fileType", data.has("file_type") ? data.get("file_type").asText() : "");
          result.put("fileName", data.has("file_name") ? data.get("file_name").asText() : filename);
          result.put("charCount", data.has("char_count") ? data.get("char_count").asInt() : 0);
          return result;
        }
      }

      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "文件解析服务异常");

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("File extraction error: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "文件解析服务不可用");
    }
  }

  /**
   * 转发音频到 Python AI 服务进行语音识别
   */
  public Map<String, Object> transcribeVoice(byte[] audioBytes, String filename) {
    try {
      String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      String header = "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
          "Content-Type: application/octet-stream\r\n\r\n";
      baos.write(header.getBytes(StandardCharsets.UTF_8));
      baos.write(audioBytes);
      baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

      HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
          .uri(URI.create(pythonServiceBaseUrl + "/ai-stream/v1/voice/transcribe"))
          .header("Content-Type", "multipart/form-data; boundary=" + boundary)
          .timeout(Duration.ofMillis(60000))
          .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()));

      if (internalSignKey != null && !internalSignKey.isEmpty()) {
        reqBuilder.header("x-internal-sign", internalSignKey);
      }

      HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("data")) {
          JsonNode data = root.get("data");
          Map<String, Object> result = new HashMap<>();
          result.put("text", data.has("text") ? data.get("text").asText() : "");
          result.put("durationMs", data.has("duration_ms") && !data.get("duration_ms").isNull() ? data.get("duration_ms").asInt() : null);
          return result;
        }
      }

      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音识别服务异常");

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Voice transcription error: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音识别服务不可用");
    }
  }

  /**
   * 更新会话模型
   */
  public void updateSessionModel(Long userId, Long sessionId, String modelName) {
    AIChatSession session = sessionMapper.selectById(sessionId);
    if (session == null || session.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
    }
    if (!session.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此会话");
    }
    session.setModelName(modelName);
    session.setUpdatedAt(LocalDateTime.now());
    sessionMapper.updateById(session);
  }

  /**
   * 删除会话
   */
  public void deleteSession(Long userId, Long sessionId) {
    AIChatSession session = sessionMapper.selectById(sessionId);
    if (session == null || session.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
    }
    if (!session.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此会话");
    }

    session.setIsDeleted(1);
    session.setUpdatedAt(LocalDateTime.now());
    sessionMapper.updateById(session);
  }
}
