# Admin Studio MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为管理后台与工作室后台补齐可运行的真实 MVP，包括仪表盘、列表管理、关键状态操作和基础响应式布局。

**Architecture:** 后端基于现有 Spring Boot + MyBatis-Plus + MySQL 表直接聚合管理/工作室视图数据，不新增复杂角色系统；前端 admin/studio 两个独立 Vite React 应用采用“仪表盘 + 菜单切换 + 数据面板”结构，优先打通真实接口，再补交互和状态。

**Tech Stack:** React 18、TypeScript、Vite、Spring Boot 2.7、MyBatis-Plus、MySQL、SLF4J + MDC traceId

---

### Task 1: 管理后台后端聚合接口

**Files:**
- Create: `backend/src/main/java/com/deltatrade/platform/modules/admin/service/AdminConsoleService.java`
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/admin/controller/AdminController.java`

**Steps:**
1. 聚合 `account_listing / trade_order / boosting_service / boosting_order / studio_profile / withdraw_application / auth_user` 的仪表盘统计。
2. 提供账号审核列表、订单列表、工作室列表、代肝服务列表、提现申请列表接口。
3. 提供账号审核通过/驳回/下架、工作室策略切换、工作室合作状态切换、代肝服务启停、提现审核通过/驳回接口。
4. 接入项目既有 `ApiResponse + MDC traceId`。

### Task 2: 工作室后台后端聚合接口

**Files:**
- Create: `backend/src/main/java/com/deltatrade/platform/modules/studio/service/StudioConsoleService.java`
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/studio/controller/StudioController.java`

**Steps:**
1. 基于当前登录用户绑定的 `studio_profile` 聚合工作台指标。
2. 提供工作室商品列表、订单列表、分润明细列表接口。
3. 提供工作室商品下架、重新上架/重提审核接口。
4. 复用现有发布逻辑和订单表，不重新造工作室商品体系。

### Task 3: Admin 前端数据层与页面重构

**Files:**
- Create: `apps/admin/src/lib/request.ts`
- Create: `apps/admin/src/modules/admin/admin-api.ts`
- Modify: `apps/admin/src/pages/AdminApp.tsx`
- Modify: `apps/admin/src/styles.css`

**Steps:**
1. 增加统一请求封装，兼容 cookie/token 鉴权、loading/error 状态。
2. 页面按“仪表盘 / 账号库管理 / 订单中心 / 工作室管理 / 代肝服务 / 提现审核”拆分视图。
3. 每个视图补齐 loading / error / empty / success。
4. 保持 PC 优先，H5 下转为纵向堆叠和可滚动 tab。

### Task 4: Studio 前端数据层与页面重构

**Files:**
- Create: `apps/studio/src/lib/request.ts`
- Create: `apps/studio/src/modules/studio/studio-api.ts`
- Modify: `apps/studio/src/pages/StudioApp.tsx`
- Modify: `apps/studio/src/styles.css`

**Steps:**
1. 增加统一请求封装，读取 cookie/token。
2. 页面按“工作台 / 账号商品 / 订单管理 / 分润明细 / 资料设置”拆分视图。
3. 商品页接真实列表与操作；订单页接真实工作室卖家订单；分润页显示结算摘要和订单维度分润。
4. H5 下改成单列工作台卡片与纵向表格卡片。

### Task 5: 联调与验证

**Files:**
- Modify: `apps/admin/package.json`（如需依赖）
- Modify: `apps/studio/package.json`（如需依赖）

**Steps:**
1. 编译后端：`mvn -DskipTests compile`
2. 构建前端：admin/studio 分别 `npm run build -w @delta/admin` / `@delta/studio`
3. 启动并抽样验证仪表盘、列表、状态操作。
4. 记录仍未覆盖的边界：角色权限、真正的运营配置 CRUD、更细后台流程。
