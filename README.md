# Inspo-Verse (灵感岛) - 泛娱乐 AI 创意社区

> 《前端开发技术》期末考核作品 | 学号: [您的学号] | 姓名: [您的姓名]

## 一、 项目概述

**Inspo-Verse** 是一个集游戏、动漫、创意于一体的泛娱乐社区平台。项目深度融合了 **AI 大模型**技术，为用户提供智能攻略助手与创意灵感生成服务。系统包含前台展示、用户中心、会员体系、AI 对话系统及后台管理系统五大核心模块。

### 技术栈
- **核心框架**: Vue 3 (Composition API) + TypeScript
- **构建工具**: Vite 5
- **路由管理**: Vue Router 4 (路由懒加载、动态路由、导航守卫)
- **状态管理**: Pinia (持久化存储、模块化管理)
- **UI/样式**: TailwindCSS + Animate.css + Lucide Icons (自定义霓虹风格)
- **数据交互**: Axios + ECharts (数据可视化)
- **表单验证**: VeeValidate + Yup (Schema 验证)
- **AI 渲染**: Marked + DOMPurify (Markdown 解析与安全过滤)

---

## 二、 核心功能模块与实现思路

### 1. 首页模块 (Home)
- **组件化架构**: 拆分为 `TheHeader` (透明度动态交互), `TheFooter` (异步加载), `Banner` 等独立组件。
- **响应式布局**: 基于 Flexbox/Grid 实现多端适配。
- **动效体验**: 使用 Vue Transition 实现页面切换动效，集成 Animate.css 实现元素入场动画。

### 2. 认证模块 (Auth)
- **双重校验**: 采用 `vee-validate` + 正则表达式实现用户名(4-16位)、强密码(大小写+特殊符号)校验。
- **安全机制**: 
  - 密码显隐切换。
  - 登录失败 5 次自动触发图形验证码 (`Captcha` 组件)。
  - JWT Token 本地存储与路由拦截。

### 3. AI 创意助手 (AI Chat) - **亮点功能**
- **流式对话**: 通过 Pinia Store 模拟 WebSocket 流式响应，实现"打字机"效果。
- **富文本渲染**: 集成 `marked` 解析 Markdown 语法，使用 `DOMPurify` 过滤 XSS 攻击脚本，支持代码高亮。
- **多轮对话**: 侧边栏 `HistoryPanel` 管理会话历史，支持新建/删除/切换会话上下文。

### 4. 个人与会员中心 (User & VIP)
- **异步并发**: 使用 `Promise.all` 并发请求用户信息与订单列表，减少首屏加载时间。
- **防抖处理**: 编辑资料时使用 `lodash/debounce` 优化输入验证性能。
- **数据可视化**: 集成 ECharts 绘制会员成长轨迹图，展示用户消费趋势。
- **敏感操作**: 修改核心信息需二次确认，保障账户安全。

### 5. 后台管理系统 (Admin)
- **嵌套路由**: `/admin` 下设子路由，配合 `keep-alive` 实现组件缓存。
- **权限控制**: 基于路由元信息 (`meta.requiresAuth`) 的全局守卫拦截。
- **仪表盘**: 封装 ECharts 组件，展示用户增长与收入构成图表，支持窗口 resize 自适应。

---

## 三、 项目运行指南

### 1. 环境准备
确保本地安装 Node.js (v16+)

### 2. 安装依赖
```bash
npm install
```

### 3. 启动开发服务器
```bash
npm run dev
```
访问 http://localhost:5173 即可预览。

### 4. 构建生产版本
```bash
npm run build
```

---

## 四、 开发心得与反思

本项目在开发过程中，重点解决了组件状态共享与跨组件通信的难题。通过引入 Pinia，将 Auth 和 Chat 状态全局化，极大简化了逻辑。在 AI 模块开发中，深入理解了 Vue 的响应式原理与 DOM 更新机制（如自动滚动到底部）。虽然目前 AI 接口为模拟实现，但架构设计已预留了对接真实 LLM API 的接口层，具备良好的扩展性。

---

## 五、 企业级后端与部署方案

已补充完整的生产级全栈落地文档（含架构链路、MySQL DDL、API 规范、SSE 流式交互、Docker/Nginx 与安全上线清单）：

- [/docs/enterprise-deployment-blueprint.md](/docs/enterprise-deployment-blueprint.md)
