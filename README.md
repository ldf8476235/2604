# Delta Trade Platform

游戏账号交易 + 代肝服务平台首期工程骨架，包含：

- 用户端：React + Vite + TypeScript，响应式布局，偏游戏交易市场视觉。
- 管理后台：React + Vite + TypeScript，PC 后台工作台。
- 工作室后台：React + Vite + TypeScript，面向工作室卖家的独立工作台。
- 后端：Spring Boot 3，提供 RESTful API、CORS、统一返回体、日志、traceId、WebSocket IM 与 OSS 接口骨架。
- 基础设施：MySQL、Redis、Docker Compose、数据库初始化脚本。

## 设计目标

1. 提供可直接扩展的三端前端目录与统一视觉令牌。
2. 提供可继续细化的 Spring Boot 分层架构，而不是一次性 demo 代码。
3. 先把认证、账号交易、订单、IM、OSS 等核心边界固定，再向业务细节深挖。

## 前端页面结构草图

### 用户端

- 顶部导航：品牌、消息、账号发布、代肝、客服、登录/注册。
- Hero：主搜索框 + 热门标签 + 平台卖点。
- 核心内容：交易大厅筛选、精选账号卡片、代肝服务、交易保障、IM 客服说明。
- 底部：入驻工作室 CTA、平台说明、备案/版权。

### 管理后台

- 左侧导航：仪表盘、账号库、订单、工作室、运营配置、客服监管。
- 顶部区：待处理事项、关键指标、支付和审核告警。
- 主区：图表、待审核列表、风控提醒、运营快捷入口。

### 工作室后台

- 左侧导航：工作台、商品管理、订单、分润、设置。
- 主区：数据卡片、审核策略提示、最近订单、分润流水。

## 后端方案说明

【备选方案：项目栈未确认】

当前仓库为空，因此我先落一个最稳妥的 Java 方案骨架，后续你可继续指定 ORM、鉴权、消息中间件与支付 SDK 细节：

| 方案 | 适用场景 | 取舍 |
| --- | --- | --- |
| Spring Boot + MyBatis-Plus + Redis | 业务表多、SQL 可控、后台管理型系统 | 开发效率高，适合账号/订单/审核/报表 |
| Spring Boot + Spring Data JPA + Redis | 领域对象稳定、查询复杂度中等 | 实体建模快，但复杂 SQL 可控性弱 |
| Spring Boot + MyBatis + Redis | 对 SQL 精细控制要求高 | 灵活，但样板代码更多 |

本次首期代码先不强绑定 ORM，实现 API 分层、配置、日志、WebSocket IM、OSS 接口与示例控制器，避免后续选型反复推倒。

## 需要你后续确认的 4 个关键信息

1. ORM 最终选型：`MyBatis-Plus / JPA / MyBatis`
2. 鉴权方案：`JWT 单体令牌 / Sa-Token / Spring Security OAuth2`
3. 支付接入优先级：`微信 JSAPI / 支付宝扫码 / 两者并行`
4. IM 落地方式：`Spring WebSocket 单体 / 第三方 IM / 自建网关`

## 快速启动

### 前端

```bash
npm install
npm run build:frontends
```

开发时分别启动：

```bash
npm run dev:web
npm run dev:admin
npm run dev:studio
```

### 后端

```bash
cd backend
mvn spring-boot:run
```

### 基础设施

```bash
docker compose up -d
```

## 目录结构

```text
.
├── apps
│   ├── web
│   ├── admin
│   └── studio
├── packages
│   └── ui
├── backend
│   └── src
├── design-system
└── docs/plans
```

