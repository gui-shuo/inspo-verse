package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.AIChatMessage;
import com.inspoverse.api.entity.AIChatSession;
import com.inspoverse.api.mapper.AIChatMessageMapper;
import com.inspoverse.api.mapper.AIChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI服务
 */
@Service
@RequiredArgsConstructor
public class AIService {
  private final AIChatSessionMapper sessionMapper;
  private final AIChatMessageMapper messageMapper;

  /**
   * 创建会话
   */
  public AIChatSession createSession(Long userId, String scene, String modelName) {
    AIChatSession session = new AIChatSession();
    session.setSessionNo("AI-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    session.setUserId(userId);
    session.setTitle("新对话");
    session.setModelName(modelName != null ? modelName : "gpt-3.5-turbo");
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
   * 发送消息并获取AI回复（模拟）
   */
  @Transactional
  public AIChatMessage sendMessage(Long userId, Long sessionId, String content) {
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

    // 模拟AI回复（TODO: 接入真实LLM API）
    String aiResponse = generateMockResponse(content);

    AIChatMessage aiMsg = new AIChatMessage();
    aiMsg.setSessionId(sessionId);
    aiMsg.setUserId(userId);
    aiMsg.setRole("assistant");
    aiMsg.setContent(aiResponse);
    aiMsg.setTokens(aiResponse.length());
    aiMsg.setLatencyMs(500);
    aiMsg.setSafetyFlag(0);
    aiMsg.setCreatedAt(LocalDateTime.now());
    aiMsg.setUpdatedAt(LocalDateTime.now());
    aiMsg.setIsDeleted(0);
    messageMapper.insert(aiMsg);

    // 更新会话token统计
    session.setTokenUsed(session.getTokenUsed() + content.length() + aiResponse.length());
    session.setUpdatedAt(LocalDateTime.now());
    sessionMapper.updateById(session);

    return aiMsg;
  }

  /**
   * 模拟AI响应（后续替换为真实LLM调用）
   */
  private String generateMockResponse(String userInput) {
    if (userInput.contains("你好") || userInput.contains("hello")) {
      return "你好！我是 Inspo-Verse 的 AI 助手。我可以帮你解答问题、生成创意内容、提供灵感建议。有什么我可以帮助你的吗？";
    } else if (userInput.contains("帮我")) {
      return "当然可以！请告诉我你需要什么帮助，我会尽力为你提供支持。";
    } else if (userInput.contains("创意") || userInput.contains("灵感")) {
      return "关于创意和灵感，我有以下建议：\n\n1. 尝试跨领域思考，将不同领域的概念结合\n2. 观察日常生活中的细节，从中寻找灵感\n3. 与他人交流，碰撞思想火花\n4. 定期记录你的想法和观察\n\n你想在哪个方向深入探讨呢？";
    } else {
      return "我理解你的问题了。这是一个很有趣的话题！让我为你详细分析一下...\n\n（注意：当前为模拟响应，实际部署时会接入真实的 LLM API，如 OpenAI GPT-4、Claude 等，提供更智能的对话体验）";
    }
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
