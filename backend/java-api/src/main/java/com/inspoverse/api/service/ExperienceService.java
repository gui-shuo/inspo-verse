package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.entity.ExpRecord;
import com.inspoverse.api.entity.UserExperience;
import com.inspoverse.api.mapper.ExpRecordMapper;
import com.inspoverse.api.mapper.UserExperienceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceService {

  private final UserExperienceMapper experienceMapper;
  private final ExpRecordMapper expRecordMapper;

  /**
   * 等级阶梯定义：level -> [所需累计经验, 等级称号]
   */
  private static final Map<Integer, long[]> LEVEL_THRESHOLDS = new LinkedHashMap<>();
  private static final Map<Integer, String> LEVEL_NAMES = new LinkedHashMap<>();

  static {
    LEVEL_NAMES.put(1, "灵感新手");
    LEVEL_NAMES.put(2, "创意学徒");
    LEVEL_NAMES.put(3, "数据行者");
    LEVEL_NAMES.put(4, "赛博猎人");
    LEVEL_NAMES.put(5, "赛博游侠");
    LEVEL_NAMES.put(6, "赛博领主");
    LEVEL_NAMES.put(7, "灵感大师");
    LEVEL_NAMES.put(8, "创世之神");

    LEVEL_THRESHOLDS.put(1, new long[]{0});
    LEVEL_THRESHOLDS.put(2, new long[]{100});
    LEVEL_THRESHOLDS.put(3, new long[]{500});
    LEVEL_THRESHOLDS.put(4, new long[]{1500});
    LEVEL_THRESHOLDS.put(5, new long[]{3500});
    LEVEL_THRESHOLDS.put(6, new long[]{7000});
    LEVEL_THRESHOLDS.put(7, new long[]{15000});
    LEVEL_THRESHOLDS.put(8, new long[]{30000});
  }

  public UserExperience getOrCreate(Long userId) {
    UserExperience exp = experienceMapper.selectOne(
        new LambdaQueryWrapper<UserExperience>().eq(UserExperience::getUserId, userId));
    if (exp == null) {
      exp = new UserExperience();
      exp.setUserId(userId);
      exp.setExpPoints(0);
      exp.setLevel(1);
      exp.setLevelName("灵感新手");
      exp.setUpdatedAt(LocalDateTime.now());
      experienceMapper.insert(exp);
    }
    return exp;
  }

  @Transactional
  public UserExperience addExp(Long userId, int amount, String source, String description, String refId) {
    if (amount <= 0) return getOrCreate(userId);

    UserExperience exp = getOrCreate(userId);
    exp.setExpPoints(exp.getExpPoints() + amount);

    // 重新计算等级
    int newLevel = calculateLevel(exp.getExpPoints());
    exp.setLevel(newLevel);
    exp.setLevelName(LEVEL_NAMES.getOrDefault(newLevel, "灵感新手"));
    exp.setUpdatedAt(LocalDateTime.now());
    experienceMapper.updateById(exp);

    // 记录经验流水
    ExpRecord record = new ExpRecord();
    record.setUserId(userId);
    record.setAmount(amount);
    record.setSource(source);
    record.setDescription(description);
    record.setRefId(refId);
    record.setCreatedAt(LocalDateTime.now());
    expRecordMapper.insert(record);

    log.info("[Experience] userId={} +{} source={} total={} level={}",
        userId, amount, source, exp.getExpPoints(), newLevel);
    return exp;
  }

  /**
   * 获取用户经验等级详情（含升级进度）
   */
  public Map<String, Object> getUserExpDetail(Long userId) {
    UserExperience exp = getOrCreate(userId);
    Map<String, Object> result = new HashMap<>();
    result.put("level", exp.getLevel());
    result.put("levelName", exp.getLevelName());
    result.put("expPoints", exp.getExpPoints());

    // 当前等级所需经验
    long currentThreshold = LEVEL_THRESHOLDS.getOrDefault(exp.getLevel(), new long[]{0})[0];
    // 下一等级所需经验
    int nextLevel = exp.getLevel() + 1;
    if (LEVEL_THRESHOLDS.containsKey(nextLevel)) {
      long nextThreshold = LEVEL_THRESHOLDS.get(nextLevel)[0];
      result.put("currentLevelExp", currentThreshold);
      result.put("nextLevelExp", nextThreshold);
      result.put("nextLevelName", LEVEL_NAMES.get(nextLevel));
      result.put("progressInLevel", exp.getExpPoints() - currentThreshold);
      result.put("expNeeded", nextThreshold - currentThreshold);
      result.put("isMaxLevel", false);
    } else {
      result.put("currentLevelExp", currentThreshold);
      result.put("nextLevelExp", currentThreshold);
      result.put("nextLevelName", exp.getLevelName());
      result.put("progressInLevel", 0);
      result.put("expNeeded", 0);
      result.put("isMaxLevel", true);
    }
    return result;
  }

  /**
   * 获取成长轨迹（近12个月累计经验变化 + 等级阈值）
   */
  public Map<String, Object> getGrowthTrajectory(Long userId) {
    UserExperience exp = getOrCreate(userId);
    YearMonth now = YearMonth.now();

    // 查询12个月窗口内所有经验记录
    LocalDateTime windowStart = now.minusMonths(11).atDay(1).atStartOfDay();
    List<ExpRecord> allRecords = expRecordMapper.selectList(
        new LambdaQueryWrapper<ExpRecord>()
            .eq(ExpRecord::getUserId, userId)
            .ge(ExpRecord::getCreatedAt, windowStart)
            .orderByAsc(ExpRecord::getCreatedAt));

    // 计算12个月窗口起点时的累计经验
    int totalInWindow = allRecords.stream().mapToInt(ExpRecord::getAmount).sum();
    int expAtWindowStart = exp.getExpPoints() - totalInWindow;

    List<Map<String, Object>> points = new ArrayList<>();
    int runningExp = Math.max(expAtWindowStart, 0);

    for (int i = 11; i >= 0; i--) {
      YearMonth month = now.minusMonths(i);
      LocalDateTime mStart = month.atDay(1).atStartOfDay();
      LocalDateTime mEnd = month.atEndOfMonth().atTime(23, 59, 59);

      int monthlyGain = allRecords.stream()
          .filter(r -> !r.getCreatedAt().isBefore(mStart) && !r.getCreatedAt().isAfter(mEnd))
          .mapToInt(ExpRecord::getAmount).sum();
      runningExp += monthlyGain;

      Map<String, Object> point = new HashMap<>();
      point.put("year", month.getYear());
      point.put("month", month.getMonthValue());
      point.put("label", month.getMonthValue() + "月");
      point.put("monthKey", month.toString());
      point.put("exp", runningExp);
      point.put("monthlyGain", monthlyGain);
      points.add(point);
    }

    // 等级阈值信息，用于前端Y轴设定
    long currentThreshold = LEVEL_THRESHOLDS.getOrDefault(exp.getLevel(), new long[]{0})[0];
    int nextLevel = exp.getLevel() + 1;
    long nextThreshold = LEVEL_THRESHOLDS.containsKey(nextLevel)
        ? LEVEL_THRESHOLDS.get(nextLevel)[0]
        : currentThreshold;

    Map<String, Object> result = new HashMap<>();
    result.put("points", points);
    result.put("currentLevelExp", currentThreshold);
    result.put("nextLevelExp", nextThreshold);
    result.put("level", exp.getLevel());
    result.put("levelName", exp.getLevelName());
    return result;
  }

  /**
   * 获取经验值变动历史
   */
  public List<ExpRecord> getExpHistory(Long userId, int limit) {
    return expRecordMapper.selectList(
        new LambdaQueryWrapper<ExpRecord>()
            .eq(ExpRecord::getUserId, userId)
            .orderByDesc(ExpRecord::getCreatedAt)
            .last("LIMIT " + Math.min(limit, 100)));
  }

  private int calculateLevel(int totalExp) {
    int level = 1;
    for (Map.Entry<Integer, long[]> entry : LEVEL_THRESHOLDS.entrySet()) {
      if (totalExp >= entry.getValue()[0]) {
        level = entry.getKey();
      }
    }
    return level;
  }
}
