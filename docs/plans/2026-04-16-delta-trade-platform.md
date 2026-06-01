# Delta Trade Platform Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从 0 到 1 搭建游戏账号交易 + 代肝服务平台的可运行首期工程骨架，覆盖三端前端、Spring Boot API、MySQL/Redis 基础设施与核心业务模块边界。

**Architecture:** 采用 monorepo 管理 React 三端与共享 UI，后端先使用 Spring Boot 单体分层架构承载 REST API、WebSocket IM、OSS 能力与基础认证边界。数据库与 Redis 先固定数据模型和接入位点，再逐步替换示例 Service 为真实持久化实现。

**Tech Stack:** React 18、Vite、TypeScript、React Router、Spring Boot 3、Spring Security、WebSocket、MySQL、Redis、Docker Compose、阿里云 OSS SDK

---

### Task 1: Workspace Bootstrapping

**Files:**
- Create: `package.json`
- Create: `tsconfig.base.json`
- Create: `.gitignore`
- Create: `README.md`
- Create: `docs/plans/2026-04-16-delta-trade-platform.md`

**Step 1: Write the failing test**

先定义可执行目标：前端三端需能独立构建，后端需可编译并暴露健康接口。

**Step 2: Run test to verify it fails**

Run: `npm run build:frontends`
Expected: 当前仓库为空，命令失败。

**Step 3: Write minimal implementation**

补齐根工作区、脚本、说明文档和基础目录。

**Step 4: Run test to verify it passes**

Run: `npm run build:frontends`
Expected: 三端工作区进入真实构建阶段，不再因为根目录缺失脚本失败。

**Step 5: Commit**

```bash
git add .
git commit -m "feat: bootstrap workspace and implementation plan"
```

### Task 2: Shared UI and Design Tokens

**Files:**
- Create: `packages/ui/package.json`
- Create: `packages/ui/tsconfig.json`
- Create: `packages/ui/src/index.ts`
- Create: `packages/ui/src/tokens.css`
- Create: `packages/ui/src/components.tsx`

**Step 1: Write the failing test**

定义三端都要依赖统一设计令牌、按钮、卡片和数据状态组件。

**Step 2: Run test to verify it fails**

Run: `npm run build:web`
Expected: `@delta/ui` 不存在，构建失败。

**Step 3: Write minimal implementation**

创建共享 UI 包，沉淀品牌令牌、布局卡片、状态组件、指标卡。

**Step 4: Run test to verify it passes**

Run: `npm run build:web`
Expected: 用户端能解析共享 UI 包。

**Step 5: Commit**

```bash
git add packages/ui apps/web
git commit -m "feat: add shared ui package and design tokens"
```

### Task 3: User App

**Files:**
- Create: `apps/web/*`

**Step 1: Write the failing test**

页面必须同时覆盖 loading / error / empty / success。

**Step 2: Run test to verify it fails**

Run: `npm run build:web`
Expected: 用户端入口文件缺失。

**Step 3: Write minimal implementation**

实现首页、登录页、响应式布局、交易卡片和代肝服务区。

**Step 4: Run test to verify it passes**

Run: `npm run build:web`
Expected: 构建成功。

**Step 5: Commit**

```bash
git add apps/web
git commit -m "feat: add responsive user app shell"
```

### Task 4: Admin and Studio Apps

**Files:**
- Create: `apps/admin/*`
- Create: `apps/studio/*`

**Step 1: Write the failing test**

两端都要有独立导航、角色感知和桌面布局。

**Step 2: Run test to verify it fails**

Run: `npm run build:admin && npm run build:studio`
Expected: 工作区不存在导致构建失败。

**Step 3: Write minimal implementation**

实现仪表盘、待处理事项、商品表格、分润视图等骨架页面。

**Step 4: Run test to verify it passes**

Run: `npm run build:admin && npm run build:studio`
Expected: 构建成功。

**Step 5: Commit**

```bash
git add apps/admin apps/studio
git commit -m "feat: add admin and studio dashboard shells"
```

### Task 5: Spring Boot Backend Foundation

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/deltatrade/platform/**`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/logback-spring.xml`

**Step 1: Write the failing test**

需要先定义后端必须具备：统一响应、CORS、异常处理、traceId、核心业务控制器。

**Step 2: Run test to verify it fails**

Run: `cd backend && mvn test`
Expected: `pom.xml` 缺失，命令失败。

**Step 3: Write minimal implementation**

搭建 Spring Boot 应用、模块化包结构、基础配置和示例 API。

**Step 4: Run test to verify it passes**

Run: `cd backend && mvn test`
Expected: 至少通过一个上下文加载测试。

**Step 5: Commit**

```bash
git add backend
git commit -m "feat: add spring boot backend foundation"
```

### Task 6: Database, Redis, Docker and Docs

**Files:**
- Create: `backend/src/main/resources/db/schema.sql`
- Create: `docker-compose.yml`

**Step 1: Write the failing test**

需要让数据库实体边界、Redis 使用位点和本地环境一眼看懂。

**Step 2: Run test to verify it fails**

Run: `docker compose config`
Expected: compose 文件缺失。

**Step 3: Write minimal implementation**

定义 MySQL、Redis、初始化 SQL 和环境变量说明。

**Step 4: Run test to verify it passes**

Run: `docker compose config`
Expected: 配置解析通过。

**Step 5: Commit**

```bash
git add docker-compose.yml backend/src/main/resources/db/schema.sql README.md
git commit -m "feat: add local infrastructure bootstrap"
```
