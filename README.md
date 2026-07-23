# MailTrace 邮迹工单

邮件工单系统（前后端同仓）。

| 项 | 值 |
|----|-----|
| 产品 | MailTrace（邮迹工单） |
| 路径 | `/Users/tanzhixing/workSpace/sfd-mailtrace` |
| 后端包名 | `com.ntn.fziot.mailtrace` |
| 表前缀 | `mt_` |

## 目录

```
sfd-mailtrace/
├── backend/     # Spring Boot 3.2 + Java 17
├── frontend/    # React + Ant Design 6 + Vite
├── deploy/      # Docker / Nginx
└── docs/        # 需求、架构、数据库设计
```

## 快速开始

### 1. 准备本地 MySQL

默认连接本机：

| 项 | 值 |
|----|-----|
| Host | `127.0.0.1:3306` |
| User | `root` |
| Password | `root123` |
| Database | `mailtrace` |

先建库（若尚未创建）：

```sql
CREATE DATABASE IF NOT EXISTS mailtrace
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> 可选：`docker-compose.dev.yml` 仅作备用，日常开发优先用本地 MySQL。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认：`http://127.0.0.1:8080/api`  
Swagger：`http://127.0.0.1:8080/api/swagger-ui.html`  
启动时 Flyway 会自动建表（`mt_*`）。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

默认：`http://127.0.0.1:5173`

## 文档

见 [docs/](./docs/)：

- [命名约定](./docs/命名约定.md)
- [需求文档](./docs/邮件工单系统需求文档.md)
- [项目架构与规范](./docs/项目架构与规范.md)
- [数据库设计](./docs/数据库设计.md)

## 默认账号（开发初始化）

后续 Flyway `V2__init_data.sql` 会写入；当前骨架仅含表结构。
