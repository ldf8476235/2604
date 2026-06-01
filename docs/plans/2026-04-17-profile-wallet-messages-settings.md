# 个人中心钱包消息与设置 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把个人中心里的钱包、消息中心、设置与安全从演示壳子升级为可持久化、可联调的真实业务链路。

**Architecture:** 后端继续沿用现有 Spring Boot + MyBatis-Plus + MySQL + Redis 鉴权体系，在 `auth_user` 上补充设置字段，并新增钱包、消息、提现账户等业务表。前端继续基于现有 `/profile` 页面扩展分区，所有功能统一复用当前 `request`、`AuthContext` 和 H5/PC 双端布局。

**Tech Stack:** React + TypeScript + Vite、Spring Boot、MyBatis-Plus、MySQL、JdbcTemplate schema upgrade、SLF4J + MDC

---

### Task 1: 梳理并补齐后端表结构

**Files:**
- Modify: `/Users/lee/Documents/code/2604/backend/src/main/resources/schema.sql`
- Modify: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthSchemaUpgrade.java`
- Create: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/profile/service/ProfileSchemaUpgrade.java`

**Step 1: 为 auth_user 补充设置类字段**

- nickname 继续复用
- 补充：
  - `wechat_bound BOOLEAN`
  - `login_alert_enabled BOOLEAN`
  - `secondary_verify_enabled BOOLEAN`
  - `avatar_key VARCHAR(255)`

**Step 2: 新建钱包相关表**

- `user_wallet`
- `wallet_transaction`
- `withdraw_account`
- `withdraw_application`

**Step 3: 新建消息表**

- `user_message`
- 支持分类、已读、删除、批量已读

**Step 4: 用 schema upgrade 做旧库补表补列**

- 启动即自动补齐测试库
- 保持与现有 `AuthSchemaUpgrade` / `ListingSchemaUpgrade` 风格一致

### Task 2: 后端实体、Mapper、Service 落地

**Files:**
- Create: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/profile/model/*.java`
- Create: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/profile/mapper/*.java`
- Create: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/profile/service/ProfileService.java`

**Step 1: 钱包领域模型**

- `UserWalletDO`
- `WalletTransactionDO`
- `WithdrawAccountDO`
- `WithdrawApplicationDO`

**Step 2: 消息领域模型**

- `UserMessageDO`

**Step 3: 统一钱包服务能力**

- 查询钱包首页聚合
- 创建充值记录并即时入账（当前项目未接支付，先按测试链路做真实入库）
- 绑定提现账户
- 提交提现申请
- 查询充值/提现/佣金流水

**Step 4: 统一消息服务能力**

- 分类查询
- 标记单条已读
- 批量已读
- 删除消息

### Task 3: 后端控制器与 Auth 设置接口

**Files:**
- Modify: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/auth/controller/AuthController.java`
- Modify: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/auth/service/AuthService.java`
- Create: `/Users/lee/Documents/code/2604/backend/src/main/java/com/deltatrade/platform/modules/profile/controller/ProfileController.java`

**Step 1: AuthController 增加设置与安全接口**

- 修改昵称
- 修改密码
- 获取设置摘要
- 修改登录提醒 / 二次验证
- 绑定/解绑微信（先做真实字段切换）
- 绑定提现账户、换绑手机号占用 profile 侧接口

**Step 2: ProfileController 增加钱包和消息接口**

- `/api/profile/wallet`
- `/api/profile/wallet/recharge`
- `/api/profile/wallet/withdraw-account`
- `/api/profile/wallet/withdraw`
- `/api/profile/messages`
- `/api/profile/messages/read`
- `/api/profile/messages/read-all`
- `/api/profile/messages/delete`

**Step 3: 接入现有统一错误码与 traceId**

- Controller 返回 `ApiResponse.success(...)`
- Service 继续抛 `BusinessException`

### Task 4: 前端 API 层和类型收口

**Files:**
- Modify: `/Users/lee/Documents/code/2604/apps/web/src/modules/profile/profile-api.ts`
- Modify: `/Users/lee/Documents/code/2604/apps/web/src/auth/auth-api.ts`

**Step 1: 新增钱包接口访问层**

- 查询钱包摘要
- 充值
- 绑定提现账户
- 提现申请

**Step 2: 新增消息接口访问层**

- 分类加载
- 单条已读
- 批量已读
- 删除

**Step 3: 新增设置接口访问层**

- 获取设置摘要
- 修改昵称
- 修改密码
- 修改安全开关
- 微信绑定状态更新

### Task 5: 前端 ProfilePage 改为真实数据

**Files:**
- Modify: `/Users/lee/Documents/code/2604/apps/web/src/pages/ProfilePage.tsx`
- Modify: `/Users/lee/Documents/code/2604/apps/web/src/modules/profile/profile-mock.ts`
- Modify: `/Users/lee/Documents/code/2604/apps/web/src/styles.css`

**Step 1: 我的钱包从 mock 改成真实链路**

- loading / error / empty / success 全状态
- 充值表单
- 提现账户表单
- 提现申请表单
- 充值/提现/佣金记录展示

**Step 2: 消息中心改成真实分类**

- 系统公告 / 交易通知 / 客服消息 / 分销通知 tab
- 未读红点
- 单条已读
- 批量已读
- 删除

**Step 3: 设置与安全改成真实交互**

- 修改昵称
- 修改密码
- 提醒/二次验证开关
- 微信绑定状态展示与解绑
- 提现账户绑定入口联动实名状态

**Step 4: H5 适配收口**

- 表单弹层 / 操作按钮 / 记录列表均校验移动端无横向溢出

### Task 6: 联调与验证

**Files:**
- Verify only

**Step 1: 后端编译**

Run: `mvn -DskipTests compile`

**Step 2: 前端构建**

Run: `npm run build -w @delta/web`

**Step 3: 浏览器联调**

- PC：`/profile?tab=wallet`
- PC：`/profile?tab=messages`
- PC：`/profile?tab=settings`
- H5：以上三页都验证无横向溢出、底部导航固定

**Step 4: 数据链路验证**

- 充值后余额增加且生成流水
- 提现账户绑定成功后再次进入可回显
- 提现申请入库并占用冻结金额
- 消息批量已读和删除后刷新仍生效
- 修改昵称后头部和个人中心同步刷新

