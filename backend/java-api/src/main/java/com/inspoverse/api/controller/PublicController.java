package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.EmailSubscription;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.mapper.EmailSubscriptionMapper;
import com.inspoverse.api.mapper.UserFollowMapper;
import com.inspoverse.api.mapper.UserMapper;
import com.inspoverse.api.entity.UserFollow;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

  private final UserMapper userMapper;
  private final UserFollowMapper userFollowMapper;
  private final EmailSubscriptionMapper emailSubscriptionMapper;

  @GetMapping("/rules")
  public ApiResponse<Map<String, String>> rules() {
    return ApiResponse.success(Map.of("content", "请遵守社区规范，文明发言。"));
  }

  @GetMapping("/contact")
  public ApiResponse<Map<String, String>> contact() {
    return ApiResponse.success(Map.of("email", "support@inspo-verse.com"));
  }

  /**
   * 获取本周热门创作者（按粉丝数排序前8名）
   */
  @GetMapping("/top-creators")
  public ApiResponse<List<Map<String, Object>>> topCreators(
      @RequestParam(defaultValue = "8") int limit
  ) {
    // 手动聚合粉丝数
    Map<Long, Long> fanCountMap = new HashMap<>();
    List<UserFollow> allFollows = userFollowMapper.selectList(null);
    for (UserFollow uf : allFollows) {
      fanCountMap.merge(uf.getFollowingId(), 1L, Long::sum);
    }

    // 按粉丝数倒排，取 top N
    List<Long> topUserIds = fanCountMap.entrySet().stream()
        .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
        .limit(limit)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    if (topUserIds.isEmpty()) {
      // 没有关注数据时，返回最新注册的用户
      List<User> latestUsers = userMapper.selectList(
          new LambdaQueryWrapper<User>()
              .eq(User::getIsDeleted, 0)
              .eq(User::getStatus, 1)
              .orderByDesc(User::getCreatedAt)
              .last("LIMIT " + limit)
      );
      List<Map<String, Object>> result = latestUsers.stream().map(u -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getId());
        m.put("nickname", u.getNickname());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("bio", u.getBio());
        m.put("totalLikes", 0);
        m.put("followerCount", 0);
        return m;
      }).collect(Collectors.toList());
      return ApiResponse.success(result);
    }

    // 查询用户信息
    List<User> users = userMapper.selectBatchIds(topUserIds);
    Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

    List<Map<String, Object>> result = topUserIds.stream()
        .filter(userMap::containsKey)
        .map(uid -> {
          User u = userMap.get(uid);
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("userId", u.getId());
          m.put("nickname", u.getNickname());
          m.put("avatarUrl", u.getAvatarUrl());
          m.put("bio", u.getBio());
          m.put("totalLikes", fanCountMap.getOrDefault(uid, 0L));
          m.put("followerCount", fanCountMap.getOrDefault(uid, 0L));
          return m;
        })
        .collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  /**
   * 邮箱订阅
   */
  @PostMapping("/subscribe")
  public ApiResponse<Map<String, Object>> subscribe(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入有效的邮箱地址");
    }

    // 检查是否已订阅
    EmailSubscription existing = emailSubscriptionMapper.selectOne(
        new LambdaQueryWrapper<EmailSubscription>().eq(EmailSubscription::getEmail, email)
    );

    if (existing != null) {
      if (existing.getStatus() == 1) {
        return ApiResponse.success(Map.of("subscribed", true, "message", "您已订阅过"));
      }
      // 重新激活
      existing.setStatus(1);
      existing.setUpdatedAt(LocalDateTime.now());
      emailSubscriptionMapper.updateById(existing);
      return ApiResponse.success(Map.of("subscribed", true, "message", "重新订阅成功"));
    }

    // 新订阅
    EmailSubscription sub = new EmailSubscription();
    sub.setEmail(email);
    sub.setStatus(1);
    sub.setCreatedAt(LocalDateTime.now());
    sub.setUpdatedAt(LocalDateTime.now());
    emailSubscriptionMapper.insert(sub);

    return ApiResponse.success(Map.of("subscribed", true, "message", "订阅成功！我们会定期发送最新动态到您的邮箱"));
  }

  /**
   * 取消邮箱订阅
   */
  @PostMapping("/unsubscribe")
  public ApiResponse<Map<String, Object>> unsubscribe(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    if (email == null || email.isBlank()) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "请提供邮箱地址");
    }

    EmailSubscription existing = emailSubscriptionMapper.selectOne(
        new LambdaQueryWrapper<EmailSubscription>().eq(EmailSubscription::getEmail, email)
    );
    if (existing != null && existing.getStatus() == 1) {
      existing.setStatus(0);
      existing.setUpdatedAt(LocalDateTime.now());
      emailSubscriptionMapper.updateById(existing);
    }
    return ApiResponse.success(Map.of("subscribed", false, "message", "已取消订阅"));
  }
}
