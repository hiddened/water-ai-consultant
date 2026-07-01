WITH project AS (
    INSERT INTO ai_project(project_code, project_name, customer_name, industry, description, status, enabled)
    VALUES ('DEMO_WATER', '示例智慧水务项目', '三亚示例水务公司', 'water', '用于验证 AI 问答和需求可行性分析的示例项目。', 'active', TRUE)
    ON CONFLICT (project_code) DO UPDATE
        SET project_name = EXCLUDED.project_name,
            customer_name = EXCLUDED.customer_name,
            description = EXCLUDED.description,
            updated_at = now(),
            enabled = TRUE,
            deleted = FALSE
    RETURNING id
)
INSERT INTO ai_document(project_id, document_name, module_name, document_type, file_path, parse_status, enabled)
SELECT id, '项目管理操作手册', '项目管理', 'manual', 'storage/demo/project-manual.md', 'ready', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_document d WHERE d.project_id = project.id AND d.document_name = '项目管理操作手册' AND d.deleted = FALSE
);

WITH doc AS (
    SELECT d.id AS document_id, d.project_id, d.document_name
    FROM ai_document d
    JOIN ai_project p ON p.id = d.project_id
    WHERE p.project_code = 'DEMO_WATER' AND d.document_name = '项目管理操作手册' AND d.deleted = FALSE
)
INSERT INTO ai_document_chunk(project_id, document_id, document_title, chunk_index, chunk_no, section_title, content, source_locator, keywords)
SELECT project_id, document_id, document_name, 0, 0, '项目新增',
       '项目管理页面支持新增水务客户项目。用户点击新增后需要填写项目编码、项目名称、客户名称、行业和状态。项目编码必须唯一，保存后项目默认启用，可用于后续文档、页面、接口和需求案例归档。',
       'storage/demo/project-manual.md#chunk-0',
       '项目管理 新增 项目编码 客户名称'
FROM doc
WHERE NOT EXISTS (
    SELECT 1 FROM ai_document_chunk c WHERE c.document_id = doc.document_id AND c.chunk_no = 0 AND c.deleted = FALSE
);

WITH doc AS (
    SELECT d.id AS document_id, d.project_id, d.document_name
    FROM ai_document d
    JOIN ai_project p ON p.id = d.project_id
    WHERE p.project_code = 'DEMO_WATER' AND d.document_name = '项目管理操作手册' AND d.deleted = FALSE
)
INSERT INTO ai_document_chunk(project_id, document_id, document_title, chunk_index, chunk_no, section_title, content, source_locator, keywords)
SELECT project_id, document_id, document_name, 1, 1, '启用状态',
       '项目禁用后不再作为智能问答和需求可行性分析的默认资料来源。已删除项目不在列表中显示，也不能继续新增文档知识。',
       'storage/demo/project-manual.md#chunk-1',
       '启用 禁用 删除 知识来源'
FROM doc
WHERE NOT EXISTS (
    SELECT 1 FROM ai_document_chunk c WHERE c.document_id = doc.document_id AND c.chunk_no = 1 AND c.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_page(project_id, page_name, module_name, route_path, operation_desc, business_rule, keywords, enabled)
SELECT id, '项目管理列表页', '项目管理', '/projects',
       '进入项目管理列表页后点击新增按钮，填写项目编码、项目名称、客户名称和状态，保存后项目进入启用状态。',
       '项目编码必须唯一；已删除项目不在列表显示；禁用项目不能作为问答和需求分析的资料来源。',
       '项目管理 新增项目 项目编码 启用状态', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_page p WHERE p.project_id = project.id AND p.page_name = '项目管理列表页' AND p.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_page(project_id, page_name, module_name, route_path, operation_desc, business_rule, keywords, enabled)
SELECT id, '巡检任务页面', '巡检管理', '/inspection/tasks',
       '巡检任务页面支持创建水厂巡检任务、分配处理人员、查看任务处理状态。',
       '巡检任务必须关联水厂或泵站；任务状态包括待处理、处理中、已完成。',
       '巡检任务 水厂 处理状态', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_page p WHERE p.project_id = project.id AND p.page_name = '巡检任务页面' AND p.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_capability(project_id, capability_name, module_name, support_level, config_required, description, limitation, keywords, enabled)
SELECT id, '项目基础信息维护', '项目管理', 'supported', FALSE,
       '系统已支持维护项目编码、项目名称、客户名称、行业、状态和启用标识。',
       '项目编码全局唯一，不能重复。',
       '项目管理 项目编码 客户名称 启用', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_capability c WHERE c.project_id = project.id AND c.capability_name = '项目基础信息维护' AND c.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_capability(project_id, capability_name, module_name, support_level, config_required, description, limitation, keywords, enabled)
SELECT id, '水厂巡检任务管理', '巡检管理', 'configurable', TRUE,
       '系统具备巡检任务管理能力，可通过配置任务类型、处理状态和责任人实现水厂巡检任务流转。',
       '如需复杂移动端定位、离线巡检或自定义审批流，需要二次开发评估。',
       '水厂 巡检任务 处理状态 配置', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_capability c WHERE c.project_id = project.id AND c.capability_name = '水厂巡检任务管理' AND c.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_api(project_id, api_name, module_name, method, path, request_desc, response_desc, auth_desc, status, keywords, enabled)
SELECT id, '创建巡检任务接口', '巡检管理', 'POST', '/api/inspection/tasks',
       '请求字段包括 plant_id、task_name、assignee_id、due_time。',
       '返回任务 id、任务状态和创建时间。',
       '需要内部系统登录态。',
       'active',
       '巡检任务 创建接口 plant_id task_status', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_api a WHERE a.project_id = project.id AND a.api_name = '创建巡检任务接口' AND a.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_db_table(project_id, table_name, table_comment, module_name, field_desc, relation_desc, keywords, enabled)
SELECT id, 'inspection_task', '巡检任务表', '巡检管理',
       '[{"name":"id","desc":"任务ID"},{"name":"plant_id","desc":"水厂ID"},{"name":"task_status","desc":"任务处理状态"},{"name":"assignee_id","desc":"处理人ID"}]'::jsonb,
       'plant_id 关联水厂基础信息，assignee_id 关联用户表。',
       '巡检任务 水厂 处理状态 task_status', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_db_table t WHERE t.project_id = project.id AND t.table_name = 'inspection_task' AND t.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_requirement_case(project_id, case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords, enabled)
SELECT id, '水厂巡检任务配置案例', '巡检管理',
       '客户要求在水厂维度创建巡检任务，并查看待处理、处理中、已完成等处理状态。',
       '通过现有巡检任务管理能力配置任务类型和状态字典即可支持，接口只需按标准字段接入。',
       'B', '中',
       '需要确认水厂基础数据是否完整，接口接入时需校验处理人权限。',
       '水厂 巡检任务 处理状态 配置', TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_requirement_case r WHERE r.project_id = project.id AND r.case_title = '水厂巡检任务配置案例' AND r.deleted = FALSE
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_eval_case(project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level, enabled)
SELECT id, '当前项目是否支持无人机巡检？',
       '当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。',
       '[]'::jsonb, 'business_qa', NULL, TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_eval_case e WHERE e.project_id = project.id AND e.question = '当前项目是否支持无人机巡检？'
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_eval_case(project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level, enabled)
SELECT id, '当前项目是否支持区块链存证？',
       '当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。',
       '[]'::jsonb, 'business_qa', NULL, TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_eval_case e WHERE e.project_id = project.id AND e.question = '当前项目是否支持区块链存证？'
);

WITH project AS (SELECT id FROM ai_project WHERE project_code = 'DEMO_WATER')
INSERT INTO ai_eval_case(project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level, enabled)
SELECT id, '当前项目是否支持水厂药耗优化算法？',
       '当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。',
       '[]'::jsonb, 'business_qa', NULL, TRUE
FROM project
WHERE NOT EXISTS (
    SELECT 1 FROM ai_eval_case e WHERE e.project_id = project.id AND e.question = '当前项目是否支持水厂药耗优化算法？'
);
