# 面试前 30 分钟快速复习小抄

## 1. 项目一句话

AI 水务项目智能顾问平台是一个面向水务内部团队的 RAG + 结构化知识系统，用于基于项目资料回答问题、分析客户需求可行性，并通过引用溯源、评测和反馈闭环保证回答质量。

## 2. 30 秒介绍

这个项目是给水务行业内部售前、产品、实施、开发和运维用的项目智能顾问。它把项目文档、页面操作说明、能力清单、接口说明、数据库表说明和历史需求案例统一管理，用户可以做智能问答和需求可行性分析。技术上用 Spring Boot、Vue 3、PostgreSQL、Apache Tika 和 DeepSeek，支持文档上传解析切片、引用溯源、无依据拒答、检索调试、问答评测和反馈转知识库。

## 3. 1 分钟介绍

这个项目解决的是水务定制化项目里资料分散、历史需求难复用、客户需求判断依赖人工经验的问题。整体架构是 Vue 3 前端、Spring Boot + Spring AI 后端和 PostgreSQL 数据库。问答时先检索多类项目知识，把证据转换为 Spring AI Document，再通过 PromptTemplate 和 ChatClient 调用 DeepSeek。没有依据时直接固定拒答；项目还包含引用溯源、检索调试、自动评测、对话记录和反馈转知识库闭环。

## 4. 必背技术栈

- 后端：Spring Boot 3.5、Spring AI 1.1、Java 21、Spring JDBC
- 前端：Vue 3、Vite、TypeScript
- 数据库：PostgreSQL、JSONB、pgvector 可选
- 文档解析：Apache Tika
- 大模型：Spring AI ChatClient + DeepSeek / OpenAI Compatible
- Embedding：Spring AI EmbeddingModel；未配置时降级关键词检索
- 文件存储：本地 storage，MinIO 可选
- API 文档：Swagger / OpenAPI
- 部署：Docker Compose、Nginx

## 5. 必背核心流程

### 文档上传解析流程

用户上传文档 -> 后端保存原文件到 storage 或 MinIO -> 写入 `ai_document` -> 创建 `ai_task` -> 异步调用 Apache Tika 提取文本 -> `DocumentChunker` 切片 -> 写入 `ai_document_chunk` -> 更新 `parse_status`、`chunk_count`、任务状态和日志。

### 智能问答流程

用户提问 -> `/api/chat` 校验 project 和 mode -> 检索文档 chunk、页面、能力、接口、数据表、历史案例 -> 转换 Spring AI Document -> 无依据固定拒答 -> 有依据则用 PromptTemplate 渲染 -> ChatClient 调 DeepSeek -> 保存会话、消息和引用。

### 需求可行性分析流程

用户输入需求 -> `/api/requirements/check` -> 优先检索能力清单和历史需求案例 -> 补充页面、接口、数据表、文档 chunk -> 模型输出结构化 JSON -> 后端归一化可行性等级 -> 返回已有能力、缺失能力、风险点、推荐方案和引用。

### 引用溯源流程

检索结果统一成 `KnowledgeEvidence` -> 转成 `references` 返回 -> 保存到 `ai_answer_reference` -> 前端可点击查看原文或预览 -> 对话记录、反馈和评测复用引用数据。

### 问答评测流程

维护 `ai_eval_case` -> 单条或批量运行 -> 调 `/api/chat` 或 `/api/requirements/check` -> 保存 `ai_eval_result` -> 按关键词、引用、拒答、可行性等级自动评分 -> 汇总到 `ai_eval_run` -> 看板展示并支持人工复核。

### 反馈转知识库流程

用户提交反馈 -> 写入 `ai_feedback`，状态 pending -> 管理员复核通过或驳回 -> 通过后可转知识库 -> 写入页面、能力、接口、数据表、历史需求案例或 FAQ 草稿 -> 保留 `source_feedback_id` 追溯来源。

## 6. 必背接口

1. `GET /api/health`：健康检查。
2. `POST /api/documents/upload`：上传文档并创建解析任务。
3. `POST /api/documents/{id}/parse`：重新解析文档。
4. `POST /api/documents/{id}/reindex`：重建指定文档索引。
5. `GET /api/documents/{id}/chunks`：查看文档切片。
6. `POST /api/chat`：智能问答。
7. `POST /api/requirements/check`：需求可行性分析。
8. `POST /api/search/debug`：检索调试，不调用模型。
9. `GET /api/tasks`：任务列表。
10. `GET /api/tasks/{id}/logs`：任务日志。
11. `GET /api/chat-sessions`：对话记录。
12. `POST /api/feedback`：提交反馈。
13. `POST /api/eval-cases/run-batch`：批量运行评测。
14. `GET /api/eval-runs/{id}/summary`：评测运行汇总。
15. `GET /api/model-configs`、`GET /api/prompt-templates`：模型和 Prompt 配置。

## 7. 必背表

1. `ai_project`：项目主表。
2. `ai_document`：文档元数据、解析状态、索引状态。
3. `ai_document_chunk`：文档切片和引用来源。
4. `ai_page`：页面操作说明。
5. `ai_capability`：系统能力清单。
6. `ai_api`：接口说明。
7. `ai_db_table`：数据库表说明。
8. `ai_requirement_case`：历史需求案例。
9. `ai_task`、`ai_task_log`：异步任务和日志。
10. `ai_chat_session`、`ai_chat_message`：会话和消息。
11. `ai_answer_reference`：回答引用来源。
12. `ai_feedback`、`ai_faq`：反馈和 FAQ 沉淀。
13. `ai_eval_case`、`ai_eval_result`、`ai_eval_run`：评测体系。
14. `ai_model_config`：模型配置。
15. `ai_prompt_template`：Prompt 模板。

## 8. 必讲亮点

1. 水务项目智能顾问：不是泛化客服，而是服务项目交付、售前和实施场景。
2. 混合知识来源：不只查文档，还查页面、能力、接口、数据表和历史需求。
3. 需求分析护栏：没有能力清单依据时，不允许直接判断 A/B。
4. 无依据拒答：没检索到证据时返回固定拒答，避免模型乱答。
5. 引用溯源：每个回答带 references，并落库到 `ai_answer_reference`。
6. 检索调试：不调用模型即可看命中、分数、策略和降级情况。
7. 评测闭环：评测用例、自动评分、批量运行和看板能支撑回归测试。
8. 反馈沉淀：用户纠错经过复核后可转成知识库草稿。

## 9. 必讲难点

1. 避免模型胡编：用检索证据、固定拒答和服务端等级归一化兜底。
2. 引用溯源：把检索证据抽象成统一引用对象，并保存到引用表。
3. 需求可行性分析：让模型做表达，后端用能力清单和历史案例控制边界。
4. 文档解析失败：异步任务落库，失败写状态、错误和日志。
5. 检索不准排查：通过检索调试页区分解析、切片、检索、Prompt 和模型问题。
6. 自动评分：按关键词、引用、拒答和可行性等级分项打分。
7. 反馈防污染：反馈先复核，再转知识库草稿，不直接启用。
8. 工程可验证：Docker Compose 一键启动，真实模型配置与非生成式功能边界清晰。

## 10. 必讲不足

### pgvector 生产调优还可以继续完善

面试怎么回答：当前代码已有 pgvector 可选检索和关键词降级，适合 MVP 验证；生产上还需要按真实 embedding 维度、数据量和召回效果调 HNSW/IVFFLAT 索引、混合排序和评测集。

### 用户权限还没做完整

面试怎么回答：当前重点是业务闭环，没有接完整登录、角色和权限；后续会用 Spring Security/JWT 增加用户、角色、菜单权限和项目访问控制。

### MinIO 生产化还没做完整

面试怎么回答：当前项目已有本地 storage 和可选 MinIO 基础接入，Docker Compose 也提供 MinIO；不足是还没做生产级桶权限、生命周期、审计、文件预览和备份策略。

### MQ 还没接

面试怎么回答：当前异步任务用 Spring `@Async` 和任务表，足够支撑本地演示；如果后续文档量大，会接 RabbitMQ/Kafka 做分布式任务、重试和死信队列。

### 多租户还没做

面试怎么回答：表结构已有 `project_id`，但还没有用户到项目的授权关系；后续会增加租户、用户项目授权和接口级数据隔离。

### 生产部署还可以增强

面试怎么回答：当前 Docker Compose 解决本地一键启动，生产上还需要 HTTPS、正式 Nginx、日志采集、监控告警、数据库备份和 CI/CD。

## 11. 高频追问 20 题

1. 为什么做这个项目？

答：为了解决水务项目资料分散、历史经验难复用和需求判断依赖人工经验的问题。

2. 为什么不是普通客服？

答：它面向内部项目团队，处理项目资料、页面操作、接口、数据库和需求可行性，不是市民咨询。

3. 和普通 RAG 有什么区别？

答：普通 RAG 多查文档，本项目还查能力、页面、接口、数据表、历史案例，并有评测和反馈闭环。

4. 文档怎么解析？

答：上传后异步任务调用 Apache Tika 提取文本，再切片写入 `ai_document_chunk`。

5. 为什么要切片？

答：控制 Prompt 长度，提高检索精度，并把引用定位到具体片段。

6. 检索哪些内容？

答：文档 chunk、文档元数据、页面说明、能力清单、接口说明、数据表说明和历史需求案例。

7. DeepSeek 怎么接？

答：`ModelRuntimeService` 根据数据库或环境配置创建 Spring AI `ChatClient`，通过 OpenAI 兼容协议调用 DeepSeek。

8. 为什么没有 Mock LLM？

答：避免固定文本被误认为真实模型结果；无 Key 时服务仍可启动和检索，但生成式回答会明确提示配置真实模型。

9. 如何避免幻觉？

答：先检索证据，无依据固定拒答；需求分析还会做等级归一化。

10. 引用怎么做？

答：检索证据返回为 references，同时保存到 `ai_answer_reference`，前端可展开详情。

11. 需求等级怎么判断？

答：模型输出后，后端根据能力清单、历史案例和依据情况归一化 A/B/C/D/E。

12. 为什么只有文档不能判 A/B？

答：文档描述不等于能力承诺，没有能力清单依据时不能说现有功能完全支持。

13. 检索不准怎么办？

答：用 `/api/search/debug` 看命中和分数，定位是解析、切片、检索、Prompt 还是模型问题。

14. 任务中心解决什么？

答：文档解析和索引耗时，任务中心让接口快速返回，并可查看进度、日志和失败原因。

15. 反馈怎么转知识？

答：用户提交反馈，管理员复核后按目标类型转成页面、能力、接口、数据表、历史案例或 FAQ 草稿。

16. 评测怎么评分？

答：按关键词、引用、拒答和可行性等级分项评分，再生成总分和通过标记。

17. Prompt 怎么管理？

答：Prompt 模板入库，按 template_type 和 mode 选择默认模板，支持渲染测试。

18. 模型配置怎么管理？

答：模型 provider、base_url、Key、模型名、维度等入库，支持测试和设默认。

19. Docker 怎么启动？

答：复制 `.env.example` 为 `.env`，执行 `docker compose up -d --build`。

20. 后续怎么生产化？

答：补权限、多租户、MQ、pgvector 调优、文件存储生产化、监控告警和备份。
