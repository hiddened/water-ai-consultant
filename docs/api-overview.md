# API 概览

Swagger 地址：

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

OpenAPI JSON：

```text
http://127.0.0.1:8080/v3/api-docs
```

## 健康检查

```http
GET /api/health
```

## 知识库数据管理

- `GET /api/projects`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`
- `GET /api/documents`
- `POST /api/documents/upload`
- `POST /api/documents/{id}/parse`
- `POST /api/documents/{id}/reindex`
- `GET /api/documents/{id}/chunks`
- `GET /api/pages`
- `GET /api/capabilities`
- `GET /api/apis`
- `GET /api/db-tables`
- `GET /api/requirement-cases`

列表接口通常支持：

- `project_id`
- `module_name`
- `keyword`

## 智能问答

```http
POST /api/chat
Content-Type: application/json

{
  "project_id": "项目ID",
  "mode": "business_qa",
  "question": "外部告警推送接口怎么用？"
}
```

返回重点字段：

- `answer`
- `references`
- `related_pages`
- `related_capabilities`
- `related_apis`
- `confidence`
- `trace_id`
- `model_provider`
- `model_name`
- `search_strategy`

## 需求可行性分析

```http
POST /api/requirements/check
Content-Type: application/json

{
  "project_id": "项目ID",
  "requirement_desc": "客户想接入二供泵房告警并自动建单，能不能做？",
  "module_name": "告警中心"
}
```

可行性等级：

- A：现有功能已支持
- B：配置即可支持
- C：需要二次开发
- D：资料不足，无法判断
- E：不建议实现

## 检索调试

```http
POST /api/search/debug
Content-Type: application/json

{
  "project_id": "项目ID",
  "question": "auto_diagnose 参数有什么作用？",
  "mode": "business_qa",
  "top_k": 10
}
```

该接口只检索，不调用大模型。

## 评测和回归看板

- `GET /api/eval-cases`
- `POST /api/eval-cases`
- `POST /api/eval-cases/{id}/run`
- `POST /api/eval-cases/run-batch`
- `GET /api/eval-runs`
- `GET /api/eval-runs/{id}`
- `GET /api/eval-runs/{id}/results`
- `GET /api/eval-runs/{id}/summary`
- `POST /api/eval-runs/{id}/rerun-failed`

## 任务中心

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `GET /api/tasks/{id}/logs`
- `POST /api/tasks/{id}/retry`
- `POST /api/tasks/{id}/cancel`
