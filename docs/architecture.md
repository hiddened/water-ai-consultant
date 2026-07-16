# 架构说明

## 模块划分

```text
frontend/
  Vue 3 + Vite 单页应用
  Nginx 生产托管和 API 反向代理

backend/
  Spring Boot REST API
  知识库 CRUD
  文档上传、解析、切片
  检索、问答、需求分析
  对话、反馈、评测和任务中心

database/
  本地初始化 SQL

docker/postgres/
  Docker 初始化 schema 和演示数据
```

## 问答流程

```text
用户问题
  |
  v
知识检索
  |-- ai_document_chunk
  |-- ai_page
  |-- ai_capability
  |-- ai_api
  |-- ai_db_table
  |-- ai_requirement_case
  |
  v
Spring AI Document + PromptTemplate
  |
  v
Spring AI ChatClient / DeepSeek
  |
  v
结构化回答 + references
  |
  v
保存会话、消息、引用
```

## 需求可行性分析流程

```text
客户需求
  |
  v
优先检索能力清单和历史需求案例
  |
  v
补充检索页面、接口、数据表、文档 chunk
  |
  v
根据依据约束 A/B/C/D/E 等级
  |
  v
输出风险点、影响模块、建议方案和引用
```

## 评测流程

```text
ai_eval_case
  |
  v
运行智能问答或需求分析
  |
  v
EvalScoringService 自动评分
  |
  v
ai_eval_result
  |
  v
ai_eval_run 统计通过率、平均分、失败数
```

## Docker 部署拓扑

```text
Host
  |
  | :3000
  v
water-ai-consultant-web
  Nginx + Vue dist
  |
  | /api
  v
water-ai-consultant-server
  Spring Boot
  |       |
  |       +-- /app/storage -> ./storage
  v
postgres
  PostgreSQL volume: postgres_data
```

## 设计原则

- 回答必须基于检索依据。
- 无依据必须拒答。
- references 是前端溯源、反馈和评测的共同基础。
- 评测使用确定性规则评分，避免回归测试结果过度依赖模型波动。
- 没有模型 Key 时服务仍可启动；生成式回答必须配置真实模型，检索调试和固定拒答不依赖模型。
