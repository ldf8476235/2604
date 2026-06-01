# IM 客服工作台与 WebSocket 实时推送 Implementation Plan

**Goal:** 为现有站内 IM 补齐客服工作台与 WebSocket 实时推送，让用户会话和客服回复都能实时更新，并保持 PC/H5 可用。

**Architecture:** 后端继续复用现有 `chat_group_no` 作为会话号，在 `ImService` 内统一维护会话初始化、消息入库、工作台列表投影与 WebSocket 推送。前端新增客服工作台页面，用户聊天页和客服工作台都基于同一套 STOMP 推送通道实时更新。

**Tech Stack:** React + TypeScript + Vite，Spring Boot + MyBatis-Plus + MySQL，Spring WebSocket(STOMP)，SLF4J + MDC traceId

---

### Task 1: 扩展后端 IM 服务

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/im/service/ImService.java`
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/im/controller/ImController.java`
- Create: `backend/src/main/java/com/deltatrade/platform/modules/im/controller/ImWorkbenchController.java`

**Steps:**
1. 给 `ImService` 增加工作台会话列表查询能力。
2. 给 `ImService` 增加客服视角加载单会话能力。
3. 给 `ImService` 增加客服发送消息能力。
4. 抽出统一的会话 payload 构造逻辑，支持用户视角与客服视角。
5. 保留现有用户接口不变，新增工作台接口。

### Task 2: 接入 WebSocket 推送

**Files:**
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/im/service/ImService.java`
- Modify: `backend/src/main/java/com/deltatrade/platform/modules/im/controller/WebSocketConfig.java`

**Steps:**
1. 在 `ImService` 注入 `SimpMessagingTemplate`。
2. 会话初始化、发消息、客服回复、已读后推送会话详情到 `/topic/im/{conversationNo}`。
3. 会话摘要变更时推送工作台事件到 `/topic/im/workbench`。
4. 保持现有 REST 接口为首屏加载兜底，WebSocket 负责增量实时更新。

### Task 3: 前端新增客服工作台

**Files:**
- Create: `apps/web/src/pages/ImWorkbenchPage.tsx`
- Modify: `apps/web/src/modules/im/im-api.ts`
- Modify: `apps/web/src/router.tsx`
- Modify: `apps/web/src/App.tsx`
- Modify: `apps/web/src/styles.css`

**Steps:**
1. 新增工作台列表接口调用封装。
2. 新增客服工作台页面，左侧会话列表、右侧聊天详情。
3. 顶部增加客服工作台入口。
4. H5 下改成单列布局，确保无横向溢出。

### Task 4: 前端聊天页接实时订阅

**Files:**
- Modify: `apps/web/src/pages/ImChatPage.tsx`
- Create: `apps/web/src/modules/im/im-realtime.ts`
- Modify: `apps/web/package.json`

**Steps:**
1. 引入 STOMP 客户端依赖。
2. 抽出 IM WebSocket 连接工具。
3. `ImChatPage` 改成首屏 REST + 后续实时订阅。
4. 客服工作台页同样接入实时订阅。
5. 保留发送中、上传中、断线提示等状态。

### Task 5: 编译与联调

**Files:**
- None

**Steps:**
1. 运行后端编译：`mvn -DskipTests compile`
2. 运行前端构建：`npm run build -w @delta/web`
3. 重启后端服务。
4. 浏览器验证：
   - PC 客服工作台可打开、可回复
   - 用户聊天页发消息后工作台实时收到
   - H5 聊天页无横向溢出、输入区不被底部导航遮挡
