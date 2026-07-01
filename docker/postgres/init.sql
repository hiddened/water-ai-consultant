CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- pgvector 不是本阶段启动前置条件；已安装扩展时可直接启用，未安装时先跳过向量列落库。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS ai_project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_code VARCHAR(64) NOT NULL UNIQUE,
    project_name VARCHAR(128) NOT NULL,
    customer_name VARCHAR(128),
    industry VARCHAR(64) DEFAULT 'water',
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    document_name VARCHAR(256) NOT NULL,
    module_name VARCHAR(128),
    document_type VARCHAR(64),
    file_path VARCHAR(512) NOT NULL,
    file_hash VARCHAR(128),
    file_size BIGINT,
    storage_type VARCHAR(32) NOT NULL DEFAULT 'local',
    storage_bucket VARCHAR(128),
    storage_object_key VARCHAR(768),
    content_type VARCHAR(128),
    parse_status VARCHAR(32) NOT NULL DEFAULT 'uploaded',
    parse_error TEXT,
    last_parsed_at TIMESTAMPTZ,
    index_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    index_error TEXT,
    last_indexed_at TIMESTAMPTZ,
    chunk_count INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by VARCHAR(64),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    document_id UUID NOT NULL REFERENCES ai_document(id),
    document_title VARCHAR(256),
    chunk_index INT,
    chunk_no INT NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(128),
    page_no INT,
    page_number INT,
    section_title VARCHAR(256),
    source_locator VARCHAR(512),
    keywords TEXT,
    embedding_model VARCHAR(128),
    index_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    index_error TEXT,
    last_indexed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(document_id, chunk_no)
);

CREATE TABLE IF NOT EXISTS ai_page (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    page_name VARCHAR(128) NOT NULL,
    module_name VARCHAR(128),
    route_path VARCHAR(256),
    operation_desc TEXT NOT NULL,
    business_rule TEXT,
    keywords TEXT,
    source_feedback_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_capability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    capability_name VARCHAR(128) NOT NULL,
    module_name VARCHAR(128),
    support_level VARCHAR(32) NOT NULL DEFAULT 'supported',
    config_required BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT NOT NULL,
    limitation TEXT,
    keywords TEXT,
    source_feedback_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_api (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    api_name VARCHAR(128) NOT NULL,
    module_name VARCHAR(128),
    method VARCHAR(16) NOT NULL,
    path VARCHAR(256) NOT NULL,
    request_desc TEXT,
    response_desc TEXT,
    auth_desc TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    keywords TEXT,
    source_feedback_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_db_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    table_name VARCHAR(128) NOT NULL,
    table_comment VARCHAR(256),
    module_name VARCHAR(128),
    field_desc JSONB NOT NULL DEFAULT '[]'::jsonb,
    relation_desc TEXT,
    keywords TEXT,
    source_feedback_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_requirement_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    case_title VARCHAR(256) NOT NULL,
    module_name VARCHAR(128),
    requirement_desc TEXT NOT NULL,
    solution_desc TEXT,
    feasibility_level VARCHAR(8),
    workload_level VARCHAR(32),
    risk_points TEXT,
    keywords TEXT,
    source_feedback_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    title VARCHAR(256),
    mode VARCHAR(64) NOT NULL,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES ai_chat_session(id),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    confidence NUMERIC(5, 4),
    trace_id VARCHAR(64),
    llm_provider VARCHAR(64),
    model_config_id UUID,
    model_provider VARCHAR(64),
    model_name VARCHAR(128),
    prompt_template_id UUID,
    prompt_template_name VARCHAR(128),
    prompt_rendered_preview TEXT,
    search_strategy VARCHAR(64),
    insufficient_answer BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_model_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_name VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(512),
    api_key_value TEXT,
    api_key_masked VARCHAR(128),
    model_name VARCHAR(128),
    dimension INT,
    temperature NUMERIC(4, 2),
    max_tokens INT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_config BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(128) NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    mode VARCHAR(64),
    system_prompt TEXT,
    user_prompt_template TEXT NOT NULL,
    output_format VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_template BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS ai_answer_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES ai_chat_message(id),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    source_type VARCHAR(64) NOT NULL,
    source_id UUID NOT NULL,
    source_title VARCHAR(256),
    source_locator VARCHAR(512),
    quote TEXT,
    score NUMERIC(8, 6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES ai_project(id),
    session_id UUID REFERENCES ai_chat_session(id),
    message_id UUID REFERENCES ai_chat_message(id),
    rating INT,
    feedback_type VARCHAR(64),
    content TEXT,
    remark TEXT,
    corrected_answer TEXT,
    expected_sources JSONB NOT NULL DEFAULT '[]'::jsonb,
    convert_to_knowledge BOOLEAN NOT NULL DEFAULT FALSE,
    target_knowledge_type VARCHAR(64),
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer VARCHAR(64),
    review_remark TEXT,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_faq (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_feedback_id UUID REFERENCES ai_feedback(id),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_eval_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES ai_project(id),
    question TEXT NOT NULL,
    expected_answer TEXT,
    expected_sources JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_mode VARCHAR(64) NOT NULL DEFAULT 'doc_qa',
    expected_feasibility_level TEXT,
    expected_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_source_titles JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_source_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_refusal BOOLEAN NOT NULL DEFAULT FALSE,
    expected_answer_type VARCHAR(64),
    score_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    difficulty VARCHAR(32),
    remark TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_eval_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_name VARCHAR(128) NOT NULL,
    project_id UUID REFERENCES ai_project(id),
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    total_cases INT NOT NULL DEFAULT 0,
    success_cases INT NOT NULL DEFAULT 0,
    failed_cases INT NOT NULL DEFAULT 0,
    average_score NUMERIC(5, 2) NOT NULL DEFAULT 0,
    pass_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    model_provider VARCHAR(64),
    model_name VARCHAR(128),
    prompt_template_id UUID,
    prompt_template_name VARCHAR(128),
    search_strategy VARCHAR(64),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    remark TEXT
);

CREATE TABLE IF NOT EXISTS ai_eval_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID REFERENCES ai_eval_run(id),
    eval_case_id UUID NOT NULL REFERENCES ai_eval_case(id),
    actual_answer TEXT,
    actual_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    actual_feasibility_level VARCHAR(8),
    auto_score NUMERIC(5, 2),
    keyword_score NUMERIC(5, 2),
    source_score NUMERIC(5, 2),
    refusal_score NUMERIC(5, 2),
    feasibility_score NUMERIC(5, 2),
    reference_count INT NOT NULL DEFAULT 0,
    matched_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    matched_sources JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_sources JSONB NOT NULL DEFAULT '[]'::jsonb,
    auto_passed BOOLEAN,
    manual_passed BOOLEAN,
    score_detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    model_provider VARCHAR(64),
    model_name VARCHAR(128),
    prompt_template_id UUID,
    prompt_template_name VARCHAR(128),
    search_strategy VARCHAR(64),
    duration_ms BIGINT,
    error_message TEXT,
    passed BOOLEAN,
    score NUMERIC(5, 2),
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_type VARCHAR(64) NOT NULL,
    biz_id UUID,
    biz_type VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    progress INT NOT NULL DEFAULT 0,
    message TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_task_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES ai_task(id) ON DELETE CASCADE,
    log_level VARCHAR(16) NOT NULL DEFAULT 'info',
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE ai_project ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS module_name VARCHAR(128);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS parse_status VARCHAR(32) NOT NULL DEFAULT 'uploaded';
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS parse_error TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS last_parsed_at TIMESTAMPTZ;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS index_status VARCHAR(32) NOT NULL DEFAULT 'pending';
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS index_error TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS last_indexed_at TIMESTAMPTZ;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS chunk_count INT NOT NULL DEFAULT 0;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS storage_type VARCHAR(32) NOT NULL DEFAULT 'local';
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS storage_bucket VARCHAR(128);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS storage_object_key VARCHAR(768);
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS content_type VARCHAR(128);
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS document_title VARCHAR(256);
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS chunk_index INT;
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS page_number INT;
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS index_status VARCHAR(32) NOT NULL DEFAULT 'pending';
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS index_error TEXT;
ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS last_indexed_at TIMESTAMPTZ;
ALTER TABLE ai_answer_reference ALTER COLUMN score TYPE NUMERIC(12, 4);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(64);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS model_config_id UUID;
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS model_provider VARCHAR(64);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS model_name VARCHAR(128);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS prompt_template_id UUID;
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS prompt_template_name VARCHAR(128);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS prompt_rendered_preview TEXT;
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS search_strategy VARCHAR(64);
ALTER TABLE ai_chat_message ADD COLUMN IF NOT EXISTS insufficient_answer BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS remark TEXT;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS corrected_answer TEXT;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS expected_sources JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS convert_to_knowledge BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS target_knowledge_type VARCHAR(64);
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS review_status VARCHAR(32) NOT NULL DEFAULT 'pending';
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS reviewer VARCHAR(64);
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS review_remark TEXT;
ALTER TABLE ai_feedback ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE ai_page ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_page ADD COLUMN IF NOT EXISTS source_feedback_id UUID;
ALTER TABLE ai_capability ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_capability ADD COLUMN IF NOT EXISTS source_feedback_id UUID;
ALTER TABLE ai_api ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_api ADD COLUMN IF NOT EXISTS source_feedback_id UUID;
ALTER TABLE ai_db_table ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_db_table ADD COLUMN IF NOT EXISTS source_feedback_id UUID;
ALTER TABLE ai_requirement_case ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_requirement_case ADD COLUMN IF NOT EXISTS source_feedback_id UUID;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE ai_eval_case ALTER COLUMN expected_feasibility_level TYPE TEXT;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS expected_keywords JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS expected_source_titles JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS expected_source_types JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS expected_refusal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS expected_answer_type VARCHAR(64);
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS score_rules JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS tags JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS difficulty VARCHAR(32);
ALTER TABLE ai_eval_case ADD COLUMN IF NOT EXISTS remark TEXT;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS run_id UUID REFERENCES ai_eval_run(id);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS auto_score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS keyword_score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS source_score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS refusal_score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS feasibility_score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS reference_count INT NOT NULL DEFAULT 0;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS matched_keywords JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS matched_sources JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS missing_keywords JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS missing_sources JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS auto_passed BOOLEAN;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS manual_passed BOOLEAN;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS score_detail JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS model_provider VARCHAR(64);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS model_name VARCHAR(128);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS prompt_template_id UUID;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS prompt_template_name VARCHAR(128);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS search_strategy VARCHAR(64);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS duration_ms BIGINT;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS error_message TEXT;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS passed BOOLEAN;
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS score NUMERIC(5, 2);
ALTER TABLE ai_eval_result ADD COLUMN IF NOT EXISTS remark TEXT;

CREATE INDEX IF NOT EXISTS idx_ai_document_project ON ai_document(project_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_document_project_module ON ai_document(project_id, module_name) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_document_chunk_project ON ai_document_chunk(project_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_page_project_module ON ai_page(project_id, module_name) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_capability_project_module ON ai_capability(project_id, module_name) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_api_project_path ON ai_api(project_id, path) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_db_table_project_name ON ai_db_table(project_id, table_name) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_requirement_case_project_module ON ai_requirement_case(project_id, module_name) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_chat_message_session ON ai_chat_message(session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_answer_reference_message ON ai_answer_reference(message_id);
CREATE INDEX IF NOT EXISTS idx_ai_feedback_project_status ON ai_feedback(project_id, review_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_feedback_message ON ai_feedback(message_id);
CREATE INDEX IF NOT EXISTS idx_ai_eval_case_project ON ai_eval_case(project_id) WHERE enabled = TRUE;
CREATE INDEX IF NOT EXISTS idx_ai_eval_result_case ON ai_eval_result(eval_case_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_eval_result_run ON ai_eval_result(run_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_eval_run_project ON ai_eval_run(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_eval_run_status ON ai_eval_run(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_task_type_status ON ai_task(task_type, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_task_biz ON ai_task(biz_id, biz_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_task_log_task ON ai_task_log(task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_model_config_type_default ON ai_model_config(model_type, default_config) WHERE deleted = FALSE AND enabled = TRUE;
CREATE INDEX IF NOT EXISTS idx_ai_model_config_provider ON ai_model_config(provider, model_type) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_type_mode ON ai_prompt_template(template_type, mode, default_template) WHERE deleted = FALSE AND enabled = TRUE;

INSERT INTO ai_project(project_code, project_name, customer_name, description)
VALUES ('DEMO_WATER', '示例智慧水务项目', '示例水务集团', '用于本地演示、问答评测和回归测试的初始化项目。')
ON CONFLICT (project_code) DO NOTHING;

INSERT INTO ai_api(project_id, api_name, module_name, method, path, request_desc, response_desc, auth_desc, keywords)
SELECT p.id, '外部告警推送接口', '告警中心', 'POST', '/api/integration/alarms',
       '请求参数包括 alarm_code、alarm_name、alarm_level、occurred_at、device_id、auto_diagnose、auto_create_workorder。auto_diagnose=true 时触发告警诊断 Trace；auto_create_workorder=true 时命中规则后自动创建工单。',
       '返回 accepted、trace_id、workorder_id。工单创建失败时返回 error_code 和 error_message。',
       '内部系统接入使用 API Key，需在接口网关配置调用方。',
       '外部告警 推送 接口 auto_diagnose auto_create_workorder 工单创建失败'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_api a WHERE a.project_id = p.id AND a.path = '/api/integration/alarms');

INSERT INTO ai_page(project_id, page_name, module_name, route_path, operation_desc, business_rule, keywords)
SELECT p.id, '告警诊断 Trace', '告警中心', '/alarm/trace',
       '进入告警中心，打开告警详情，切换到“诊断 Trace”页签，可查看规则命中、接口调用、自动建单结果和失败原因。',
       '只有开启 auto_diagnose 或规则引擎触发诊断时才生成 Trace。',
       '告警诊断 Trace 查看 自动建单 失败原因'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_page a WHERE a.project_id = p.id AND a.route_path = '/alarm/trace');

INSERT INTO ai_page(project_id, page_name, module_name, route_path, operation_desc, business_rule, keywords)
SELECT p.id, '文档切片查看', '知识库', '/documents',
       '在文档知识库页面点击“查看切片”，可以查看文档解析后的 chunk 列表、chunk 序号、索引状态和原文内容。',
       '文档必须解析成功后才会生成 chunk。',
       '文档 chunk 查看 切片 知识库'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_page a WHERE a.project_id = p.id AND a.route_path = '/documents' AND a.page_name = '文档切片查看');

INSERT INTO ai_capability(project_id, capability_name, module_name, support_level, config_required, description, limitation, keywords)
SELECT p.id, '外部告警接入与自动建单', '告警中心', 'config_supported', TRUE,
       '支持外部系统通过告警推送接口接入二供泵房告警，并按规则自动诊断、自动创建工单。',
       '需要先配置告警类型映射、工单模板和责任班组；未知告警类型不会自动建单。',
       '二供泵房 告警 自动建单 外部接入 auto_create_workorder'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_capability c WHERE c.project_id = p.id AND c.capability_name = '外部告警接入与自动建单');

INSERT INTO ai_db_table(project_id, table_name, table_comment, module_name, field_desc, relation_desc, keywords)
SELECT p.id, 'biz_workorder', '业务工单主表', '工单中心',
       '[{"field":"id","desc":"工单ID"},{"field":"source_alarm_id","desc":"来源告警ID"},{"field":"create_status","desc":"创建状态"},{"field":"fail_reason","desc":"工单创建失败原因"}]'::jsonb,
       'source_alarm_id 关联告警记录；失败时 fail_reason 记录规则缺失、班组未配置或接口校验失败。',
       '工单 创建失败 fail_reason source_alarm_id'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_db_table t WHERE t.project_id = p.id AND t.table_name = 'biz_workorder');

INSERT INTO ai_requirement_case(project_id, case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords)
SELECT p.id, '二供泵房告警接入并自动建单', '告警中心',
       '客户希望接入二供泵房告警并在满足规则时自动创建工单。',
       '复用外部告警推送接口，配置告警类型映射、工单模板和责任班组，开启 auto_diagnose 与 auto_create_workorder。',
       'B', '低', '需要确认告警类型映射、工单模板和责任班组配置。',
       '二供泵房 告警 自动建单 可行性'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_requirement_case c WHERE c.project_id = p.id AND c.case_title = '二供泵房告警接入并自动建单');

INSERT INTO ai_requirement_case(project_id, case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords)
SELECT p.id, '水质异常自动派单', '告警中心',
       '客户希望水质异常后自动派发处理工单。',
       '可复用告警接入和工单创建能力，但需要补充水质指标映射、阈值规则和责任班组配置。',
       'C', '中', '水质指标来源、阈值规则和责任班组配置不完整时不能直接上线。',
       '水质异常 自动派单 二次开发 规则适配'
FROM ai_project p
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_requirement_case c WHERE c.project_id = p.id AND c.case_title = '水质异常自动派单');

INSERT INTO ai_eval_case(
    project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level,
    expected_keywords, expected_source_titles, expected_source_types, expected_refusal, expected_answer_type,
    tags, difficulty, remark
)
SELECT p.id, v.question, v.expected_answer, '[]'::jsonb, v.expected_mode, v.expected_level,
       v.expected_keywords, v.expected_source_titles, v.expected_source_types, v.expected_refusal,
       v.expected_answer_type, v.tags, v.difficulty, v.remark
FROM ai_project p
CROSS JOIN (
    VALUES
    ('外部告警推送接口怎么用？', '说明接口路径、请求参数和返回 trace_id。', 'api', NULL, '["/api/integration/alarms","auto_diagnose","auto_create_workorder"]'::jsonb, '["外部告警推送接口"]'::jsonb, '["API"]'::jsonb, FALSE, 'factual', '["api","alarm","regression"]'::jsonb, 'medium', '接口说明类问题'),
    ('auto_diagnose 参数有什么作用？', '说明该参数会触发告警诊断 Trace。', 'api', NULL, '["auto_diagnose","诊断","Trace"]'::jsonb, '["外部告警推送接口","告警诊断 Trace"]'::jsonb, '["API","PAGE"]'::jsonb, FALSE, 'factual', '["api","parameter"]'::jsonb, 'easy', '接口参数评测'),
    ('auto_create_workorder 是干什么的？', '说明该参数用于满足规则时自动创建工单。', 'api', NULL, '["auto_create_workorder","自动创建工单","规则"]'::jsonb, '["外部告警推送接口","外部告警接入与自动建单"]'::jsonb, '["API","CAPABILITY"]'::jsonb, FALSE, 'factual', '["api","workorder"]'::jsonb, 'easy', '接口参数评测'),
    ('告警诊断 Trace 在哪里看？', '说明从告警详情进入诊断 Trace 页签查看。', 'page_help', NULL, '["告警详情","诊断 Trace","页签"]'::jsonb, '["告警诊断 Trace"]'::jsonb, '["PAGE"]'::jsonb, FALSE, 'operation', '["page","alarm"]'::jsonb, 'easy', '页面操作评测'),
    ('客户想接入二供泵房告警并自动建单，能不能做？', '判断可通过配置支持。', 'requirement_check', 'B', '["二供泵房","告警","自动建单","配置"]'::jsonb, '["外部告警接入与自动建单","二供泵房告警接入并自动建单"]'::jsonb, '["CAPABILITY","REQUIREMENT_CASE"]'::jsonb, FALSE, 'feasibility', '["requirement","alarm"]'::jsonb, 'medium', '需求可行性评测'),
    ('客户想接入水质异常自动派单，能不能做？', '判断需要配置和规则适配。', 'requirement_check', 'C', '["水质异常","自动派单","规则适配"]'::jsonb, '["水质异常自动派单"]'::jsonb, '["REQUIREMENT_CASE"]'::jsonb, FALSE, 'feasibility', '["requirement","water_quality"]'::jsonb, 'hard', '需求可行性评测'),
    ('工单创建失败可能是什么原因？', '说明规则缺失、班组未配置或接口校验失败。', 'business_qa', NULL, '["规则缺失","班组未配置","接口校验失败"]'::jsonb, '["业务工单主表","告警诊断 Trace"]'::jsonb, '["DB_TABLE","PAGE"]'::jsonb, FALSE, 'diagnosis', '["workorder","failure"]'::jsonb, 'medium', '业务规则评测'),
    ('页面上怎么查看文档 chunk？', '说明文档知识库点击查看切片。', 'page_help', NULL, '["文档知识库","查看切片","chunk"]'::jsonb, '["文档切片查看"]'::jsonb, '["PAGE"]'::jsonb, FALSE, 'operation', '["document","chunk"]'::jsonb, 'easy', '页面操作评测'),
    ('某个接口请求参数有哪些？', '说明接口参数可在接口说明中查看。', 'api', NULL, '["请求参数","接口说明"]'::jsonb, '["外部告警推送接口"]'::jsonb, '["API"]'::jsonb, FALSE, 'factual', '["api"]'::jsonb, 'medium', '泛化接口问题'),
    ('当前项目是否支持无人机巡检？', '资料不足时必须拒答。', 'requirement_check', 'D', '["当前资料不足","无法确认"]'::jsonb, '[]'::jsonb, '[]'::jsonb, TRUE, 'refusal', '["refusal","unknown"]'::jsonb, 'medium', '资料不足拒答评测')
) AS v(question, expected_answer, expected_mode, expected_level, expected_keywords, expected_source_titles, expected_source_types, expected_refusal, expected_answer_type, tags, difficulty, remark)
WHERE p.project_code = 'DEMO_WATER'
  AND NOT EXISTS (SELECT 1 FROM ai_eval_case c WHERE c.project_id = p.id AND c.question = v.question);

UPDATE ai_eval_case
SET expected_refusal = TRUE,
    expected_keywords = '["当前资料不足","无法确认"]'::jsonb,
    expected_source_titles = '[]'::jsonb,
    expected_source_types = '[]'::jsonb,
    expected_feasibility_level = CASE WHEN expected_mode = 'requirement_check' THEN 'D' ELSE expected_feasibility_level END,
    expected_answer_type = 'refusal',
    tags = CASE WHEN tags = '[]'::jsonb THEN '["refusal","unknown"]'::jsonb ELSE tags END,
    updated_at = now()
WHERE question IN ('当前项目是否支持无人机巡检？', '当前项目是否支持区块链存证？', '当前项目是否支持水厂药耗优化算法？');

-- 只有安装 pgvector 时才添加真实向量列，避免第一阶段环境因为缺少扩展而无法初始化。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        -- 默认维度为 1536；更换 embedding 模型时，EMBEDDING_DIMENSION 必须与此列维度一致。
        ALTER TABLE ai_document_chunk ADD COLUMN IF NOT EXISTS embedding VECTOR(1536);
        BEGIN
            CREATE INDEX IF NOT EXISTS idx_ai_document_chunk_embedding_ivfflat
                ON ai_document_chunk USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100)
                WHERE deleted = FALSE AND embedding IS NOT NULL;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'pgvector ivfflat index skipped: %', SQLERRM;
        END;
    END IF;
END $$;
