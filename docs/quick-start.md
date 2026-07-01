# 快速启动

## Docker Compose 启动

```bash
cp .env.example .env
docker compose up -d --build
```

访问：

- 前端：http://127.0.0.1:3000
- 后端：http://127.0.0.1:8080
- Swagger：http://127.0.0.1:8080/swagger-ui/index.html

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

## 重置演示数据

PostgreSQL 容器只会在首次初始化时执行 SQL。如果修改了 `docker/postgres/*.sql`，需要清空 volume：

```bash
docker compose down -v
docker compose up -d --build
```

## 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 3000
```

本地数据库：

```bash
createdb water_ai_consultant
psql -U postgres -d water_ai_consultant -f database/init.sql
```
