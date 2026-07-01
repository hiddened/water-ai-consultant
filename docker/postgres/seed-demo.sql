-- Public demo data for water-ai-consultant. No real customer data is included.

INSERT INTO ai_project(project_code, project_name, customer_name, industry, description, status, enabled)
VALUES (
    'DEMO_ALARM',
    '智慧水务告警智能处置示范项目',
    '公开演示水务公司',
    'water',
    '用于演示智能问答、需求可行性分析、检索调试和自动评分评测的公开示例项目。',
    'active',
    TRUE
)
ON CONFLICT (project_code) DO UPDATE
SET project_name = EXCLUDED.project_name,
    customer_name = EXCLUDED.customer_name,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO ai_capability(project_id, capability_name, module_name, support_level, config_required, description, limitation, keywords, enabled)
SELECT p.id, v.capability_name, v.module_name, v.support_level, v.config_required, v.description, v.limitation, v.keywords, TRUE
FROM ai_project p
CROSS JOIN (
    VALUES
    ('外部告警推送', '告警中心', 'supported', TRUE, '支持外部系统通过标准接口推送告警事件，包含告警编码、等级、设备、发生时间和扩展参数。', '需要配置调用方鉴权和告警类型映射。', '外部告警 推送 接入 告警中心'),
    ('告警自动诊断', '告警中心', 'configurable', TRUE, '支持根据告警类型、设备、阈值和历史事件生成自动诊断 Trace。', '诊断规则需要按项目配置，未知告警类型仅记录原始事件。', 'auto_diagnose 告警诊断 Trace'),
    ('告警自动生成工单', '工单中心', 'configurable', TRUE, '支持告警命中规则后自动创建处理工单，并返回 workorder_id。', '需要配置工单模板、责任班组和告警等级映射。', 'auto_create_workorder 自动建单 工单'),
    ('告警 Trace 查看', '告警中心', 'supported', FALSE, '支持在告警详情页查看诊断 Trace、规则命中、接口调用和工单创建结果。', '只有触发诊断或自动建单的告警才有完整 Trace。', 'Trace 告警详情 诊断过程'),
    ('页面操作说明管理', '知识库', 'supported', FALSE, '支持维护页面路径、操作步骤和业务规则，用于页面帮助类问答。', NULL, '页面说明 操作指导 page_help'),
    ('文档知识库问答', '知识库', 'supported', FALSE, '支持上传文档、解析文本、切片入库，并在问答时引用文档 chunk。', '文档需解析成功后才进入检索范围。', '文档知识库 文档问答 chunk 引用'),
    ('需求可行性分析', '需求管理', 'supported', FALSE, '支持优先检索能力清单和历史需求案例，输出 A-E 可行性等级。', '资料不足时必须返回 D：资料不足，无法判断。', '需求可行性 分析 A B C D E')
) AS v(capability_name, module_name, support_level, config_required, description, limitation, keywords)
WHERE p.project_code = 'DEMO_ALARM'
  AND NOT EXISTS (
      SELECT 1 FROM ai_capability c WHERE c.project_id = p.id AND c.capability_name = v.capability_name
  );

INSERT INTO ai_page(project_id, page_name, module_name, route_path, operation_desc, business_rule, keywords, enabled)
SELECT p.id, v.page_name, v.module_name, v.route_path, v.operation_desc, v.business_rule, v.keywords, TRUE
FROM ai_project p
CROSS JOIN (
    VALUES
    ('告警诊断页', '告警中心', '/alarm/trace', '进入告警详情，切换到“诊断 Trace”页签，可查看规则命中、接口调用、自动建单结果和失败原因。', '未开启 auto_diagnose 的告警只显示基础事件信息。', '告警诊断 Trace 告警详情'),
    ('文档知识库页', '知识库', '/documents', '在文档知识库上传文档后，点击解析生成切片；点击查看切片可查看 chunk 原文和索引状态。', '解析成功后 chunk 才会进入智能问答检索。', '文档知识库 上传 解析 切片 chunk'),
    ('检索调试页', '知识库质量', '/search-debug', '选择项目、输入问题和 top_k，点击开始检索查看文档 chunk、页面、能力、接口、数据表和历史需求案例命中情况。', '该页面只做检索，不调用大模型。', '检索调试 search debug top_k'),
    ('问答评测页', '知识库质量', '/eval', '维护评测问题、期望关键词和期望来源，可单条运行或批量运行自动评分评测。', '自动评分不替代人工复核，低分用例需要人工检查。', '问答评测 自动评分 回归'),
    ('需求可行性分析页', '需求管理', '/requirements/check', '输入客户需求后，系统优先检索能力清单和历史需求案例，输出可行性等级、风险点和建议方案。', '只有命中能力清单和类似历史需求时，才能判断为 A/B/C。', '需求可行性 分析 风险 建议')
) AS v(page_name, module_name, route_path, operation_desc, business_rule, keywords)
WHERE p.project_code = 'DEMO_ALARM'
  AND NOT EXISTS (
      SELECT 1 FROM ai_page pg WHERE pg.project_id = p.id AND pg.route_path = v.route_path
  );

INSERT INTO ai_api(project_id, api_name, module_name, method, path, request_desc, response_desc, auth_desc, status, keywords, enabled)
SELECT p.id, v.api_name, v.module_name, v.method, v.path, v.request_desc, v.response_desc, v.auth_desc, 'active', v.keywords, TRUE
FROM ai_project p
CROSS JOIN (
    VALUES
    ('智能问答接口', '智能问答', 'POST', '/api/chat', '请求字段：project_id、mode、question。mode 支持 doc_qa、page_help、business_qa、requirement_check。', '返回 answer、references、related_pages、related_capabilities、related_apis、confidence、trace_id。', '内部系统访问。', '智能问答 chat references'),
    ('需求可行性分析接口', '需求管理', 'POST', '/api/requirements/check', '请求字段：project_id、requirement_desc、module_name。', '返回 requirement_understanding、feasibility_level、conclusion、matched_capabilities、risk_points、recommended_solution、references。', '内部系统访问。', '需求分析 feasibility_level requirement_check'),
    ('文档上传接口', '知识库', 'POST', '/api/documents/upload', 'multipart/form-data，包含 project_id、module_name、document_type、file。', '返回文档记录、chunk_count 和异步解析 task_id。', '内部系统访问。', '文档上传 upload parse chunk'),
    ('检索调试接口', '知识库质量', 'POST', '/api/search/debug', '请求字段：project_id、question、mode、top_k。', '返回 search_strategy、chunks、related_pages、related_capabilities、related_apis、related_tables、requirement_cases。', '内部系统访问。', '检索调试 search_strategy chunks'),
    ('批量评测接口', '知识库质量', 'POST', '/api/eval-cases/run-batch', '请求字段：project_id、tags、expected_mode。', '返回评测运行 ai_eval_run，结果可在 /api/eval-runs/{id}/results 查询。', '内部系统访问。', '批量评测 run-batch auto_score')
) AS v(api_name, module_name, method, path, request_desc, response_desc, auth_desc, keywords)
WHERE p.project_code = 'DEMO_ALARM'
  AND NOT EXISTS (
      SELECT 1 FROM ai_api a WHERE a.project_id = p.id AND a.path = v.path
  );

INSERT INTO ai_requirement_case(project_id, case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords, enabled)
SELECT p.id, v.case_title, v.module_name, v.requirement_desc, v.solution_desc, v.feasibility_level, v.workload_level, v.risk_points, v.keywords, TRUE
FROM ai_project p
CROSS JOIN (
    VALUES
    ('二供泵房告警接入并自动建单', '告警中心', '客户希望接入二供泵房告警，告警推送后自动诊断并创建工单。', '复用外部告警推送、告警自动诊断和自动生成工单能力，配置告警类型映射、工单模板和责任班组即可支持。', 'B', '低', '需要确认告警来源字段、责任班组和工单模板配置。', '二供泵房 告警 自动建单'),
    ('水质异常告警自动派单', '告警中心', '客户希望水质异常时自动生成处理工单，并通知责任人员。', '可复用告警接入和工单创建能力，但需要补充水质指标映射、阈值规则和派单策略。', 'C', '中', '水质指标来源和阈值规则需要二次适配。', '水质异常 自动派单 工单'),
    ('外部系统告警推送接入', '告警中心', '第三方平台希望通过接口推送告警事件到本系统。', '使用 POST /api/documents/upload 之外的业务接入接口示例时，应复用外部告警推送规范并配置 API Key。', 'B', '低', '需要进行接口联调和字段映射校验。', '外部系统 告警推送 API Key')
) AS v(case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords)
WHERE p.project_code = 'DEMO_ALARM'
  AND NOT EXISTS (
      SELECT 1 FROM ai_requirement_case r WHERE r.project_id = p.id AND r.case_title = v.case_title
  );

INSERT INTO ai_eval_case(
    project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level,
    expected_keywords, expected_source_titles, expected_source_types, expected_refusal, expected_answer_type,
    tags, difficulty, remark, enabled
)
SELECT p.id, v.question, v.expected_answer, '[]'::jsonb, v.expected_mode, v.expected_level,
       v.expected_keywords, v.expected_source_titles, v.expected_source_types, v.expected_refusal,
       v.expected_answer_type, v.tags, v.difficulty, v.remark, TRUE
FROM ai_project p
CROSS JOIN (
    VALUES
    ('外部告警推送接口怎么用？', '说明接口请求字段、鉴权和返回 trace_id。', 'api', NULL, '["外部告警","推送","trace_id"]'::jsonb, '["外部告警推送","智能问答接口"]'::jsonb, '["CAPABILITY","API"]'::jsonb, FALSE, 'factual', '["demo","api","alarm"]'::jsonb, 'easy', '演示接口问答'),
    ('auto_diagnose 参数有什么作用？', '说明开启后会触发告警自动诊断并生成 Trace。', 'business_qa', NULL, '["auto_diagnose","告警自动诊断","Trace"]'::jsonb, '["告警自动诊断","告警诊断页"]'::jsonb, '["CAPABILITY","PAGE"]'::jsonb, FALSE, 'factual', '["demo","alarm"]'::jsonb, 'easy', '演示参数解释'),
    ('auto_create_workorder 是干什么的？', '说明命中规则后自动创建工单。', 'business_qa', NULL, '["auto_create_workorder","自动生成工单","工单"]'::jsonb, '["告警自动生成工单"]'::jsonb, '["CAPABILITY"]'::jsonb, FALSE, 'factual', '["demo","workorder"]'::jsonb, 'easy', '演示参数解释'),
    ('客户想接入二供泵房告警并自动建单，能不能做？', '判断为配置即可支持。', 'requirement_check', 'B', '["二供泵房","告警","自动建单","配置"]'::jsonb, '["二供泵房告警接入并自动建单","告警自动生成工单"]'::jsonb, '["REQUIREMENT_CASE","CAPABILITY"]'::jsonb, FALSE, 'feasibility', '["demo","requirement"]'::jsonb, 'medium', '演示需求分析'),
    ('当前项目是否支持无人机巡检？', '资料不足时必须拒答。', 'business_qa', NULL, '["当前资料不足","无法确认"]'::jsonb, '[]'::jsonb, '[]'::jsonb, TRUE, 'refusal', '["demo","refusal"]'::jsonb, 'medium', '演示资料不足拒答')
) AS v(question, expected_answer, expected_mode, expected_level, expected_keywords, expected_source_titles, expected_source_types, expected_refusal, expected_answer_type, tags, difficulty, remark)
WHERE p.project_code = 'DEMO_ALARM'
  AND NOT EXISTS (
      SELECT 1 FROM ai_eval_case e WHERE e.project_id = p.id AND e.question = v.question
  );
