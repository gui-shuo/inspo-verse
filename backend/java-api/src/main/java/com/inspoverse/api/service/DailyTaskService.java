package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.DailyTask;
import com.inspoverse.api.entity.UserTaskProgress;
import com.inspoverse.api.mapper.DailyTaskMapper;
import com.inspoverse.api.mapper.UserTaskProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTaskService {

  private final DailyTaskMapper dailyTaskMapper;
  private final UserTaskProgressMapper progressMapper;
  private final WalletService walletService;
  private final ExperienceService experienceService;

  /**
   * 获取所有启用的每日任务，并附带用户今日进度
   */
  public List<Map<String, Object>> getTodayTasks(Long userId) {
    List<DailyTask> tasks = dailyTaskMapper.selectList(
        new LambdaQueryWrapper<DailyTask>()
            .eq(DailyTask::getStatus, 1)
            .orderByAsc(DailyTask::getSortOrder));

    LocalDate today = LocalDate.now();
    List<UserTaskProgress> todayProgress = progressMapper.selectList(
        new LambdaQueryWrapper<UserTaskProgress>()
            .eq(UserTaskProgress::getUserId, userId)
            .eq(UserTaskProgress::getTaskDate, today));

    Map<String, UserTaskProgress> progressMap = todayProgress.stream()
        .collect(Collectors.toMap(UserTaskProgress::getTaskCode, p -> p));

    List<Map<String, Object>> result = new ArrayList<>();
    for (DailyTask task : tasks) {
      UserTaskProgress progress = progressMap.get(task.getTaskCode());
      Map<String, Object> item = new HashMap<>();
      item.put("taskCode", task.getTaskCode());
      item.put("taskName", task.getTaskName());
      item.put("description", task.getDescription());
      item.put("rewardPoints", task.getRewardPoints());
      item.put("rewardExp", task.getRewardExp());
      item.put("dailyLimit", task.getDailyLimit());
      item.put("taskType", task.getTaskType());
      item.put("routePath", task.getRoutePath());

      int current = progress != null ? progress.getProgress() : 0;
      boolean completed = current >= task.getDailyLimit();
      boolean rewarded = progress != null && progress.getRewarded() == 1;

      item.put("progress", current);
      item.put("completed", completed);
      item.put("rewarded", rewarded);

      // 状态文本
      if (completed && rewarded) {
        item.put("statusText", "已完成");
      } else if (completed && !rewarded) {
        item.put("statusText", "领取奖励");
      } else if (task.getDailyLimit() > 1) {
        item.put("statusText", current + "/" + task.getDailyLimit());
      } else {
        item.put("statusText", "去完成");
      }

      result.add(item);
    }
    return result;
  }

  /**
   * 记录任务进度（由其他业务自动调用：签到、发帖、AI对话等）
   */
  @Transactional
  public void recordProgress(Long userId, String taskCode) {
    DailyTask task = dailyTaskMapper.selectOne(
        new LambdaQueryWrapper<DailyTask>()
            .eq(DailyTask::getTaskCode, taskCode)
            .eq(DailyTask::getStatus, 1));
    if (task == null) return;

    LocalDate today = LocalDate.now();
    UserTaskProgress progress = progressMapper.selectOne(
        new LambdaQueryWrapper<UserTaskProgress>()
            .eq(UserTaskProgress::getUserId, userId)
            .eq(UserTaskProgress::getTaskCode, taskCode)
            .eq(UserTaskProgress::getTaskDate, today));

    if (progress == null) {
      progress = new UserTaskProgress();
      progress.setUserId(userId);
      progress.setTaskCode(taskCode);
      progress.setTaskDate(today);
      progress.setProgress(1);
      progress.setCompleted(1 >= task.getDailyLimit() ? 1 : 0);
      progress.setRewarded(0);
      progress.setCreatedAt(LocalDateTime.now());
      progress.setUpdatedAt(LocalDateTime.now());
      progressMapper.insert(progress);
    } else if (progress.getProgress() < task.getDailyLimit()) {
      progress.setProgress(progress.getProgress() + 1);
      progress.setCompleted(progress.getProgress() >= task.getDailyLimit() ? 1 : 0);
      progress.setUpdatedAt(LocalDateTime.now());
      progressMapper.updateById(progress);
    }
  }

  /**
   * 领取任务奖励（手动领取）
   */
  @Transactional
  public Map<String, Object> claimReward(Long userId, String taskCode) {
    DailyTask task = dailyTaskMapper.selectOne(
        new LambdaQueryWrapper<DailyTask>()
            .eq(DailyTask::getTaskCode, taskCode)
            .eq(DailyTask::getStatus, 1));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
    }

    LocalDate today = LocalDate.now();
    UserTaskProgress progress = progressMapper.selectOne(
        new LambdaQueryWrapper<UserTaskProgress>()
            .eq(UserTaskProgress::getUserId, userId)
            .eq(UserTaskProgress::getTaskCode, taskCode)
            .eq(UserTaskProgress::getTaskDate, today));

    if (progress == null || progress.getProgress() < task.getDailyLimit()) {
      throw new BusinessException(ErrorCode.CONFLICT, "任务尚未完成");
    }
    if (progress.getRewarded() == 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "奖励已领取");
    }

    // 发放积分奖励
    if (task.getRewardPoints() > 0) {
      walletService.earn(userId, task.getRewardPoints(), "EARN_TASK",
          "任务奖励：" + task.getTaskName(), "TASK:" + taskCode);
    }

    // 发放经验奖励
    if (task.getRewardExp() > 0) {
      experienceService.addExp(userId, task.getRewardExp(), "TASK",
          "任务奖励：" + task.getTaskName(), "TASK:" + taskCode);
    }

    progress.setRewarded(1);
    progress.setUpdatedAt(LocalDateTime.now());
    progressMapper.updateById(progress);

    Map<String, Object> result = new HashMap<>();
    result.put("pointsEarned", task.getRewardPoints());
    result.put("expEarned", task.getRewardExp());
    result.put("taskCode", taskCode);
    return result;
  }

  /**
   * 完成签到任务（签到是特殊任务，一键完成+领取）
   */
  @Transactional
  public Map<String, Object> completeSignIn(Long userId) {
    String taskCode = "DAILY_SIGNIN";
    LocalDate today = LocalDate.now();

    UserTaskProgress progress = progressMapper.selectOne(
        new LambdaQueryWrapper<UserTaskProgress>()
            .eq(UserTaskProgress::getUserId, userId)
            .eq(UserTaskProgress::getTaskCode, taskCode)
            .eq(UserTaskProgress::getTaskDate, today));

    if (progress != null && progress.getRewarded() == 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "今日已签到");
    }

    // 记录进度
    recordProgress(userId, taskCode);

    // 自动领取奖励
    return claimReward(userId, taskCode);
  }
}
