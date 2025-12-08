# Inspo-Verse 项目 Sprint 开发计划

> 项目周期：5 个 Sprint (模拟周期)
> 目标：打造基于 Vue 3 全家桶的极致 UI 泛娱乐 AI 社区

## 📅 Sprint 1: 基础设施与视觉基调搭建
**目标**：完成项目脚手架搭建，确立赛博朋克视觉风格，实现响应式布局框架。

*   **P0 (高优先级)**:
    *   [x] 初始化 Vite + Vue 3 + TypeScript 项目环境。
    *   [x] 配置 TailwindCSS，定义 `neon-blue`, `neon-purple` 等自定义原子色系。
    *   [x] 搭建全局路由 (Vue Router 4) 与状态管理 (Pinia) 基础结构。
*   **P1 (中优先级)**:
    *   [x] 开发全局布局组件 `TheHeader` (实现滚动磨砂变色交互) 和 `TheFooter` (异步加载)。
    *   [x] 开发首页 `HomeView`，实现 Banner 轮播与基础入场动画。
*   **UI/UX**:
    *   [x] 引入 `Animate.css` 和 `Lucide Icons` 图标库。
    *   [ ] **(优化项)** 实现 Canvas 动态粒子背景，替代静态 CSS 背景。

## 📅 Sprint 2: 核心认证体系 (Auth System)
**目标**：构建高安全性的用户认证模块，实现前端逻辑闭环。

*   **P0**:
    *   [x] 开发 `LoginForm` 与 `RegisterForm` 组件。
    *   [x] 集成 `VeeValidate` + `Yup`，实现复杂的表单验证 (强密码、手机号)。
    *   [x] 开发 `Captcha` 自定义组件，实现前端图形验证码绘制与校验。
*   **P1**:
    *   [x] 构建 `AuthStore`，处理 JWT Token 的 localStorage 持久化存储。
    *   [x] 实现路由守卫 (`beforeEach`)，拦截未登录访问。
*   **UI/UX**:
    *   [x] 设计 3D 翻转或淡入淡出切换的登录/注册卡片容器。

## 📅 Sprint 3: AI 创意助手核心 (Core Feature)
**目标**：实现类似 ChatGPT 的流式对话体验，完成项目核心价值交付。

*   **P0**:
    *   [x] 搭建双栏布局：左侧历史记录 (`HistoryPanel`)，右侧对话区。
    *   [x] 开发 `MessageList` 组件，集成 `Marked` + `DOMPurify` 实现 Markdown 安全渲染。
    *   [x] 实现 `ChatStore`，模拟 WebSocket 流式消息推送 (打字机效果)。
*   **P1**:
    *   [x] 开发智能输入框 `InputArea`，支持 Shift+Enter 换行与发送状态锁定。
    *   [x] 实现多轮对话上下文管理 (新建、切换、删除会话)。

## 📅 Sprint 4: 用户生态与数据可视化
**目标**：完善用户权益体系，展示 ECharts 数据处理能力。

*   **P0**:
    *   [x] 开发用户中心 `UserCenter`，使用 `Promise.all` 并发加载用户信息与订单。
    *   [x] 实现 `lodash/debounce` 防抖，优化个人信息编辑体验。
    *   [x] 开发会员中心 `VipCenter`，设计 3D 会员卡片。
*   **P1**:
    *   [x] 集成 `ECharts`，绘制会员成长轨迹折线图。
    *   [x] 实现虚拟滚动 (Virtual Scroll) 思想优化长列表渲染。

## 📅 Sprint 5: 后台管理与极致 UI 打磨
**目标**：构建管理员视图，进行全站 UI/UX 升维。

*   **P0**:
    *   [x] 搭建后台布局 `AdminLayout`，实现侧边栏折叠交互。
    *   [x] 开发数据仪表盘 `Dashboard`，实现多维度数据可视化 (折线图 + 饼图)。
*   **P1**:
    *   [x] 完善全站 404 页面与错误处理。
    *   [ ] **(优化项)** 增加全局 "故障风 (Glitch)" 文字特效。
    *   [ ] **(优化项)** 优化按钮交互，增加流光边框效果。
*   **交付**:
    *   [x] 编写 `README.md` 技术文档。
    *   [x] 代码规范检查 (ESLint/Prettier)。
