# 基于Java Web的旅行规划助手工作台

个人独立开发项目 · 你好小赵同学（轻描淡写这一页）

## 功能

- 用户注册 / 登录（JWT）
- 旅行工作台：看板统计（待规划 / 已规划 / 进行中 / 已完成）
- 新建行程：目的地、日期、人数、预算、偏好
- 智能规划：优先调用 DeepSeek 大模型；无 Key 时本地规则引擎兜底
- 行程管理：查看、重命名、删除、状态流转
- 行程展示：每日安排、预算估算、地图点位示意、天气（需高德 Key）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus + MySQL + JWT |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| AI | DeepSeek API（可选） |
| 地图/天气 | 高德开放平台 Web服务（可选） |

## 本地开发

```bash
# 1. 初始化数据库
mysql -uroot -p < backend/src/main/resources/db/schema.sql

# 2. 启动后端（默认 8088）
cd backend && mvn spring-boot:run

# 3. 启动前端（5173，已代理 /api 到 8088）
cd frontend && npm install && npm run dev
```

## 配置 API Key（可选）

编辑 `backend/src/main/resources/application.yml`：

```yaml
app:
  deepseek:
    api-key: sk-xxxx
  amap:
    key: 高德Web服务Key
```

或启动时环境变量：`DEEPSEEK_API_KEY`、`AMAP_KEY`。

## 部署

打包后以 systemd / 守护进程方式运行 `travel-planner-*.jar`，前面挂 Nginx 反代即可。数据库初始化同「本地开发」，生产环境请通过环境变量注入数据库口令与 JWT 密钥。

## 目录结构

```
travel-planner/
├── backend/     # Spring Boot 后端
│   └── src/main/java/com/travelplanner/
│       ├── controller/   # REST 接口
│       ├── service/      # 业务（含 AiPlanService、AmapService）
│       ├── mapper/       # MyBatis-Plus
│       ├── entity/       # 实体
│       └── config/       # JWT、CORS、拦截器
├── frontend/    # Vue3 前端
│   └── src/views/
│       ├── Login.vue       # 登录注册
│       ├── Workbench.vue   # 工作台
│       ├── TripCreate.vue  # 新建行程
│       └── TripDetail.vue  # 行程详情/规划/地图
└── deploy/      # 部署脚本
```

## 作者

你好小赵同学（轻描淡写这一页）

- GitHub：https://github.com/zw-user1202
- 邮箱：3535188074@qq.com
- 微信：EA7_1202
