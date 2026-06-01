# Auth Modal Login Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将用户端独立登录页改造为站内弹框式认证中心，并补齐短信登录、注册、找回密码、微信扫码登录的前后端基础闭环。

**Architecture:** 前端以 `AuthProvider + AuthModal` 管理弹框状态、用户态和 token 持久化；后端在现有 Spring Boot `auth` 模块内扩展内存态认证服务，提供短信验证码、注册、密码登录、找回密码、微信二维码轮询等 REST 接口。首期不接真实短信与微信，只保留可落地的接口边界、时效约束、冷却时间、日志和错误模型。

**Tech Stack:** React 18 + TypeScript + Vite，Spring Boot 2.7 + Java 8，SLF4J + Logback，现有 ApiResponse / traceId / CORS 体系。

---

### Task 1: 认证流文档与接口边界

**Files:**
- Modify: `docs/plans/2026-04-16-auth-modal-login.md`
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/auth/controller/AuthController.java`

**Step 1: 明确前端弹框场景**

- 登录入口：头部 `登录`、`注册`
- 弹框页签：短信登录、账号密码、微信扫码
- 辅助流程：注册、找回密码、微信未绑定手机号

**Step 2: 明确后端接口**

- `POST /api/auth/sms-code`
- `POST /api/auth/sms-login`
- `POST /api/auth/password-login`
- `POST /api/auth/register/verify-code`
- `POST /api/auth/register/complete`
- `POST /api/auth/password-reset/verify-code`
- `POST /api/auth/password-reset/complete`
- `POST /api/auth/wechat/qr-code`
- `POST /api/auth/wechat/poll`
- `POST /api/auth/wechat/bind-phone`

### Task 2: 后端认证内存域模型

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthService.java`

**Step 1: 增加内存态存储**

- 用户表：手机号、密码、昵称、是否实名认证、微信 openId
- 验证码表：手机号、场景、验证码、过期时间、冷却截止时间
- 注册会话：手机号、校验通过时间
- 找回密码会话：手机号、校验通过时间
- 微信二维码会话：sceneId、二维码地址、过期时间、状态、openId

**Step 2: 增加业务规则**

- 手机号格式校验
- 同手机号 60 秒冷却
- 验证码 15 分钟有效
- 注册手机号唯一
- 密码 6-18 位且字母数字组合
- 登录成功返回 token、过期天数、用户信息

**Step 3: 增加日志**

- 发码开始 / 成功
- 验证码校验成功 / 失败
- 注册完成
- 密码登录成功
- 找回密码完成
- 微信二维码创建与轮询结果

### Task 3: 后端接口扩展

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/auth/controller/AuthController.java`

**Step 1: 补齐请求 DTO**

- 发验证码
- 短信登录
- 密码登录
- 注册校验 / 完成
- 找回密码校验 / 完成
- 微信二维码创建 / 轮询 / 绑手机

**Step 2: 接入统一响应**

- 所有接口继续走 `ApiResponse.success(...)`
- 使用 `MDC.get("traceId")` 透传 traceId

### Task 4: 前端认证状态层

**Files:**
- Create: `apps/web/src/auth/auth-api.ts`
- Create: `apps/web/src/auth/auth-storage.ts`
- Create: `apps/web/src/auth/auth-context.tsx`

**Step 1: API 封装**

- 封装 fetch 请求
- 统一处理 loading / error / data
- 处理后端 `ApiResponse`

**Step 2: 用户态管理**

- token 写入 localStorage + cookie
- profile 写入 localStorage
- 提供 `openAuthModal` / `closeAuthModal`
- 提供 `login` / `logout`

### Task 5: 弹框式认证 UI

**Files:**
- Create: `apps/web/src/auth/AuthModal.tsx`
- Modify: `apps/web/src/App.tsx`
- Modify: `apps/web/src/router.tsx`
- Modify: `apps/web/src/styles.css`

**Step 1: 替换入口**

- 头部 `登录` / `注册` 改为打开弹框
- 删除独立 `/login` 路由依赖

**Step 2: 拆分弹框内容**

- 登录视图：短信登录 / 账号密码 / 微信扫码
- 注册视图：手机号 + 验证码 + 设置密码 + 确认密码
- 找回密码：手机号 + 验证码 + 新密码 + 确认密码
- 微信未绑定：弹出绑定手机号流程

**Step 3: UI/UX 要求**

- 明确 tab 切换
- 倒计时按钮禁用态
- 接口请求 loading
- 字段错误提示
- 空态说明
- ESC 关闭、遮罩关闭、焦点可达

### Task 6: 验证

**Files:**
- Modify: `backend/src/test/java/com/deltatrade/platform/DeltaTradeApplicationTests.java`

**Step 1: 前端验证**

- Run: `npm run build:web`

**Step 2: 后端验证**

- Run: `cd backend && mvn test`

**Step 3: 浏览器验证**

- 确认首页头部点击登录/注册可拉起弹框
- 确认短信、密码、微信 tab 可切换
- 确认注册和找回密码流程可完成
