import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '灵感岛 - 首页' }
    },
    {
      path: '/explore',
      name: 'explore',
      component: () => import('../views/ExploreView.vue'),
      meta: { title: '发现 - Inspo-Verse' }
    },
    {
      path: '/forum',
      name: 'forum',
      component: () => import('../views/ForumView.vue'),
      meta: { title: '论坛 - Inspo-Verse' }
    },
    {
      path: '/forum/post/:id',
      name: 'forum-post',
      component: () => import('../views/ForumPostDetailView.vue'),
      meta: { title: '帖子详情 - Inspo-Verse' }
    },
    // --- 新增 6 个极致页面 ---
    {
      path: '/games',
      name: 'games',
      component: () => import('../views/GamesView.vue'),
      meta: { title: '热门游戏 - Inspo-Verse' }
    },
    {
      path: '/anime',
      name: 'anime',
      component: () => import('../views/AnimeView.vue'),
      meta: { title: '动漫番剧 - Inspo-Verse' }
    },
    {
      path: '/workshop',
      name: 'workshop',
      component: () => import('../views/WorkshopView.vue'),
      meta: { title: '创意工坊 - Inspo-Verse' }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: { title: '关于我们 - Inspo-Verse' }
    },
    {
      path: '/rules',
      name: 'rules',
      component: () => import('../views/CommunityRulesView.vue'),
      meta: { title: '社区规范 - Inspo-Verse' }
    },
    {
      path: '/contact',
      name: 'contact',
      component: () => import('../views/ContactView.vue'),
      meta: { title: '联系客服 - Inspo-Verse' }
    },
    // ----------------------
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录 - 灵感岛' }
    },
    {
      path: '/ai-chat',
      name: 'ai-chat',
      component: () => import('../views/AIChatView.vue'),
      meta: { title: 'AI 创意助手 - Inspo-Verse', requiresAuth: true }
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/UserCenterView.vue'),
      meta: { title: '个人中心 - Inspo-Verse', requiresAuth: true }
    },
    {
      path: '/vip',
      name: 'vip',
      component: () => import('../views/VipCenterView.vue'),
      meta: { title: '会员中心 - Inspo-Verse', requiresAuth: true }
    },
    {
      path: '/oauth/:provider/callback',
      name: 'oauth-callback',
      component: () => import('../views/OAuthCallbackView.vue'),
      meta: { title: 'OAuth 授权回调', requiresAuth: true }
    },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: '数据仪表盘' }
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/UserManagement.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: () => import('../views/admin/PostManagement.vue'),
          meta: { title: '帖子管理' }
        },
        {
          path: 'explore',
          name: 'admin-explore',
          component: () => import('../views/admin/ExploreManagement.vue'),
          meta: { title: '内容推荐管理' }
        },
        {
          path: 'ai-monitor',
          name: 'admin-ai',
          component: () => import('../views/admin/AIMonitor.vue'),
          meta: { title: 'AI 服务监控' }
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('../views/admin/OrderManagement.vue'),
          meta: { title: '订单管理' }
        },
        {
          path: 'anime',
          name: 'admin-anime',
          component: () => import('../views/admin/AnimeManagement.vue'),
          meta: { title: '番剧管理' }
        },
        {
          path: 'games',
          name: 'admin-games',
          component: () => import('../views/admin/GameManagement.vue'),
          meta: { title: '游戏管理' }
        },
        {
          path: 'workshop',
          name: 'admin-workshop',
          component: () => import('../views/admin/WorkshopManagement.vue'),
          meta: { title: '工坊管理' }
        },
        {
          path: 'vip',
          name: 'admin-vip',
          component: () => import('../views/admin/VipManagement.vue'),
          meta: { title: 'VIP管理' }
        },
        {
          path: 'settings',
          name: 'admin-settings',
          component: () => import('../views/admin/SystemSettings.vue'),
          meta: { title: '系统设置' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '404 - Lost in Void' }
    }
  ]
})

// 不需要登录即可访问的白名单路由
const PUBLIC_ROUTES = ['home', 'login', 'not-found']

router.beforeEach((to, _from, next) => {
  // 设置标题
  document.title = (to.meta.title as string) || 'Inspo-Verse'
  
  const token = localStorage.getItem('token')
  // 非白名单页面，未登录时重定向到登录页
  if (!PUBLIC_ROUTES.includes(to.name as string) && !token) {
    next('/login')
    return
  }

  // 管理员权限校验
  if (to.matched.some(record => record.meta.requiresAdmin)) {
    const userInfoStr = localStorage.getItem('user_info')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (!userInfo.isAdmin) {
          next('/')
          return
        }
      } catch {
        next('/login')
        return
      }
    } else {
      next('/login')
      return
    }
  }

  next()
})

export default router
