# Auth MySQL Redis MyBatisPlus Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将当前内存态登录注册模块改造为基于 MySQL + Redis 的可持久化认证实现，并保留现有前端弹框交互与接口协议不变。

**Architecture:** 用户核心资料通过 MyBatis-Plus 持久化到 MySQL；短信验证码、注册/找回密码校验票据、微信扫码会话与登录态写入 Redis，并按业务时效设置 TTL。控制层继续沿用现有 `ApiResponse + traceId + GlobalExceptionHandler` 体系，前端无需改接口路径。

**Tech Stack:** Spring Boot 2.7、MyBatis-Plus、MySQL 8、Spring Data Redis、SLF4J + Logback、React + Vite 现有认证弹框。

---

### Task 1: 基础依赖与配置接入

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application.yml`

**Step 1: 增加数据层依赖**

- 引入 MyBatis-Plus Boot Starter
- 引入 Spring Data Redis Starter
- 引入 H2 测试依赖，避免测试直连生产库

**Step 2: 接入生产配置**

- datasource 使用用户提供的 MySQL 连接信息
- redis 使用用户提供的 Redis 连接信息
- 保留 `${ENV:default}` 覆盖能力，避免后续部署只能写死
- 增加 MyBatis-Plus 驼峰映射与日志关闭配置

**Step 3: 补测试配置**

- 测试环境切 H2
- 测试环境关闭远端依赖的强绑定
- 保证 `mvn test` 在本地可稳定执行

### Task 2: 账号持久化模型

**Files:**
- Create: `backend/src/main/java/com/deltatrade/platform/modules/auth/model/AuthUserDO.java`
- Create: `backend/src/main/java/com/deltatrade/platform/modules/auth/mapper/AuthUserMapper.java`

**Step 1: 建立用户表映射**

- 主键 id
- 手机号唯一
- 昵称
- 密码摘要
- 微信 openId 唯一
- 实名状态
- 创建/更新时间

**Step 2: 用 MyBatis-Plus 承接 CRUD**

- 通过 `BaseMapper<AuthUserDO>` 完成查询 / 新增 / 更新
- AuthService 不再维护内存用户 Map

### Task 3: Redis 认证缓存层

**Files:**
- Create: `backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthRedisStore.java`

**Step 1: 设计 Redis Key**

- `auth:sms:{scene}:{phone}`
- `auth:verify:{token}`
- `auth:wechat:scene:{sceneId}`
- `auth:login:{token}`

**Step 2: 封装 Redis 读写**

- 使用 `StringRedisTemplate`
- JSON 序列化缓存对象
- TTL 与业务时效对齐

### Task 4: AuthService 改造

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthService.java`

**Step 1: 切换用户来源**

- 手机号 / openId 查询改走 MyBatis-Plus
- 注册写 MySQL
- 重置密码更新 MySQL

**Step 2: 切换临时态来源**

- 短信验证码走 Redis
- 校验票据走 Redis
- 微信二维码会话走 Redis
- 登录成功后 token 会话写 Redis 7 天

**Step 3: 保持接口契约不变**

- 继续返回现有 `LoginResult / VerifyTicketResult / WechatPollResult`
- 前端无需改接口路径与字段

**Step 4: 保持日志规范**

- INFO：发码、校验成功、登录成功、注册成功、找回密码成功、微信绑定成功
- WARN：二维码过期、业务可恢复异常
- 不记录密码、验证码、token 原文

### Task 5: 数据初始化与稳定性

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthService.java`

**Step 1: 初始化演示账号**

- 若库中不存在 `13800138000 / 13900139000`，启动时自动补齐
- 避免每次启动重复插入

**Step 2: 兼容已有前端演示逻辑**

- 开发环境验证码仍固定为 `246810`
- 微信扫码继续模拟“已绑定 / 未绑定”两种路径

### Task 6: 验证

**Files:**
- Modify: `backend/src/test/java/com/deltatrade/platform/DeltaTradeApplicationTests.java`

**Step 1: 编译与测试**

- Run: `cd backend && mvn test`

**Step 2: 前端生产构建回归**

- Run: `npm run build:web`

**Step 3: 最小联调**

- 启动后端
- 验证短信发码、密码登录、微信二维码创建接口可访问
