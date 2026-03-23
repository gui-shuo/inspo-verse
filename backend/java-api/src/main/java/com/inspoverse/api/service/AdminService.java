package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.*;
import com.inspoverse.api.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final ForumPostMapper forumPostMapper;
    private final ForumCommentMapper forumCommentMapper;
    private final ContentItemMapper contentItemMapper;
    private final ContentCommentMapper contentCommentMapper;
    private final AIChatSessionMapper aiChatSessionMapper;
    private final AIChatMessageMapper aiChatMessageMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final VipOrderMapper vipOrderMapper;
    private final VipPlanMapper vipPlanMapper;
    private final VipMembershipMapper vipMembershipMapper;
    private final AnimeSeriesMapper animeSeriesMapper;
    private final AnimeOrderMapper animeOrderMapper;
    private final GameMapper gameMapper;
    private final GameOrderMapper gameOrderMapper;
    private final WorkshopProjectMapper workshopProjectMapper;
    private final SysConfigMapper sysConfigMapper;
    private final OperationLogMapper operationLogMapper;
    private final EmailSubscriptionMapper emailSubscriptionMapper;
    private final UserPointsMapper userPointsMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final DailyTaskMapper dailyTaskMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_USERS_KEY = "online:users";
    private volatile boolean redisAvailable = true;

    // ========================= 权限校验 =========================

    /**
     * 校验用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, "ROLE_ADMIN")
                .eq(Role::getIsDeleted, 0));
        if (adminRole == null) return false;

        Long count = userRoleRelMapper.selectCount(new LambdaQueryWrapper<UserRoleRel>()
                .eq(UserRoleRel::getUserId, userId)
                .eq(UserRoleRel::getRoleId, adminRole.getId()));
        return count > 0;
    }

    /**
     * 要求管理员权限
     */
    public void requireAdmin(Long userId) {
        if (!isAdmin(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }

    /**
     * 获取用户角色列表
     */
    public List<String> getUserRoles(Long userId) {
        List<UserRoleRel> rels = userRoleRelMapper.selectList(
                new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId));
        if (rels.isEmpty()) return List.of("ROLE_USER");

        List<Long> roleIds = rels.stream().map(UserRoleRel::getRoleId).toList();
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getId, roleIds)
                .eq(Role::getIsDeleted, 0));
        return roles.stream().map(Role::getRoleCode).toList();
    }

    // ========================= 仪表盘 =========================

    /**
     * 获取仪表盘统计数据
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总用户数
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0));
        stats.put("totalUsers", totalUsers);

        // 今日新增用户
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreatedAt, todayStart));
        stats.put("todayNewUsers", todayUsers);

        // 在线用户
        long onlineCount = 0;
        try {
            Double now = (double) (System.currentTimeMillis() / 1000);
            Long count = redisTemplate.opsForZSet().count(ONLINE_USERS_KEY, now - 300, now);
            if (count != null) onlineCount = count;
            redisAvailable = true;
        } catch (Exception e) {
            if (redisAvailable) {
                redisAvailable = false;
                log.warn("Redis不可用，在线用户降级为0");
            }
        }
        stats.put("onlineUsers", onlineCount);

        // 总帖子数
        Long totalPosts = forumPostMapper.selectCount(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getIsDeleted, 0));
        stats.put("totalPosts", totalPosts);

        // 总内容数（发现）
        Long totalContent = contentItemMapper.selectCount(new LambdaQueryWrapper<ContentItem>().eq(ContentItem::getIsDeleted, 0));
        stats.put("totalContent", totalContent);

        // AI会话总数
        Long totalAiSessions = aiChatSessionMapper.selectCount(new LambdaQueryWrapper<AIChatSession>().eq(AIChatSession::getIsDeleted, 0));
        stats.put("totalAiSessions", totalAiSessions);

        // 今日AI消息数
        Long todayAiMessages = aiChatMessageMapper.selectCount(new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getIsDeleted, 0)
                .ge(AIChatMessage::getCreatedAt, todayStart));
        stats.put("todayAiMessages", todayAiMessages);

        // 今日token消耗
        List<AIChatMessage> todayMessages = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getIsDeleted, 0)
                .ge(AIChatMessage::getCreatedAt, todayStart)
                .select(AIChatMessage::getTokens));
        int todayTokens = todayMessages.stream().mapToInt(m -> m.getTokens() != null ? m.getTokens() : 0).sum();
        stats.put("todayTokenUsage", todayTokens);

        // 支付订单统计
        Long totalPaidOrders = paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, "PAID"));
        stats.put("totalPaidOrders", totalPaidOrders);

        // 今日支付金额（分→元）
        List<PaymentOrder> todayPaidOrders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, "PAID")
                .ge(PaymentOrder::getPaidAt, todayStart)
                .select(PaymentOrder::getAmount));
        BigDecimal todayRevenue = todayPaidOrders.stream()
                .map(PaymentOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);

        // VIP会员总数
        Long totalVip = vipMembershipMapper.selectCount(new LambdaQueryWrapper<VipMembership>()
                .eq(VipMembership::getStatus, 1)
                .gt(VipMembership::getEndTime, LocalDateTime.now()));
        stats.put("totalActiveVip", totalVip);

        // 总番剧数
        Long totalAnime = animeSeriesMapper.selectCount(new LambdaQueryWrapper<AnimeSeries>().eq(AnimeSeries::getIsDeleted, 0));
        stats.put("totalAnime", totalAnime);

        // 总游戏数
        Long totalGames = gameMapper.selectCount(new LambdaQueryWrapper<Game>().eq(Game::getIsDeleted, 0));
        stats.put("totalGames", totalGames);

        // 总工坊项目数
        Long totalWorkshop = workshopProjectMapper.selectCount(new LambdaQueryWrapper<WorkshopProject>().eq(WorkshopProject::getIsDeleted, 0));
        stats.put("totalWorkshopProjects", totalWorkshop);

        return stats;
    }

    /**
     * 获取近7天用户增长趋势
     */
    public List<Map<String, Object>> getUserGrowthTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            Long newUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getIsDeleted, 0)
                    .ge(User::getCreatedAt, start)
                    .lt(User::getCreatedAt, end));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.format(fmt));
            point.put("newUsers", newUsers);
            result.add(point);
        }
        return result;
    }

    /**
     * 获取收入构成
     */
    public List<Map<String, Object>> getRevenueComposition() {
        List<Map<String, Object>> result = new ArrayList<>();

        // VIP订单
        List<PaymentOrder> vipOrders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, "PAID").eq(PaymentOrder::getBizType, "VIP"));
        BigDecimal vipTotal = vipOrders.stream().map(PaymentOrder::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.add(Map.of("name", "VIP会员", "value", vipTotal));

        // 充值订单
        List<PaymentOrder> rechargeOrders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, "PAID").eq(PaymentOrder::getBizType, "RECHARGE"));
        BigDecimal rechargeTotal = rechargeOrders.stream().map(PaymentOrder::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.add(Map.of("name", "积分充值", "value", rechargeTotal));

        // 番剧订单
        List<AnimeOrder> animeOrders = animeOrderMapper.selectList(new LambdaQueryWrapper<AnimeOrder>()
                .eq(AnimeOrder::getStatus, "PAID"));
        int animeTotal = animeOrders.stream().mapToInt(o -> o.getAmountCents() != null ? o.getAmountCents() : 0).sum();
        result.add(Map.of("name", "番剧付费", "value", new BigDecimal(animeTotal).divide(new BigDecimal(100))));

        // 游戏订单
        List<GameOrder> gameOrders = gameOrderMapper.selectList(new LambdaQueryWrapper<GameOrder>()
                .eq(GameOrder::getStatus, "PAID"));
        int gameTotal = gameOrders.stream().mapToInt(o -> o.getAmountCents() != null ? o.getAmountCents() : 0).sum();
        result.add(Map.of("name", "游戏购买", "value", new BigDecimal(gameTotal).divide(new BigDecimal(100))));

        return result;
    }

    // ========================= 用户管理 =========================

    /**
     * 分页查询用户列表
     */
    public Page<Map<String, Object>> getUserList(int page, int size, String keyword, String status, String role) {
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(User::getStatus, "active".equals(status) ? 1 : 0);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(userPage, wrapper);

        // 转换为前端需要的格式
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", u.getId());
            vo.put("username", u.getUsername());
            vo.put("nickname", u.getNickname());
            vo.put("email", u.getEmail());
            vo.put("phone", u.getPhone());
            vo.put("avatarUrl", u.getAvatarUrl());
            vo.put("status", u.getStatus() == 1 ? "active" : "banned");
            vo.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            vo.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : "");

            // 获取角色
            List<String> roles = getUserRoles(u.getId());
            String roleStr = "user";
            if (roles.contains("ROLE_ADMIN")) roleStr = "admin";
            else if (roles.contains("ROLE_VIP")) roleStr = "vip";
            vo.put("role", roleStr);

            records.add(vo);
        }

        // 按角色过滤（在查询后过滤，因为角色在关系表中）
        if (role != null && !role.isBlank()) {
            records = records.stream().filter(r -> role.equals(r.get("role"))).collect(Collectors.toList());
        }

        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 封禁/解封用户
     */
    @Transactional
    public void toggleUserStatus(Long adminUserId, Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        int newStatus = user.getStatus() == 1 ? 0 : 1;
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员[{}]{}用户[{}]", adminUserId, newStatus == 1 ? "解封" : "封禁", targetUserId);
    }

    /**
     * 修改用户角色
     */
    @Transactional
    public void updateUserRole(Long adminUserId, Long targetUserId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode).eq(Role::getIsDeleted, 0));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 删除旧角色关系
        userRoleRelMapper.delete(new LambdaQueryWrapper<UserRoleRel>()
                .eq(UserRoleRel::getUserId, targetUserId));

        // 插入新角色
        UserRoleRel rel = new UserRoleRel();
        rel.setUserId(targetUserId);
        rel.setRoleId(role.getId());
        rel.setCreatedAt(LocalDateTime.now());
        userRoleRelMapper.insert(rel);

        log.info("管理员[{}]设置用户[{}]角色为[{}]", adminUserId, targetUserId, roleCode);
    }

    /**
     * 重置用户密码
     */
    @Transactional
    public void resetUserPassword(Long adminUserId, Long targetUserId, String newPassword) {
        User user = userMapper.selectById(targetUserId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        user.setPasswordHash(encoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员[{}]重置了用户[{}]的密码", adminUserId, targetUserId);
    }

    // ========================= 帖子管理 =========================

    /**
     * 分页查询帖子列表
     */
    public Page<Map<String, Object>> getPostList(int page, int size, String keyword, String category, String status) {
        Page<ForumPost> postPage = new Page<>(page, size);
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getIsDeleted, 0);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(ForumPost::getTitle, keyword);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(ForumPost::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ForumPost::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(ForumPost::getIsTop).orderByDesc(ForumPost::getCreatedAt);

        Page<ForumPost> result = forumPostMapper.selectPage(postPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (ForumPost p : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", p.getId());
            vo.put("postNo", p.getPostNo());
            vo.put("title", p.getTitle());
            vo.put("category", p.getCategory());
            vo.put("viewCount", p.getViewCount());
            vo.put("likeCount", p.getLikeCount());
            vo.put("commentCount", p.getCommentCount());
            vo.put("isTop", p.getIsTop());
            vo.put("isEssence", p.getIsEssence());
            vo.put("status", p.getStatus());
            vo.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            // 获取作者信息
            User author = userMapper.selectById(p.getUserId());
            vo.put("authorName", author != null ? author.getNickname() : "已注销");
            vo.put("authorId", p.getUserId());
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 置顶/取消置顶帖子
     */
    @Transactional
    public void togglePostTop(Long adminUserId, Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null || post.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        post.setIsTop(post.getIsTop() == 1 ? 0 : 1);
        post.setUpdatedAt(LocalDateTime.now());
        forumPostMapper.updateById(post);
    }

    /**
     * 设置/取消精华帖
     */
    @Transactional
    public void togglePostEssence(Long adminUserId, Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null || post.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        post.setIsEssence(post.getIsEssence() == 1 ? 0 : 1);
        post.setUpdatedAt(LocalDateTime.now());
        forumPostMapper.updateById(post);
    }

    /**
     * 删除帖子（逻辑删除）
     */
    @Transactional
    public void deletePost(Long adminUserId, Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        forumPostMapper.deleteById(postId); // MyBatis-Plus逻辑删除
        log.info("管理员[{}]删除帖子[{}]", adminUserId, postId);
    }

    /**
     * 修改帖子状态
     */
    @Transactional
    public void updatePostStatus(Long adminUserId, Long postId, int status) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null || post.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        post.setStatus(status);
        post.setUpdatedAt(LocalDateTime.now());
        forumPostMapper.updateById(post);
    }

    // ========================= 订单管理 =========================

    /**
     * 分页查询所有支付订单
     */
    public Page<Map<String, Object>> getOrderList(int page, int size, String keyword, String status, String bizType) {
        Page<PaymentOrder> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(PaymentOrder::getOrderNo, keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(PaymentOrder::getStatus, status);
        }
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(PaymentOrder::getBizType, bizType);
        }
        wrapper.orderByDesc(PaymentOrder::getCreatedAt);

        Page<PaymentOrder> result = paymentOrderMapper.selectPage(orderPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder o : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", o.getId());
            vo.put("orderNo", o.getOrderNo());
            vo.put("userId", o.getUserId());
            vo.put("amount", o.getAmount());
            vo.put("points", o.getPoints());
            vo.put("payMethod", o.getPayMethod());
            vo.put("status", o.getStatus());
            vo.put("bizType", o.getBizType());
            vo.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            vo.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : "");
            // 获取用户名
            User user = userMapper.selectById(o.getUserId());
            vo.put("username", user != null ? user.getNickname() : "未知");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    // ========================= AI 监控 =========================

    /**
     * 获取AI使用统计
     */
    public Map<String, Object> getAiStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 今日token消耗
        List<AIChatMessage> todayMessages = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getIsDeleted, 0)
                .ge(AIChatMessage::getCreatedAt, todayStart)
                .select(AIChatMessage::getTokens, AIChatMessage::getSafetyFlag, AIChatMessage::getRole));
        int todayTokens = todayMessages.stream().mapToInt(m -> m.getTokens() != null ? m.getTokens() : 0).sum();
        stats.put("todayTokenUsage", todayTokens);

        // 今日请求数
        long todayRequests = todayMessages.stream().filter(m -> "user".equals(m.getRole())).count();
        stats.put("todayRequests", todayRequests);

        // 成功率
        long totalMsg = todayMessages.size();
        long blockedMsg = todayMessages.stream().filter(m -> m.getSafetyFlag() != null && m.getSafetyFlag() == 1).count();
        double successRate = totalMsg > 0 ? ((totalMsg - blockedMsg) * 100.0 / totalMsg) : 100.0;
        stats.put("successRate", Math.round(successRate * 10) / 10.0);
        stats.put("blockedCount", blockedMsg);

        // 总会话数
        Long totalSessions = aiChatSessionMapper.selectCount(new LambdaQueryWrapper<AIChatSession>()
                .eq(AIChatSession::getIsDeleted, 0));
        stats.put("totalSessions", totalSessions);

        return stats;
    }

    /**
     * 获取AI消息日志（分页）
     */
    public Page<Map<String, Object>> getAiMessageLogs(int page, int size, String safetyFilter) {
        Page<AIChatMessage> msgPage = new Page<>(page, size);
        LambdaQueryWrapper<AIChatMessage> wrapper = new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getIsDeleted, 0)
                .eq(AIChatMessage::getRole, "user"); // 只显示用户消息

        if ("blocked".equals(safetyFilter)) {
            wrapper.eq(AIChatMessage::getSafetyFlag, 1);
        }
        wrapper.orderByDesc(AIChatMessage::getCreatedAt);

        Page<AIChatMessage> result = aiChatMessageMapper.selectPage(msgPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (AIChatMessage m : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", m.getId());
            vo.put("userId", m.getUserId());
            vo.put("content", m.getContent() != null && m.getContent().length() > 100
                    ? m.getContent().substring(0, 100) + "..." : m.getContent());
            vo.put("tokens", m.getTokens());
            vo.put("safetyFlag", m.getSafetyFlag());
            vo.put("latencyMs", m.getLatencyMs());
            vo.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");

            // 获取会话信息
            AIChatSession session = aiChatSessionMapper.selectById(m.getSessionId());
            vo.put("modelName", session != null ? session.getModelName() : "unknown");

            // 获取用户名
            User user = userMapper.selectById(m.getUserId());
            vo.put("username", user != null ? user.getNickname() : "未知");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    // ========================= 内容管理（发现） =========================

    /**
     * 分页查询发现内容
     */
    public Page<Map<String, Object>> getContentList(int page, int size, String keyword, String category, String status) {
        Page<ContentItem> contentPage = new Page<>(page, size);
        LambdaQueryWrapper<ContentItem> wrapper = new LambdaQueryWrapper<ContentItem>()
                .eq(ContentItem::getIsDeleted, 0);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(ContentItem::getTitle, keyword);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(ContentItem::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ContentItem::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(ContentItem::getCreatedAt);

        Page<ContentItem> result = contentItemMapper.selectPage(contentPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (ContentItem c : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", c.getId());
            vo.put("contentNo", c.getContentNo());
            vo.put("title", c.getTitle());
            vo.put("category", c.getCategory());
            vo.put("coverUrl", c.getCoverUrl());
            vo.put("likeCount", c.getLikeCount());
            vo.put("commentCount", c.getCommentCount());
            vo.put("viewCount", c.getViewCount());
            vo.put("status", c.getStatus());
            vo.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");

            User author = userMapper.selectById(c.getUserId());
            vo.put("authorName", author != null ? author.getNickname() : "已注销");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 修改内容状态
     */
    @Transactional
    public void updateContentStatus(Long adminUserId, Long contentId, int status) {
        ContentItem item = contentItemMapper.selectById(contentId);
        if (item == null || item.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
        }
        item.setStatus(status);
        item.setUpdatedAt(LocalDateTime.now());
        contentItemMapper.updateById(item);
    }

    /**
     * 删除内容
     */
    @Transactional
    public void deleteContent(Long adminUserId, Long contentId) {
        contentItemMapper.deleteById(contentId);
        log.info("管理员[{}]删除内容[{}]", adminUserId, contentId);
    }

    // ========================= 番剧管理 =========================

    public Page<Map<String, Object>> getAnimeList(int page, int size, String keyword, String status) {
        Page<AnimeSeries> animePage = new Page<>(page, size);
        LambdaQueryWrapper<AnimeSeries> wrapper = new LambdaQueryWrapper<AnimeSeries>()
                .eq(AnimeSeries::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AnimeSeries::getTitle, keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AnimeSeries::getStatus, status);
        }
        wrapper.orderByDesc(AnimeSeries::getCreatedAt);

        Page<AnimeSeries> result = animeSeriesMapper.selectPage(animePage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (AnimeSeries a : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", a.getId());
            vo.put("seriesNo", a.getSeriesNo());
            vo.put("title", a.getTitle());
            vo.put("coverUrl", a.getCoverUrl());
            vo.put("score", a.getScore());
            vo.put("status", a.getStatus());
            vo.put("currentEpisode", a.getCurrentEpisode());
            vo.put("totalEpisodes", a.getTotalEpisodes());
            vo.put("viewCount", a.getViewCount());
            vo.put("subscribeCount", a.getSubscribeCount());
            vo.put("isPaid", a.getIsPaid());
            vo.put("scheduleDay", a.getScheduleDay());
            vo.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional
    public void updateAnimeStatus(Long adminUserId, Long animeId, String status) {
        AnimeSeries anime = animeSeriesMapper.selectById(animeId);
        if (anime == null || anime.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
        }
        anime.setStatus(status);
        anime.setUpdatedAt(LocalDateTime.now());
        animeSeriesMapper.updateById(anime);
    }

    @Transactional
    public void deleteAnime(Long adminUserId, Long animeId) {
        animeSeriesMapper.deleteById(animeId);
        log.info("管理员[{}]删除番剧[{}]", adminUserId, animeId);
    }

    // ========================= 游戏管理 =========================

    public Page<Map<String, Object>> getGameList(int page, int size, String keyword, String genre) {
        Page<Game> gamePage = new Page<>(page, size);
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<Game>().eq(Game::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Game::getTitle, keyword);
        }
        if (genre != null && !genre.isBlank()) {
            wrapper.eq(Game::getGenre, genre);
        }
        wrapper.orderByDesc(Game::getCreatedAt);

        Page<Game> result = gameMapper.selectPage(gamePage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Game g : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", g.getId());
            vo.put("gameNo", g.getGameNo());
            vo.put("title", g.getTitle());
            vo.put("genre", g.getGenre());
            vo.put("coverUrl", g.getCoverUrl());
            vo.put("rating", g.getRating());
            vo.put("playCount", g.getPlayCount());
            vo.put("favoriteCount", g.getFavoriteCount());
            vo.put("status", g.getStatus());
            vo.put("isPaid", g.getIsPaid());
            vo.put("developer", g.getDeveloper());
            vo.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional
    public void updateGameStatus(Long adminUserId, Long gameId, int status) {
        Game game = gameMapper.selectById(gameId);
        if (game == null || game.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
        }
        game.setStatus(status);
        game.setUpdatedAt(LocalDateTime.now());
        gameMapper.updateById(game);
    }

    @Transactional
    public void deleteGame(Long adminUserId, Long gameId) {
        gameMapper.deleteById(gameId);
        log.info("管理员[{}]删除游戏[{}]", adminUserId, gameId);
    }

    // ========================= 工坊管理 =========================

    public Page<Map<String, Object>> getWorkshopList(int page, int size, String keyword, String category) {
        Page<WorkshopProject> wpPage = new Page<>(page, size);
        LambdaQueryWrapper<WorkshopProject> wrapper = new LambdaQueryWrapper<WorkshopProject>()
                .eq(WorkshopProject::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(WorkshopProject::getTitle, keyword);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(WorkshopProject::getCategory, category);
        }
        wrapper.orderByDesc(WorkshopProject::getCreatedAt);

        Page<WorkshopProject> result = workshopProjectMapper.selectPage(wpPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (WorkshopProject w : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", w.getId());
            vo.put("projectNo", w.getProjectNo());
            vo.put("title", w.getTitle());
            vo.put("category", w.getCategory());
            vo.put("coverUrl", w.getCoverUrl());
            vo.put("downloadCount", w.getDownloadCount());
            vo.put("likeCount", w.getLikeCount());
            vo.put("status", w.getStatus());
            vo.put("visibility", w.getVisibility());
            double avgRating = w.getRatingCount() > 0 ? (w.getRatingSum() * 1.0 / w.getRatingCount() / 10.0) : 0;
            vo.put("rating", Math.round(avgRating * 10) / 10.0);
            vo.put("createdAt", w.getCreatedAt() != null ? w.getCreatedAt().toString() : "");

            User author = userMapper.selectById(w.getUserId());
            vo.put("authorName", author != null ? author.getNickname() : "已注销");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional
    public void updateWorkshopStatus(Long adminUserId, Long projectId, int status) {
        WorkshopProject wp = workshopProjectMapper.selectById(projectId);
        if (wp == null || wp.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工坊项目不存在");
        }
        wp.setStatus(status);
        wp.setUpdatedAt(LocalDateTime.now());
        workshopProjectMapper.updateById(wp);
    }

    @Transactional
    public void deleteWorkshopProject(Long adminUserId, Long projectId) {
        workshopProjectMapper.deleteById(projectId);
        log.info("管理员[{}]删除工坊项目[{}]", adminUserId, projectId);
    }

    // ========================= 系统配置 =========================

    /**
     * 获取所有系统配置
     */
    public List<Map<String, Object>> getAllConfigs() {
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getIsDeleted, 0));
        return configs.stream().map(c -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", c.getId());
            vo.put("configKey", c.getConfigKey());
            vo.put("configValue", c.getConfigValue());
            vo.put("valueType", c.getValueType());
            vo.put("remark", c.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新或创建系统配置
     */
    @Transactional
    public void upsertConfig(String key, String value, String valueType, String remark) {
        SysConfig existing = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).eq(SysConfig::getIsDeleted, 0));

        if (existing != null) {
            existing.setConfigValue(value);
            if (valueType != null) existing.setValueType(valueType);
            if (remark != null) existing.setRemark(remark);
            existing.setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.updateById(existing);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setValueType(valueType != null ? valueType : "string");
            config.setRemark(remark);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setIsDeleted(0);
            sysConfigMapper.insert(config);
        }
    }

    /**
     * 获取单个配置值
     */
    public String getConfigValue(String key) {
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).eq(SysConfig::getIsDeleted, 0));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            upsertConfig(entry.getKey(), entry.getValue(), "string", null);
        }
    }

    // ========================= VIP管理 =========================

    public Page<Map<String, Object>> getVipMemberList(int page, int size) {
        Page<VipMembership> memberPage = new Page<>(page, size);
        LambdaQueryWrapper<VipMembership> wrapper = new LambdaQueryWrapper<VipMembership>()
                .eq(VipMembership::getIsDeleted, 0)
                .orderByDesc(VipMembership::getCreatedAt);

        Page<VipMembership> result = vipMembershipMapper.selectPage(memberPage, wrapper);
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (VipMembership v : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", v.getId());
            vo.put("userId", v.getUserId());
            vo.put("vipLevel", v.getVipLevel());
            vo.put("startTime", v.getStartTime() != null ? v.getStartTime().toString() : "");
            vo.put("endTime", v.getEndTime() != null ? v.getEndTime().toString() : "");
            vo.put("status", v.getStatus());
            vo.put("sourceOrderNo", v.getSourceOrderNo());

            User user = userMapper.selectById(v.getUserId());
            vo.put("username", user != null ? user.getNickname() : "未知");
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    public List<Map<String, Object>> getVipPlans() {
        List<VipPlan> plans = vipPlanMapper.selectList(new LambdaQueryWrapper<VipPlan>()
                .eq(VipPlan::getIsDeleted, 0).orderByAsc(VipPlan::getLevel));
        return plans.stream().map(p -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", p.getId());
            vo.put("planCode", p.getPlanCode());
            vo.put("planName", p.getPlanName());
            vo.put("priceCents", p.getPriceCents());
            vo.put("durationDays", p.getDurationDays());
            vo.put("level", p.getLevel());
            vo.put("status", p.getStatus());
            return vo;
        }).toList();
    }

    @Transactional
    public void updateVipPlan(Long planId, String planName, Integer priceCents, Integer durationDays, Integer status) {
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null || plan.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "VIP方案不存在");
        }
        if (planName != null) plan.setPlanName(planName);
        if (priceCents != null) plan.setPriceCents(priceCents);
        if (durationDays != null) plan.setDurationDays(durationDays);
        if (status != null) plan.setStatus(status);
        plan.setUpdatedAt(LocalDateTime.now());
        vipPlanMapper.updateById(plan);
    }

    // ========================= 每日任务管理 =========================

    public List<Map<String, Object>> getDailyTasks() {
        List<DailyTask> tasks = dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>().orderByAsc(DailyTask::getSortOrder));
        return tasks.stream().map(t -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", t.getId());
            vo.put("taskCode", t.getTaskCode());
            vo.put("taskName", t.getTaskName());
            vo.put("description", t.getDescription());
            vo.put("rewardPoints", t.getRewardPoints());
            vo.put("rewardExp", t.getRewardExp());
            vo.put("dailyLimit", t.getDailyLimit());
            vo.put("taskType", t.getTaskType());
            vo.put("status", t.getStatus());
            return vo;
        }).toList();
    }

    @Transactional
    public void updateDailyTask(Long taskId, String taskName, String description,
                                 Integer rewardPoints, Integer rewardExp, Integer dailyLimit, Integer status) {
        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (taskName != null) task.setTaskName(taskName);
        if (description != null) task.setDescription(description);
        if (rewardPoints != null) task.setRewardPoints(rewardPoints);
        if (rewardExp != null) task.setRewardExp(rewardExp);
        if (dailyLimit != null) task.setDailyLimit(dailyLimit);
        if (status != null) task.setStatus(status);
        dailyTaskMapper.updateById(task);
    }

    // ========================= 邮件订阅管理 =========================

    public Page<Map<String, Object>> getEmailSubscriptions(int page, int size) {
        Page<EmailSubscription> subPage = new Page<>(page, size);
        Page<EmailSubscription> result = emailSubscriptionMapper.selectPage(subPage,
                new LambdaQueryWrapper<EmailSubscription>().orderByDesc(EmailSubscription::getCreatedAt));
        Page<Map<String, Object>> voPage = new Page<>(page, size, result.getTotal());

        List<Map<String, Object>> records = result.getRecords().stream().map(s -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", s.getId());
            vo.put("email", s.getEmail());
            vo.put("status", s.getStatus());
            vo.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            return vo;
        }).toList();
        voPage.setRecords(records);
        return voPage;
    }
}
