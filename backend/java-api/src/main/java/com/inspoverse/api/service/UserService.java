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
   * 更新用户信息（昵称、头像、简介、手机号）
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

  /**
   * 更新用户完整资料（含bio和phone）
   */
  public void updateProfile(Long userId, String nickname, String bio, String phone) {
    User user = getUserById(userId);
    if (nickname != null && !nickname.isBlank()) {
      user.setNickname(nickname.trim());
    }
    if (bio != null) {
      user.setBio(bio.trim());
    }
    if (phone != null && !phone.isBlank()) {
      // 验证手机号格式
      if (!phone.matches("^1[3-9]\\d{9}$")) {
        throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
      }
      // 检查手机号是否被其他用户占用
      User existingPhone = userMapper.selectOne(new LambdaQueryWrapper<User>()
          .eq(User::getPhone, phone)
          .eq(User::getIsDeleted, 0)
          .ne(User::getId, userId));
      if (existingPhone != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被其他账号绑定");
      }
      user.setPhone(phone);
    }
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
  }

  /**
   * 更新头像
   */
  public void updateAvatar(Long userId, String avatarUrl) {
    User user = getUserById(userId);
    user.setAvatarUrl(avatarUrl);
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
  }

  /**
   * 修改密码
   */
  public void changePassword(Long userId, String currentPassword, String newPassword) {
    User user = getUserById(userId);
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "当前密码不正确");
    }
    // 新密码强度校验：8位以上含大小写字母
    if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码需8位以上且包含大小写字母及数字");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
  }

  /**
   * 手机号脱敏展示
   */
  public static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) return phone;
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }
}
