package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {
  private final UserMapper userMapper;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /**
   * 根据用户名查询用户
   */
  public User getUserByUsername(String username) {
    return userMapper.selectOne(new LambdaQueryWrapper<User>()
        .eq(User::getUsername, username)
        .eq(User::getIsDeleted, 0));
  }

  /**
   * 根据ID查询用户
   */
  public User getUserById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null || user.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
    }
    return user;
  }

  /**
   * 注册用户
   */
  public User register(String username, String phone, String password) {
    // 检查用户名是否已存在
    if (getUserByUsername(username) != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
    }

    // 检查手机号是否已存在
    User existingPhone = userMapper.selectOne(new LambdaQueryWrapper<User>()
        .eq(User::getPhone, phone)
        .eq(User::getIsDeleted, 0));
    if (existingPhone != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "手机号已被注册");
    }

    // 创建用户
    User user = new User();
    user.setUsername(username);
    user.setPhone(phone);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setNickname(username); // 默认昵称为用户名
    user.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username);
    user.setStatus(1);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setIsDeleted(0);

    userMapper.insert(user);
    return user;
  }

  /**
   * 验证密码
   */
  public boolean verifyPassword(User user, String rawPassword) {
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }

  /**
   * 更新最后登录时间
   */
  public void updateLastLogin(Long userId) {
    User user = new User();
    user.setId(userId);
    user.setLastLoginAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
  }

  /**
   * 更新用户信息
   */
  public void updateUser(Long userId, String nickname, String avatarUrl) {
    User user = getUserById(userId);
    if (nickname != null) {
      user.setNickname(nickname);
    }
    if (avatarUrl != null) {
      user.setAvatarUrl(avatarUrl);
    }
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
  }
}
