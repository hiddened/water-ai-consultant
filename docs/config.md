# 配置说明

配置入口为 `.env`、Spring Boot 环境变量和 `backend/src/main/resources/application.yml`。

## Docker 环境变量

从模板创建：

```bash
cp .env.example .env
```

主要变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `POSTGRES_DB` | `water_ai_consultant` | PostgreSQL 数据库名 |
| `POSTGRES_USER` | `postgres` | PostgreSQL 用户 |
| `POSTGRES_PASSWORD` | `postgres` | PostgreSQL 密码 |
| `POSTGRES_PORT` | `5432` | 本机映射端口 |
| `BACKEND_PORT` | `8080` | 后端本机端口 |
| `FRONTEND_PORT` | `3000` | 前端本机端口 |
| `STORAGE_TYPE` | `local` | 文件存储类型，`local` 或 `minio` |
| `STORAGE_PATH` | `/app/storage` | 后端容器内文件存储路径 |
| `MINIO_API_PORT` | `9000` | MinIO API 本机映射端口 |
| `MINIO_CONSOLE_PORT` | `9001` | MinIO 控制台本机映射端口 |
| `MINIO_ENDPOINT` | `http://minio:9000` | 后端访问 MinIO 的地址 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO 访问账号 |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO 访问密钥 |
| `MINIO_BUCKET` | `water-ai-consultant` | 文档原文件存储 bucket |
| `LLM_PROVIDER` | `mock` | `mock` 或 `deepseek` |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek 兼容接口地址 |
| `DEEPSEEK_MODEL` | `deepseek-v4-flash` | DeepSeek 模型名 |
| `EMBEDDING_PROVIDER` | `mock` | Embedding 提供方 |
| `VECTOR_PROVIDER` | `pgvector` | 向量检索提供方 |

## Mock 模式

默认配置：

```env
LLM_PROVIDER=mock
```

Mock 模式不调用外部模型，适合本地启动、页面演示、接口联调和自动评分流程验证。

## DeepSeek 模式

```env
LLM_PROVIDER=deepseek
DEEPSEEK_API_KEY=你的 DeepSeek API Key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
```

不要提交真实 API Key。`.gitignore` 已忽略 `.env`。

## 文件存储

后端配置：

```yaml
app:
  storage:
    type: ${STORAGE_TYPE:local}
    local-root: ${STORAGE_PATH:${LOCAL_STORAGE_ROOT:./storage}}
    minio:
      endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: ${MINIO_BUCKET:water-ai-consultant}
```

Docker Compose 会把宿主机 `./storage` 挂载到容器内 `${STORAGE_PATH}`。仓库只保留 `storage/.gitkeep`。

默认 `STORAGE_TYPE=local`，上传文档保存到本地 `storage/`。如需使用 MinIO：

```env
STORAGE_TYPE=minio
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=water-ai-consultant
```

Docker Compose 会同时启动 MinIO，控制台地址为 `http://127.0.0.1:9001`。MinIO 只保存文档原文件；解析后的文本切片、引用来源和索引状态仍写入 PostgreSQL。

如果数据库已经初始化过，需要先执行一次迁移补字段：

```bash
psql -h 127.0.0.1 -U postgres -d water_ai_consultant -f database/migrations/20260701_add_minio_storage_fields.sql
```

## pgvector 和 Embedding

当前项目保留 pgvector/embedding 配置入口。没有真实 embedding 配置时，系统使用 MockEmbeddingClient 或关键词检索兜底。

如果启用真实向量检索，需要确认：

1. PostgreSQL 已安装 pgvector 扩展。
2. `EMBEDDING_DIMENSION` 与数据库 `VECTOR(n)` 维度一致。
3. embedding 模型和向量列维度不一致时，需要重建数据库列和索引。

## Nginx 代理

前端容器使用 `frontend/nginx.conf`：

- `/api/` -> `water-ai-consultant-server:8080/api/`
- `/swagger-ui/` -> `water-ai-consultant-server:8080/swagger-ui/`
- `/v3/api-docs` -> `water-ai-consultant-server:8080/v3/api-docs`
