<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

type ApiResponse<T> = {
  code: number
  message: string
  data: T | null
  trace_id: string
  timestamp: string
}

type HealthData = {
  status: string
  service: string
  time: string
}

type DataRecord = Record<string, unknown>
type FormValue = string | number | boolean | null | undefined

type FieldConfig = {
  key: string
  label: string
  type?: 'text' | 'textarea' | 'number' | 'checkbox' | 'select' | 'project'
  required?: boolean
  options?: string[]
}

type ModuleConfig = {
  key: string
  title: string
  endpoint: string
  primaryField: string
  columns: string[]
  fields: FieldConfig[]
  requiresProject?: boolean
}

type ReferenceItem = {
  source_type: string
  source_id: string
  title?: string
  source_title: string
  source_locator: string
  chunk_id?: string | null
  chunk_index?: number | null
  document_id?: string | null
  document_title?: string | null
  content_preview?: string | null
  content?: string | null
  module_name?: string | null
  score_type?: string | null
  index_status?: string | null
  quote: string
  score: number
}

type ChatResult = {
  answer: string
  references: ReferenceItem[]
  related_pages: DataRecord[]
  related_capabilities: DataRecord[]
  related_apis: DataRecord[]
  confidence: number
  trace_id: string
  session_id: string
  message_id: string
  llm_provider: string
  model_provider: string
  model_name: string
  prompt_template_id?: string | null
  prompt_template_name?: string | null
  search_strategy: string
  insufficient_answer: boolean
}

type RequirementResult = {
  requirement_understanding: string
  feasibility_level: string
  conclusion: string
  matched_capabilities: DataRecord[]
  missing_capabilities: string[]
  related_pages: DataRecord[]
  related_apis: DataRecord[]
  related_tables: DataRecord[]
  impact_modules: string[]
  risk_points: string[]
  recommended_solution: string
  workload_level: string
  references: ReferenceItem[]
  trace_id: string
  session_id: string
  message_id: string
  llm_provider: string
  model_provider: string
  model_name: string
  prompt_template_id?: string | null
  prompt_template_name?: string | null
  search_strategy: string
  insufficient_answer: boolean
}

type DocumentChunk = {
  id: string
  document_id: string
  document_title: string
  chunk_index: number
  page_number?: number | null
  section_title?: string | null
  content: string
  source_locator: string
  index_status?: string | null
  index_error?: string | null
}

type DocumentUploadResponse = {
  document: DataRecord
  chunk_count: number
  task_id?: string | null
}

type TaskResponse = {
  task_id: string
  status: string
  message: string
}

type TaskRecord = {
  id: string
  task_type: string
  biz_id?: string | null
  biz_type?: string | null
  status: string
  progress: number
  message?: string | null
  error_message?: string | null
  started_at?: string | null
  finished_at?: string | null
  created_at?: string
  updated_at?: string
}

type TaskLog = {
  id: string
  task_id: string
  log_level: string
  message: string
  created_at: string
}

type SearchDebugHit = {
  source_type: string
  source_id: string
  title: string
  score: number
  content_preview: string
  content: string
  source_locator: string
  project_id?: string
  project_name?: string
  module_name?: string
}

type SearchDebugChunk = {
  chunk_id: string
  document_id: string
  document_title: string
  chunk_index: number
  score: number
  score_type: string
  content_preview: string
  content: string
  source_type: string
  page_number?: number | null
  section_title?: string | null
  index_status?: string | null
  module_name?: string | null
  source_locator: string
}

type SearchDebugResult = {
  query: string
  search_strategy: string
  embedding_provider: string
  vector_enabled: boolean
  keyword_fallback_used: boolean
  total_hits: number
  chunks: SearchDebugChunk[]
  related_pages: SearchDebugHit[]
  related_capabilities: SearchDebugHit[]
  related_apis: SearchDebugHit[]
  related_tables: SearchDebugHit[]
  requirement_cases: SearchDebugHit[]
}

type EvalResult = {
  id: string
  run_id?: string | null
  actual_answer: string
  actual_references: ReferenceItem[] | string
  actual_feasibility_level?: string | null
  auto_score?: number | null
  keyword_score?: number | null
  source_score?: number | null
  refusal_score?: number | null
  feasibility_score?: number | null
  reference_count?: number | null
  matched_keywords?: string[] | string
  matched_sources?: string[] | string
  missing_keywords?: string[] | string
  missing_sources?: string[] | string
  auto_passed?: boolean | null
  manual_passed?: boolean | null
  model_provider?: string | null
  model_name?: string | null
  prompt_template_name?: string | null
  search_strategy?: string | null
  duration_ms?: number | null
  error_message?: string | null
  passed?: boolean | null
  score?: number | null
  remark?: string | null
  created_at?: string
}

type EvalRunRecord = {
  id: string
  run_name: string
  project_id?: string | null
  project_name?: string | null
  status: string
  total_cases: number
  success_cases: number
  failed_cases: number
  average_score: number
  pass_rate: number
  model_provider?: string | null
  model_name?: string | null
  prompt_template_name?: string | null
  search_strategy?: string | null
  started_at?: string | null
  finished_at?: string | null
  created_at?: string
  remark?: string | null
}

type ChatSessionRecord = {
  session_id: string
  project_id: string
  project_name?: string
  mode: string
  question_count: number
  last_question?: string
  last_answer_preview?: string
  created_at?: string
  updated_at?: string
}

type ChatMessageRecord = {
  id: string
  session_id: string
  project_id: string
  role: string
  content: string
  confidence?: number | null
  trace_id?: string | null
  llm_provider?: string | null
  model_provider?: string | null
  model_name?: string | null
  prompt_template_id?: string | null
  prompt_template_name?: string | null
  search_strategy?: string | null
  insufficient_answer?: boolean
  created_at?: string
  references: ReferenceItem[]
}

type FeedbackRecord = {
  id: string
  project_id: string
  project_name?: string
  session_id: string
  message_id: string
  feedback_type: string
  remark?: string | null
  corrected_answer?: string | null
  expected_sources?: unknown
  convert_to_knowledge?: boolean
  target_knowledge_type?: string | null
  review_status: string
  reviewer?: string | null
  review_remark?: string | null
  question?: string | null
  answer?: string | null
  trace_id?: string | null
  llm_provider?: string | null
  model_provider?: string | null
  model_name?: string | null
  prompt_template_id?: string | null
  prompt_template_name?: string | null
  search_strategy?: string | null
  references?: ReferenceItem[]
  created_at?: string
  updated_at?: string
}

type ModelConfigRecord = {
  id: string
  config_name: string
  provider: string
  model_type: string
  base_url?: string | null
  api_key_masked?: string | null
  model_name?: string | null
  dimension?: number | null
  temperature?: number | null
  max_tokens?: number | null
  enabled: boolean
  default_config: boolean
  remark?: string | null
  created_at?: string
  updated_at?: string
}

type PromptTemplateRecord = {
  id: string
  template_name: string
  template_type: string
  mode?: string | null
  system_prompt?: string | null
  user_prompt_template: string
  output_format?: string | null
  enabled: boolean
  default_template: boolean
  remark?: string | null
  created_at?: string
  updated_at?: string
}

const navItems = [
  { key: 'dashboard', label: 'Dashboard' },
  { key: 'chat', label: '智能问答' },
  { key: 'requirement-check', label: '需求可行性分析' },
  { key: 'search-debug', label: '检索调试' },
  { key: 'eval', label: '问答评测' },
  { key: 'eval-runs', label: '评测运行看板' },
  { key: 'model-configs', label: '模型配置' },
  { key: 'prompt-templates', label: 'Prompt 模板' },
  { key: 'chat-history', label: '对话记录' },
  { key: 'feedback', label: '反馈管理' },
  { key: 'tasks', label: '任务中心' },
  { key: 'projects', label: '项目管理' },
  { key: 'documents', label: '文档知识库' },
  { key: 'pages', label: '页面说明' },
  { key: 'capabilities', label: '能力清单' },
  { key: 'apis', label: '接口说明' },
  { key: 'db-tables', label: '数据表说明' },
  { key: 'requirement-cases', label: '历史需求案例' }
]

const navGroups = [
  { label: '总览', items: navItems.filter(item => ['dashboard'].includes(item.key)) },
  { label: '智能交互', items: navItems.filter(item => ['chat', 'requirement-check', 'search-debug'].includes(item.key)) },
  { label: '评测与监控', items: navItems.filter(item => ['eval', 'eval-runs', 'tasks'].includes(item.key)) },
  { label: '配置中心', items: navItems.filter(item => ['model-configs', 'prompt-templates'].includes(item.key)) },
  { label: '数据与记录', items: navItems.filter(item => ['chat-history', 'feedback', 'projects', 'documents'].includes(item.key)) }
]

const health = ref<ApiResponse<HealthData> | null>(null)
const loading = ref(true)
const error = ref('')
const flowCanvas = ref<HTMLCanvasElement | null>(null)
const activeKey = ref('dashboard')
const records = ref<DataRecord[]>([])
const projects = ref<DataRecord[]>([])
const dashboardDocuments = ref<DataRecord[]>([])
const dashboardTasks = ref<TaskRecord[]>([])
const dashboardEvalCases = ref<DataRecord[]>([])
const dashboardLoading = ref(false)
const dashboardError = ref('')
const dataLoading = ref(false)
const dataError = ref('')
const modalOpen = ref(false)
const editingRecord = ref<DataRecord | null>(null)
const formState = reactive<Record<string, FormValue>>({})
const uploadLoading = ref(false)
const uploadError = ref('')
const uploadResult = ref<DocumentUploadResponse | null>(null)
const chunkModalOpen = ref(false)
const chunkLoading = ref(false)
const chunkError = ref('')
const documentChunks = ref<DocumentChunk[]>([])
const chunkDocumentTitle = ref('')
const taskRecords = ref<TaskRecord[]>([])
const taskLogs = ref<TaskLog[]>([])
const taskLoading = ref(false)
const taskError = ref('')
const taskModalOpen = ref(false)
const activeTask = ref<TaskRecord | null>(null)
const taskFilters = reactive({
  task_type: '',
  status: '',
  biz_id: ''
})
const chatLoading = ref(false)
const chatError = ref('')
const chatResult = ref<ChatResult | null>(null)
const chatForm = reactive({
  project_id: '',
  mode: 'doc_qa',
  question: '如何新增项目？'
})
const requirementLoading = ref(false)
const requirementError = ref('')
const requirementResult = ref<RequirementResult | null>(null)
const requirementForm = reactive({
  project_id: '',
  module_name: '',
  requirement_desc: '客户希望在项目管理中支持新增水厂巡检任务，并能查看处理状态。'
})
const referenceModalOpen = ref(false)
const activeReference = ref<ReferenceItem | SearchDebugHit | SearchDebugChunk | null>(null)
const searchLoading = ref(false)
const searchError = ref('')
const searchResult = ref<SearchDebugResult | null>(null)
const searchForm = reactive({
  project_id: '',
  mode: 'doc_qa',
  question: '文档归档规则是什么？',
  top_k: 10
})
const evalCases = ref<DataRecord[]>([])
const evalLoading = ref(false)
const evalError = ref('')
const evalModalOpen = ref(false)
const editingEvalCase = ref<DataRecord | null>(null)
const evalForm = reactive({
  project_id: '',
  question: '',
  expected_answer: '',
  expected_sources: '',
  expected_mode: 'business_qa',
  expected_feasibility_level: '',
  expected_keywords: '',
  expected_source_titles: '',
  expected_source_types: '',
  expected_refusal: false,
  expected_answer_type: '',
  tags: '',
  difficulty: '',
  remark: '',
  enabled: true
})
const evalBatchFilters = reactive({
  project_id: '',
  expected_mode: '',
  tags: ''
})
const evalRuns = ref<EvalRunRecord[]>([])
const evalRunResults = ref<DataRecord[]>([])
const evalRunSummary = ref<DataRecord | null>(null)
const evalRunLoading = ref(false)
const evalRunError = ref('')
const activeEvalRun = ref<EvalRunRecord | null>(null)
const reviewModalOpen = ref(false)
const activeEvalResult = ref<EvalResult | null>(null)
const reviewForm = reactive({
  passed: true,
  score: 80,
  remark: ''
})
const chatSessions = ref<ChatSessionRecord[]>([])
const chatSessionMessages = ref<ChatMessageRecord[]>([])
const chatHistoryLoading = ref(false)
const chatHistoryError = ref('')
const activeChatSession = ref<ChatSessionRecord | null>(null)
const chatHistoryFilters = reactive({
  project_id: '',
  mode: '',
  keyword: '',
  start_time: '',
  end_time: ''
})
const feedbackRecords = ref<FeedbackRecord[]>([])
const feedbackLoading = ref(false)
const feedbackError = ref('')
const feedbackModalOpen = ref(false)
const feedbackReviewModalOpen = ref(false)
const activeFeedback = ref<FeedbackRecord | null>(null)
const feedbackContext = ref<{ project_id: string; session_id: string; message_id: string; references: ReferenceItem[] } | null>(null)
const feedbackForm = reactive({
  feedback_type: 'useful',
  remark: '',
  corrected_answer: '',
  expected_sources: '',
  convert_to_knowledge: false,
  target_knowledge_type: ''
})
const feedbackFilters = reactive({
  project_id: '',
  feedback_type: '',
  review_status: '',
  keyword: '',
  start_time: '',
  end_time: ''
})
const feedbackReviewForm = reactive({
  review_status: 'accepted',
  reviewer: '',
  review_remark: '',
  target_knowledge_type: ''
})
const modelConfigs = ref<ModelConfigRecord[]>([])
const modelConfigLoading = ref(false)
const modelConfigError = ref('')
const modelConfigModalOpen = ref(false)
const editingModelConfig = ref<ModelConfigRecord | null>(null)
const modelConfigTestResult = ref<DataRecord | null>(null)
const modelConfigFilters = reactive({
  provider: '',
  model_type: '',
  enabled: ''
})
const modelConfigForm = reactive({
  config_name: '',
  provider: 'deepseek',
  model_type: 'chat',
  base_url: 'https://api.deepseek.com',
  api_key: '',
  model_name: 'deepseek-chat',
  dimension: 1536,
  temperature: 0.2,
  max_tokens: 2048,
  enabled: true,
  default_config: false,
  remark: ''
})
const promptTemplates = ref<PromptTemplateRecord[]>([])
const promptTemplateLoading = ref(false)
const promptTemplateError = ref('')
const promptTemplateModalOpen = ref(false)
const promptTemplateTestOpen = ref(false)
const editingPromptTemplate = ref<PromptTemplateRecord | null>(null)
const activePromptTemplate = ref<PromptTemplateRecord | null>(null)
const promptTemplateTestResult = ref<DataRecord | null>(null)
const promptTemplateFilters = reactive({
  template_type: '',
  mode: '',
  enabled: ''
})
const promptTemplateForm = reactive({
  template_name: '',
  template_type: 'doc_qa',
  mode: 'doc_qa',
  system_prompt: '',
  user_prompt_template: '',
  output_format: 'markdown',
  enabled: true,
  default_template: false,
  remark: ''
})
const promptTemplateTestForm = reactive({
  question: '如何新增项目？',
  requirement_desc: '客户希望新增水厂巡检任务，并展示处理状态。',
  project_name: '示例智慧水务项目',
  mode: 'doc_qa',
  references: 'R1 文档归档规则',
  context: '上传后的文档需要解析成功并生成切片，才会进入智能问答检索范围。',
  module_name: '项目管理'
})
const filters = reactive({
  project_id: '',
  module_name: '',
  keyword: ''
})
const pageSizeOptions = [5, 10, 20, 50]
const pagination = reactive<Record<string, { page: number; pageSize: number }>>({
  records: { page: 1, pageSize: 10 },
  evalCases: { page: 1, pageSize: 10 },
  evalRuns: { page: 1, pageSize: 10 },
  evalRunResults: { page: 1, pageSize: 5 },
  chatSessions: { page: 1, pageSize: 8 },
  chatSessionMessages: { page: 1, pageSize: 6 },
  feedbackRecords: { page: 1, pageSize: 8 },
  taskRecords: { page: 1, pageSize: 10 },
  taskLogs: { page: 1, pageSize: 8 },
  documentChunks: { page: 1, pageSize: 8 },
  searchChunks: { page: 1, pageSize: 6 },
  searchPages: { page: 1, pageSize: 6 },
  searchCapabilities: { page: 1, pageSize: 6 },
  searchApis: { page: 1, pageSize: 6 },
  searchTables: { page: 1, pageSize: 6 },
  searchRequirementCases: { page: 1, pageSize: 6 },
  modelConfigs: { page: 1, pageSize: 10 },
  promptTemplates: { page: 1, pageSize: 10 }
})
const uploadForm = reactive<{
  project_id: string
  module_name: string
  document_type: string
  file: File | null
}>({
  project_id: '',
  module_name: '',
  document_type: 'manual',
  file: null
})

const crudModules: ModuleConfig[] = [
  {
    key: 'projects',
    title: '项目管理',
    endpoint: '/api/projects',
    primaryField: 'project_name',
    columns: ['project_code', 'project_name', 'customer_name', 'industry', 'status', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_code', label: '项目编码', required: true },
      { key: 'project_name', label: '项目名称', required: true },
      { key: 'customer_name', label: '客户名称' },
      { key: 'industry', label: '行业' },
      { key: 'status', label: '状态', options: ['active', 'archived'] },
      { key: 'enabled', label: '启用', type: 'checkbox' },
      { key: 'description', label: '项目说明', type: 'textarea' }
    ]
  },
  {
    key: 'documents',
    title: '文档管理',
    endpoint: '/api/documents',
    primaryField: 'document_name',
    requiresProject: true,
    columns: ['document_name', 'module_name', 'document_type', 'storage_type', 'storage_bucket', 'file_size', 'parse_status', 'index_status', 'chunk_count', 'last_parsed_at', 'last_indexed_at', 'parse_error', 'index_error', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'document_name', label: '文档名称', required: true },
      { key: 'module_name', label: '模块名称' },
      { key: 'document_type', label: '文档类型' },
      { key: 'file_path', label: '文件路径', required: true },
      { key: 'file_hash', label: '文件 Hash' },
      { key: 'file_size', label: '文件大小', type: 'number' },
      { key: 'parse_status', label: '解析状态', options: ['uploaded', 'ready', 'failed'] },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  },
  {
    key: 'pages',
    title: '页面操作说明',
    endpoint: '/api/pages',
    primaryField: 'page_name',
    requiresProject: true,
    columns: ['page_name', 'module_name', 'route_path', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'page_name', label: '页面名称', required: true },
      { key: 'module_name', label: '模块名称' },
      { key: 'route_path', label: '页面路径' },
      { key: 'operation_desc', label: '操作说明', type: 'textarea', required: true },
      { key: 'business_rule', label: '业务规则', type: 'textarea' },
      { key: 'keywords', label: '关键词' },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  },
  {
    key: 'capabilities',
    title: '系统能力清单',
    endpoint: '/api/capabilities',
    primaryField: 'capability_name',
    requiresProject: true,
    columns: ['capability_name', 'module_name', 'support_level', 'config_required', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'capability_name', label: '能力名称', required: true },
      { key: 'module_name', label: '模块名称' },
      { key: 'support_level', label: '支持级别', options: ['supported', 'configurable', 'custom_required', 'unsupported'] },
      { key: 'config_required', label: '需配置', type: 'checkbox' },
      { key: 'description', label: '能力说明', type: 'textarea', required: true },
      { key: 'limitation', label: '限制说明', type: 'textarea' },
      { key: 'keywords', label: '关键词' },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  },
  {
    key: 'apis',
    title: '接口说明管理',
    endpoint: '/api/apis',
    primaryField: 'api_name',
    requiresProject: true,
    columns: ['api_name', 'module_name', 'method', 'path', 'status', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'api_name', label: '接口名称', required: true },
      { key: 'module_name', label: '模块名称' },
      { key: 'method', label: '请求方法', options: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'] },
      { key: 'path', label: '接口路径', required: true },
      { key: 'request_desc', label: '请求说明', type: 'textarea' },
      { key: 'response_desc', label: '响应说明', type: 'textarea' },
      { key: 'auth_desc', label: '鉴权说明', type: 'textarea' },
      { key: 'status', label: '状态', options: ['active', 'deprecated'] },
      { key: 'keywords', label: '关键词' },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  },
  {
    key: 'db-tables',
    title: '数据表说明管理',
    endpoint: '/api/db-tables',
    primaryField: 'table_name',
    requiresProject: true,
    columns: ['table_name', 'table_comment', 'module_name', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'table_name', label: '表名', required: true },
      { key: 'table_comment', label: '表说明' },
      { key: 'module_name', label: '模块名称' },
      { key: 'field_desc', label: '字段 JSON', type: 'textarea' },
      { key: 'relation_desc', label: '关系说明', type: 'textarea' },
      { key: 'keywords', label: '关键词' },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  },
  {
    key: 'requirement-cases',
    title: '历史需求案例',
    endpoint: '/api/requirement-cases',
    primaryField: 'case_title',
    requiresProject: true,
    columns: ['case_title', 'module_name', 'feasibility_level', 'workload_level', 'enabled', 'updated_at'],
    fields: [
      { key: 'project_id', label: '所属项目', type: 'project', required: true },
      { key: 'case_title', label: '案例标题', required: true },
      { key: 'module_name', label: '模块名称' },
      { key: 'requirement_desc', label: '需求描述', type: 'textarea', required: true },
      { key: 'solution_desc', label: '方案说明', type: 'textarea' },
      { key: 'feasibility_level', label: '可行性等级', options: ['A', 'B', 'C', 'D', 'E'] },
      { key: 'workload_level', label: '工作量', options: ['低', '中', '高'] },
      { key: 'risk_points', label: '风险点', type: 'textarea' },
      { key: 'keywords', label: '关键词' },
      { key: 'enabled', label: '启用', type: 'checkbox' }
    ]
  }
]

const activeModule = computed(() => crudModules.find(module => module.key === activeKey.value) ?? null)
const projectOptions = computed(() => projects.value.map(project => ({
  id: String(project.id ?? ''),
  name: String(project.project_name ?? project.project_code ?? project.id ?? '')
})))
const chatRuntimeLabel = computed(() => {
  if (!chatResult.value) return '知识库问答'
  const provider = formatProviderName(chatResult.value.model_provider || chatResult.value.llm_provider)
  const strategy = formatSearchStrategy(chatResult.value.search_strategy)
  return [provider, strategy].filter(Boolean).join(' / ')
})
const chatHistorySummary = computed(() => {
  const questionCount = chatSessions.value.reduce((total, session) => total + Number(session.question_count ?? 0), 0)
  return [
    { label: '会话数', value: String(chatSessions.value.length) },
    { label: '问题数', value: String(questionCount) },
    { label: '当前详情', value: activeChatSession.value ? '已选择' : '未选择' }
  ]
})
const feedbackColumns = computed(() => [
  { key: 'pending', title: '待复核', records: feedbackRecords.value.filter(record => record.review_status === 'pending') },
  { key: 'accepted', title: '已通过', records: feedbackRecords.value.filter(record => record.review_status === 'accepted') },
  { key: 'converted', title: '已沉淀', records: feedbackRecords.value.filter(record => record.review_status === 'converted') },
  { key: 'rejected', title: '已驳回', records: feedbackRecords.value.filter(record => record.review_status === 'rejected') }
])
const feedbackSummary = computed(() => [
  { label: '反馈数', value: String(feedbackRecords.value.length) },
  { label: '待复核', value: String(feedbackRecords.value.filter(record => record.review_status === 'pending').length) },
  { label: '已沉淀', value: String(feedbackRecords.value.filter(record => record.review_status === 'converted').length) }
])
const searchDebugGroups = computed(() => {
  if (!searchResult.value) return []
  return [
    { title: '页面说明', list: searchResult.value.related_pages, pageKey: 'searchPages' },
    { title: '能力清单', list: searchResult.value.related_capabilities, pageKey: 'searchCapabilities' },
    { title: '接口说明', list: searchResult.value.related_apis, pageKey: 'searchApis' },
    { title: '数据表说明', list: searchResult.value.related_tables, pageKey: 'searchTables' },
    { title: '历史需求案例', list: searchResult.value.requirement_cases, pageKey: 'searchRequirementCases' }
  ]
})
const activeProjectId = computed(() => {
  // 顶部项目摘要跟随当前页面的项目选择，避免被其他页面残留筛选条件覆盖。
  if (activeKey.value === 'chat') return chatForm.project_id
  if (activeKey.value === 'requirement-check') return requirementForm.project_id
  if (activeKey.value === 'search-debug') return searchForm.project_id
  if (activeKey.value === 'eval') return evalForm.project_id
  if (activeKey.value === 'eval-runs') return evalBatchFilters.project_id || evalForm.project_id
  if (activeKey.value === 'chat-history') return chatHistoryFilters.project_id
  if (activeKey.value === 'feedback') return feedbackFilters.project_id
  if (activeKey.value === 'documents') return uploadForm.project_id || filters.project_id
  if (activeModule.value?.requiresProject) return filters.project_id
  return filters.project_id || chatForm.project_id || requirementForm.project_id || searchForm.project_id || evalForm.project_id || projectOptions.value[0]?.id || ''
})
const currentProjectSummary = computed(() => {
  const selectedId = activeProjectId.value
  const project = projects.value.find(item => String(item.id ?? '') === selectedId) ?? projects.value[0]
  return {
    code: String(project?.project_code ?? 'DEMO_WATER'),
    status: String(project?.status ?? health.value?.data?.status ?? 'ACTIVE').toUpperCase(),
    industry: String(project?.industry ?? 'WATER').toUpperCase(),
    updated: String(project?.updated_at ?? health.value?.data?.time ?? '-').slice(0, 10)
  }
})
const dashboardMetrics = computed(() => {
  const chunkCount = dashboardDocuments.value.reduce((total, document) => total + Number(document.chunk_count ?? 0), 0)
  const latestTask = dashboardTasks.value[0]
  const failedTasks = dashboardTasks.value.filter(task => task.status === 'failed').length
  return [
    { label: '项目数量', value: String(projects.value.length), tone: 'cyan' },
    { label: '文档数量', value: String(dashboardDocuments.value.length), tone: 'blue' },
    { label: '文档切片', value: String(chunkCount), tone: 'cyan' },
    { label: '最近任务', value: latestTask ? latestTask.status : '-', tone: latestTask?.status === 'failed' ? 'amber' : 'blue' },
    { label: '评测用例', value: String(dashboardEvalCases.value.length), tone: 'cyan' },
    { label: '异常任务', value: String(failedTasks), tone: failedTasks > 0 ? 'amber' : 'blue' }
  ]
})
const capabilityCards = [
  { title: '结构化知识库', desc: '项目、页面、能力、接口、数据表和历史需求案例统一管理。', status: '已接入' },
  { title: '文档解析切片', desc: '上传文档后提取文本、切片入库，并保留 chunk 级引用来源。', status: '异步化' },
  { title: 'AI 智能问答', desc: '基于检索上下文调用大模型，资料不足时拒答。', status: '可用' },
  { title: '需求可行性分析', desc: '能力清单和历史案例优先，输出结构化等级、风险和方案。', status: '可用' },
  { title: '引用溯源', desc: '回答返回 references，可展开原文、分数类型和索引状态。', status: '已开启' },
  { title: '检索调试', desc: '不调用模型，直接查看 chunk、页面、接口等检索命中。', status: '可排查' },
  { title: '问答评测', desc: '维护评测问题，保存实际回答和引用，人工复核评分。', status: '半自动' },
  { title: '任务中心', desc: '解析、重解析和索引任务可查看进度、错误与日志。', status: '已上线' }
]
const quickActions = [
  { label: '上传文档', key: 'documents' },
  { label: '智能问答', key: 'chat' },
  { label: '需求分析', key: 'requirement-check' },
  { label: '检索调试', key: 'search-debug' },
  { label: '任务中心', key: 'tasks' }
]

let animationFrameId = 0
let resizeObserver: ResizeObserver | null = null
let removeMouseMoveListener: (() => void) | null = null
let taskPollTimer = 0

type ShaderRuntime = {
  gl: WebGLRenderingContext
  uTime: WebGLUniformLocation | null
  uResolution: WebGLUniformLocation | null
  uMouse: WebGLUniformLocation | null
  mouse: { x: number; y: number }
}

let shaderRuntime: ShaderRuntime | null = null

const vertexShaderSource = `
attribute vec2 a_position;
varying vec2 v_texCoord;

void main() {
  v_texCoord = a_position * 0.5 + 0.5;
  gl_Position = vec4(a_position, 0.0, 1.0);
}
`

const fragmentShaderSource = `
precision highp float;

uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_mouse;
varying vec2 v_texCoord;

vec3 permute(vec3 x) {
  return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v) {
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
    -0.577350269189626, 0.024390243902439);
  vec2 i = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
    + i.x + vec3(0.0, i1.x, 1.0));
  vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy),
    dot(x12.zw, x12.zw)), 0.0);
  m = m * m;
  m = m * m;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
  vec3 g;
  g.x = a0.x * x0.x + h.x * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

void main() {
  vec2 uv = v_texCoord;
  vec2 m = u_mouse / u_resolution;

  float n1 = snoise(uv * 2.0 - u_time * 0.05);
  float n2 = snoise(uv * 4.0 + u_time * 0.08);
  float wave = (n1 + n2 * 0.5) * 0.5 + 0.5;

  float c = snoise(uv * 10.0 + wave * 2.0);
  c = pow(abs(c), 12.0);

  vec3 colorBase = vec3(0.02, 0.08, 0.12);
  vec3 colorMid = vec3(0.1, 0.4, 0.5);
  vec3 colorHigh = vec3(0.4, 0.8, 0.9);

  vec3 finalColor = mix(colorBase, colorMid, wave);
  finalColor += colorHigh * c * 0.6;

  float dist = length(uv - m);
  float ripple = sin(dist * 20.0 - u_time * 3.0) * exp(-dist * 4.0);
  finalColor += colorHigh * ripple * 0.2;

  gl_FragColor = vec4(finalColor, 1.0);
}
`

function compileShader(gl: WebGLRenderingContext, type: number, source: string) {
  const shader = gl.createShader(type)
  if (!shader) {
    throw new Error('无法创建 WebGL Shader')
  }

  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    const message = gl.getShaderInfoLog(shader) ?? 'Shader 编译失败'
    gl.deleteShader(shader)
    throw new Error(message)
  }
  return shader
}

function createShaderProgram(gl: WebGLRenderingContext) {
  const vertexShader = compileShader(gl, gl.VERTEX_SHADER, vertexShaderSource)
  const fragmentShader = compileShader(gl, gl.FRAGMENT_SHADER, fragmentShaderSource)
  const program = gl.createProgram()
  if (!program) {
    throw new Error('无法创建 WebGL Program')
  }

  gl.attachShader(program, vertexShader)
  gl.attachShader(program, fragmentShader)
  gl.linkProgram(program)
  gl.deleteShader(vertexShader)
  gl.deleteShader(fragmentShader)

  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    const message = gl.getProgramInfoLog(program) ?? 'Shader 链接失败'
    gl.deleteProgram(program)
    throw new Error(message)
  }
  return program
}

function syncShaderCanvasSize() {
  const canvas = flowCanvas.value
  const runtime = shaderRuntime
  if (!canvas || !runtime) {
    return
  }

  const width = canvas.clientWidth || window.innerWidth || 1280
  const height = canvas.clientHeight || window.innerHeight || 720
  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width
    canvas.height = height
  }
  runtime.gl.viewport(0, 0, canvas.width, canvas.height)
}

function renderShaderFrame(time: number) {
  const canvas = flowCanvas.value
  const runtime = shaderRuntime
  if (!canvas || !runtime) {
    return
  }

  syncShaderCanvasSize()
  const { gl, uMouse, uResolution, uTime, mouse } = runtime
  if (uTime) gl.uniform1f(uTime, time * 0.001)
  if (uResolution) gl.uniform2f(uResolution, canvas.width, canvas.height)
  if (uMouse) gl.uniform2f(uMouse, mouse.x, mouse.y)
  gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4)
  animationFrameId = window.requestAnimationFrame(renderShaderFrame)
}

function startFlowAnimation() {
  const canvas = flowCanvas.value
  const gl = canvas?.getContext('webgl') ?? canvas?.getContext('experimental-webgl')
  if (!canvas || !(gl instanceof WebGLRenderingContext)) {
    return
  }

  const program = createShaderProgram(gl)
  gl.useProgram(program)

  const buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW)

  const position = gl.getAttribLocation(program, 'a_position')
  gl.enableVertexAttribArray(position)
  gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0)

  shaderRuntime = {
    gl,
    uTime: gl.getUniformLocation(program, 'u_time'),
    uResolution: gl.getUniformLocation(program, 'u_resolution'),
    uMouse: gl.getUniformLocation(program, 'u_mouse'),
    mouse: { x: canvas.width / 2, y: canvas.height / 2 }
  }

  // 鼠标扰动沿用 Stitch Shader 的交互逻辑：按像素坐标传入，片元里再归一化。
  const handleMouseMove = (event: MouseEvent) => {
    if (!shaderRuntime) {
      return
    }
    const rect = canvas.getBoundingClientRect()
    if (!rect.width || !rect.height) {
      return
    }
    const nx = (event.clientX - rect.left) / rect.width
    const ny = 1 - (event.clientY - rect.top) / rect.height
    shaderRuntime.mouse.x = nx * canvas.width
    shaderRuntime.mouse.y = ny * canvas.height
  }

  window.addEventListener('mousemove', handleMouseMove)
  removeMouseMoveListener = () => window.removeEventListener('mousemove', handleMouseMove)

  resizeObserver = new ResizeObserver(syncShaderCanvasSize)
  resizeObserver.observe(canvas)
  syncShaderCanvasSize()
  animationFrameId = window.requestAnimationFrame(renderShaderFrame)
}

async function requestApi<T>(url: string, options?: RequestInit) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    ...options
  })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `HTTP ${response.status}`)
  }
  return payload.data as T
}

async function requestUpload<T>(url: string, formData: FormData) {
  const response = await fetch(url, {
    method: 'POST',
    body: formData
  })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `HTTP ${response.status}`)
  }
  return payload.data as T
}

function paginationState(key: string) {
  if (!pagination[key]) {
    pagination[key] = { page: 1, pageSize: 10 }
  }
  return pagination[key]
}

function pageTotal(total: number, key: string) {
  return Math.max(1, Math.ceil(total / paginationState(key).pageSize))
}

function currentPage(total: number, key: string) {
  return Math.min(paginationState(key).page, pageTotal(total, key))
}

function pageStart(total: number, key: string) {
  if (total === 0) return 0
  return (currentPage(total, key) - 1) * paginationState(key).pageSize + 1
}

function pageEnd(total: number, key: string) {
  return Math.min(currentPage(total, key) * paginationState(key).pageSize, total)
}

function pagedItems<T>(items: T[], key: string) {
  const state = paginationState(key)
  const page = currentPage(items.length, key)
  const start = (page - 1) * state.pageSize
  return items.slice(start, start + state.pageSize)
}

function setPage(key: string, page: number, total: number) {
  const state = paginationState(key)
  state.page = Math.min(Math.max(1, page), pageTotal(total, key))
}

function resetPage(key: string) {
  paginationState(key).page = 1
}

function buildListUrl(module: ModuleConfig) {
  const params = new URLSearchParams()
  // 项目管理本身不是项目内资源，不能继承全局 project_id，否则会被后端按项目 id 精确过滤。
  if (module.requiresProject && filters.project_id) params.set('project_id', filters.project_id)
  if (filters.module_name) params.set('module_name', filters.module_name)
  if (filters.keyword) params.set('keyword', filters.keyword)
  const query = params.toString()
  return query ? `${module.endpoint}?${query}` : module.endpoint
}

async function loadProjects() {
  try {
    projects.value = await requestApi<DataRecord[]>('/api/projects')
  } catch {
    projects.value = []
  }
}

async function loadDashboard() {
  dashboardLoading.value = true
  dashboardError.value = ''
  const [documentsResult, tasksResult, evalCasesResult] = await Promise.allSettled([
    requestApi<DataRecord[]>('/api/documents'),
    requestApi<TaskRecord[]>('/api/tasks'),
    requestApi<DataRecord[]>('/api/eval-cases')
  ])

  if (documentsResult.status === 'fulfilled') {
    dashboardDocuments.value = documentsResult.value
  } else {
    dashboardDocuments.value = []
    dashboardError.value = '文档统计加载失败'
  }

  if (tasksResult.status === 'fulfilled') {
    dashboardTasks.value = tasksResult.value.slice(0, 5)
  } else {
    dashboardTasks.value = []
    dashboardError.value = dashboardError.value || '任务统计加载失败'
  }

  dashboardEvalCases.value = evalCasesResult.status === 'fulfilled' ? evalCasesResult.value : []
  dashboardLoading.value = false
}

async function loadCurrentModule() {
  const module = activeModule.value
  if (!module) {
    return
  }

  dataLoading.value = true
  dataError.value = ''
  try {
    records.value = await requestApi<DataRecord[]>(buildListUrl(module))
    resetPage('records')
  } catch (err) {
    dataError.value = err instanceof Error ? err.message : '列表加载失败'
    records.value = []
  } finally {
    dataLoading.value = false
  }
}

function activate(key: string) {
  activeKey.value = key
}

function jumpTo(key: string) {
  activeKey.value = key
}

function resetForm(module: ModuleConfig, record?: DataRecord) {
  Object.keys(formState).forEach(key => delete formState[key])
  for (const field of module.fields) {
    if (record) {
      const value = record[field.key]
      formState[field.key] = typeof value === 'object' && value !== null ? JSON.stringify(value, null, 2) : value as FormValue ?? defaultValue(field)
    } else {
      formState[field.key] = defaultValue(field)
    }
  }
  if (!record && module.requiresProject) {
    formState.project_id = filters.project_id || projectOptions.value[0]?.id || ''
  }
}

function defaultValue(field: FieldConfig) {
  if (field.type === 'checkbox') return field.key === 'enabled'
  if (field.type === 'number') return ''
  if (field.key === 'field_desc') return '[]'
  if (field.key === 'industry') return 'water'
  if (field.key === 'status') return 'active'
  if (field.key === 'parse_status') return 'uploaded'
  if (field.key === 'support_level') return 'supported'
  if (field.key === 'method') return 'GET'
  return field.options?.[0] ?? ''
}

function openCreateModal() {
  const module = activeModule.value
  if (!module) return
  editingRecord.value = null
  resetForm(module)
  modalOpen.value = true
}

function openEditModal(record: DataRecord) {
  const module = activeModule.value
  if (!module) return
  editingRecord.value = record
  resetForm(module, record)
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

function preparePayload(module: ModuleConfig) {
  const payload: DataRecord = {}
  for (const field of module.fields) {
    const rawValue = formState[field.key]
    if (field.type === 'number') {
      payload[field.key] = rawValue === '' || rawValue == null ? null : Number(rawValue)
    } else if (field.key === 'field_desc') {
      const text = String(rawValue || '[]')
      JSON.parse(text)
      payload[field.key] = text
    } else {
      payload[field.key] = rawValue
    }
  }
  return payload
}

async function saveRecord() {
  const module = activeModule.value
  if (!module) return

  dataError.value = ''
  try {
    const payload = preparePayload(module)
    const id = editingRecord.value?.id
    await requestApi<DataRecord>(id ? `${module.endpoint}/${id}` : module.endpoint, {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    closeModal()
    await loadProjects()
    await loadCurrentModule()
  } catch (err) {
    dataError.value = err instanceof Error ? err.message : '保存失败'
  }
}

async function deleteRecord(record: DataRecord) {
  const module = activeModule.value
  if (!module || !record.id) return
  if (!window.confirm(`确认删除「${record[module.primaryField] ?? record.id}」？`)) {
    return
  }

  dataError.value = ''
  try {
    await requestApi<void>(`${module.endpoint}/${record.id}`, { method: 'DELETE' })
    await loadProjects()
    await loadCurrentModule()
  } catch (err) {
    dataError.value = err instanceof Error ? err.message : '删除失败'
  }
}

function handleUploadFile(event: Event) {
  const target = event.target as HTMLInputElement
  uploadForm.file = target.files?.[0] ?? null
}

async function uploadDocument() {
  uploadLoading.value = true
  uploadError.value = ''
  uploadResult.value = null
  try {
    const projectId = uploadForm.project_id || filters.project_id
    if (!projectId) {
      throw new Error('请选择项目')
    }
    if (!uploadForm.file) {
      throw new Error('请选择上传文件')
    }

    const formData = new FormData()
    formData.set('project_id', projectId)
    if (uploadForm.module_name) formData.set('module_name', uploadForm.module_name)
    if (uploadForm.document_type) formData.set('document_type', uploadForm.document_type)
    formData.set('file', uploadForm.file)
    uploadResult.value = await requestUpload<DocumentUploadResponse>('/api/documents/upload', formData)
    filters.project_id = projectId
    await loadCurrentModule()
    if (uploadResult.value.task_id) {
      openTaskModal(uploadResult.value.task_id)
    }
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : '文档上传失败'
  } finally {
    uploadLoading.value = false
  }
}

async function parseDocument(record: DataRecord) {
  if (!record.id) return
  dataError.value = ''
  try {
    const task = await requestApi<TaskResponse>(`/api/documents/${record.id}/parse`, { method: 'POST' })
    openTaskModal(task.task_id)
    await loadCurrentModule()
  } catch (err) {
    dataError.value = err instanceof Error ? err.message : '解析失败'
  }
}

async function reindexDocument(record: DataRecord) {
  if (!record.id) return
  dataError.value = ''
  try {
    const task = await requestApi<TaskResponse>(`/api/documents/${record.id}/reindex`, { method: 'POST' })
    openTaskModal(task.task_id)
    await loadCurrentModule()
  } catch (err) {
    dataError.value = err instanceof Error ? err.message : '重建索引失败'
  }
}

async function openDocumentTasks(record: DataRecord) {
  if (!record.id) return
  taskFilters.biz_id = String(record.id)
  taskFilters.task_type = ''
  taskFilters.status = ''
  activeKey.value = 'tasks'
  await loadTasks()
}

function downloadDocument(record: DataRecord) {
  if (!record.id) return
  window.open(`/api/documents/${record.id}/download`, '_blank', 'noreferrer')
}

async function loadTasks() {
  taskLoading.value = true
  taskError.value = ''
  try {
    const params = new URLSearchParams()
    if (taskFilters.task_type) params.set('task_type', taskFilters.task_type)
    if (taskFilters.status) params.set('status', taskFilters.status)
    if (taskFilters.biz_id) params.set('biz_id', taskFilters.biz_id)
    taskRecords.value = await requestApi<TaskRecord[]>(`/api/tasks${params.toString() ? `?${params}` : ''}`)
    resetPage('taskRecords')
  } catch (err) {
    taskError.value = err instanceof Error ? err.message : '任务加载失败'
  } finally {
    taskLoading.value = false
  }
}

async function loadTaskDetail(taskId: string) {
  activeTask.value = await requestApi<TaskRecord>(`/api/tasks/${taskId}`)
  taskLogs.value = await requestApi<TaskLog[]>(`/api/tasks/${taskId}/logs`)
  resetPage('taskLogs')
  if (activeTask.value.status === 'success' || activeTask.value.status === 'failed' || activeTask.value.status === 'cancelled') {
    window.clearInterval(taskPollTimer)
    taskPollTimer = 0
    if (activeKey.value === 'documents') {
      void loadCurrentModule()
    }
  }
}

function openTaskModal(taskId: string) {
  taskModalOpen.value = true
  activeTask.value = null
  taskLogs.value = []
  void loadTaskDetail(taskId)
  window.clearInterval(taskPollTimer)
  taskPollTimer = window.setInterval(() => {
    void loadTaskDetail(taskId)
  }, 2000)
}

function closeTaskModal() {
  taskModalOpen.value = false
  window.clearInterval(taskPollTimer)
  taskPollTimer = 0
}

async function retryTask(task: TaskRecord) {
  taskError.value = ''
  try {
    const response = await requestApi<TaskResponse>(`/api/tasks/${task.id}/retry`, { method: 'POST' })
    await loadTasks()
    openTaskModal(response.task_id)
  } catch (err) {
    taskError.value = err instanceof Error ? err.message : '任务重试失败'
  }
}

async function cancelTask(task: TaskRecord) {
  taskError.value = ''
  try {
    await requestApi<TaskRecord>(`/api/tasks/${task.id}/cancel`, { method: 'POST' })
    await loadTasks()
  } catch (err) {
    taskError.value = err instanceof Error ? err.message : '任务取消失败'
  }
}

async function openChunkModal(record: DataRecord) {
  if (!record.id) return
  chunkModalOpen.value = true
  chunkLoading.value = true
  chunkError.value = ''
  documentChunks.value = []
  chunkDocumentTitle.value = String(record.document_name ?? '文档切片')
  try {
    documentChunks.value = await requestApi<DocumentChunk[]>(`/api/documents/${record.id}/chunks`)
    resetPage('documentChunks')
  } catch (err) {
    chunkError.value = err instanceof Error ? err.message : '切片加载失败'
  } finally {
    chunkLoading.value = false
  }
}

function closeChunkModal() {
  chunkModalOpen.value = false
}

async function sendChat() {
  chatLoading.value = true
  chatError.value = ''
  chatResult.value = null
  try {
    chatResult.value = await requestApi<ChatResult>('/api/chat', {
      method: 'POST',
      body: JSON.stringify(chatForm)
    })
  } catch (err) {
    chatError.value = err instanceof Error ? err.message : '问答请求失败'
  } finally {
    chatLoading.value = false
  }
}

async function checkRequirement() {
  requirementLoading.value = true
  requirementError.value = ''
  requirementResult.value = null
  try {
    requirementResult.value = await requestApi<RequirementResult>('/api/requirements/check', {
      method: 'POST',
      body: JSON.stringify(requirementForm)
    })
  } catch (err) {
    requirementError.value = err instanceof Error ? err.message : '需求分析失败'
  } finally {
    requirementLoading.value = false
  }
}

async function runSearchDebug() {
  searchLoading.value = true
  searchError.value = ''
  searchResult.value = null
  try {
    searchResult.value = await requestApi<SearchDebugResult>('/api/search/debug', {
      method: 'POST',
      body: JSON.stringify(searchForm)
    })
    ;['searchChunks', 'searchPages', 'searchCapabilities', 'searchApis', 'searchTables', 'searchRequirementCases'].forEach(resetPage)
  } catch (err) {
    searchError.value = err instanceof Error ? err.message : '检索调试失败'
  } finally {
    searchLoading.value = false
  }
}

async function loadEvalCases() {
  evalLoading.value = true
  evalError.value = ''
  try {
    const params = new URLSearchParams()
    if (evalForm.project_id) params.set('project_id', evalForm.project_id)
    evalCases.value = await requestApi<DataRecord[]>(`/api/eval-cases${params.toString() ? `?${params}` : ''}`)
    resetPage('evalCases')
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '评测用例加载失败'
  } finally {
    evalLoading.value = false
  }
}

function openEvalModal(record?: DataRecord) {
  editingEvalCase.value = record ?? null
  evalForm.project_id = String(record?.project_id ?? (evalForm.project_id || (projectOptions.value[0]?.id ?? '')))
  evalForm.question = String(record?.question ?? '')
  evalForm.expected_answer = String(record?.expected_answer ?? '')
  evalForm.expected_sources = typeof record?.expected_sources === 'string'
    ? record.expected_sources
    : JSON.stringify(record?.expected_sources ?? [], null, 2)
  evalForm.expected_mode = String(record?.expected_mode ?? 'business_qa')
  evalForm.expected_feasibility_level = String(record?.expected_feasibility_level ?? '')
  evalForm.expected_keywords = textListValue(record?.expected_keywords)
  evalForm.expected_source_titles = textListValue(record?.expected_source_titles)
  evalForm.expected_source_types = textListValue(record?.expected_source_types)
  evalForm.expected_refusal = Boolean(record?.expected_refusal)
  evalForm.expected_answer_type = String(record?.expected_answer_type ?? '')
  evalForm.tags = textListValue(record?.tags)
  evalForm.difficulty = String(record?.difficulty ?? '')
  evalForm.remark = String(record?.remark ?? '')
  evalForm.enabled = record?.enabled == null ? true : Boolean(record.enabled)
  evalModalOpen.value = true
}

function closeEvalModal() {
  evalModalOpen.value = false
}

async function saveEvalCase() {
  evalError.value = ''
  try {
    const expectedSources = evalForm.expected_sources.trim() ? JSON.parse(evalForm.expected_sources) : []
    const payload = {
      project_id: evalForm.project_id,
      question: evalForm.question,
      expected_answer: evalForm.expected_answer,
      expected_sources: expectedSources,
      expected_mode: evalForm.expected_mode,
      expected_feasibility_level: evalForm.expected_feasibility_level || null,
      expected_keywords: parseTextList(evalForm.expected_keywords),
      expected_source_titles: parseTextList(evalForm.expected_source_titles),
      expected_source_types: parseTextList(evalForm.expected_source_types),
      expected_refusal: evalForm.expected_refusal,
      expected_answer_type: evalForm.expected_answer_type || null,
      tags: parseTextList(evalForm.tags),
      difficulty: evalForm.difficulty || null,
      remark: evalForm.remark,
      enabled: evalForm.enabled
    }
    const id = editingEvalCase.value?.id
    await requestApi<DataRecord>(id ? `/api/eval-cases/${id}` : '/api/eval-cases', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    closeEvalModal()
    await loadEvalCases()
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '评测用例保存失败'
  }
}

async function deleteEvalCase(record: DataRecord) {
  if (!record.id || !window.confirm(`确认删除评测问题「${record.question}」？`)) return
  evalError.value = ''
  try {
    await requestApi<void>(`/api/eval-cases/${record.id}`, { method: 'DELETE' })
    await loadEvalCases()
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '删除评测用例失败'
  }
}

async function runEvalCase(record: DataRecord) {
  if (!record.id) return
  evalError.value = ''
  try {
    await requestApi<DataRecord>(`/api/eval-cases/${record.id}/run`, { method: 'POST' })
    await loadEvalCases()
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '运行评测失败'
  }
}

async function runEvalBatch() {
  evalError.value = ''
  try {
    const run = await requestApi<EvalRunRecord>('/api/eval-cases/run-batch', {
      method: 'POST',
      body: JSON.stringify({
        project_id: evalBatchFilters.project_id || evalForm.project_id || null,
        expected_mode: evalBatchFilters.expected_mode || null,
        tags: evalBatchFilters.tags || null
      })
    })
    activeEvalRun.value = run
    await loadEvalCases()
    await loadEvalRuns()
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '批量评测失败'
  }
}

async function loadEvalRuns() {
  evalRunLoading.value = true
  evalRunError.value = ''
  try {
    const params = new URLSearchParams()
    if (evalBatchFilters.project_id) params.set('project_id', evalBatchFilters.project_id)
    evalRuns.value = await requestApi<EvalRunRecord[]>(`/api/eval-runs${params.toString() ? `?${params}` : ''}`)
    resetPage('evalRuns')
  } catch (err) {
    evalRunError.value = err instanceof Error ? err.message : '评测运行加载失败'
  } finally {
    evalRunLoading.value = false
  }
}

async function openEvalRun(record: EvalRunRecord) {
  activeEvalRun.value = record
  evalRunError.value = ''
  try {
    const [results, summary] = await Promise.all([
      requestApi<DataRecord[]>(`/api/eval-runs/${record.id}/results`),
      requestApi<DataRecord>(`/api/eval-runs/${record.id}/summary`)
    ])
    evalRunResults.value = results
    evalRunSummary.value = summary
    resetPage('evalRunResults')
  } catch (err) {
    evalRunError.value = err instanceof Error ? err.message : '评测运行详情加载失败'
  }
}

async function rerunFailedEvalRun(record: EvalRunRecord) {
  evalRunError.value = ''
  try {
    const run = await requestApi<EvalRunRecord>(`/api/eval-runs/${record.id}/rerun-failed`, { method: 'POST' })
    await loadEvalRuns()
    await openEvalRun(run)
  } catch (err) {
    evalRunError.value = err instanceof Error ? err.message : '重跑失败用例失败'
  }
}

async function loadChatSessions() {
  chatHistoryLoading.value = true
  chatHistoryError.value = ''
  try {
    const params = new URLSearchParams()
    if (chatHistoryFilters.project_id) params.set('project_id', chatHistoryFilters.project_id)
    if (chatHistoryFilters.mode) params.set('mode', chatHistoryFilters.mode)
    if (chatHistoryFilters.keyword) params.set('keyword', chatHistoryFilters.keyword)
    if (chatHistoryFilters.start_time) params.set('start_time', chatHistoryFilters.start_time)
    if (chatHistoryFilters.end_time) params.set('end_time', chatHistoryFilters.end_time)
    chatSessions.value = await requestApi<ChatSessionRecord[]>(`/api/chat-sessions${params.toString() ? `?${params}` : ''}`)
    resetPage('chatSessions')
  } catch (err) {
    chatHistoryError.value = err instanceof Error ? err.message : '对话记录加载失败'
  } finally {
    chatHistoryLoading.value = false
  }
}

async function openChatSession(record: ChatSessionRecord) {
  activeChatSession.value = record
  chatHistoryError.value = ''
  try {
    chatSessionMessages.value = await requestApi<ChatMessageRecord[]>(`/api/chat-sessions/${record.session_id}/messages`)
    resetPage('chatSessionMessages')
  } catch (err) {
    chatHistoryError.value = err instanceof Error ? err.message : '对话详情加载失败'
  }
}

async function deleteChatSession(record: ChatSessionRecord) {
  if (!window.confirm(`确认删除对话「${record.last_question || record.session_id}」？`)) return
  chatHistoryError.value = ''
  try {
    await requestApi<void>(`/api/chat-sessions/${record.session_id}`, { method: 'DELETE' })
    if (activeChatSession.value?.session_id === record.session_id) {
      activeChatSession.value = null
      chatSessionMessages.value = []
    }
    await loadChatSessions()
  } catch (err) {
    chatHistoryError.value = err instanceof Error ? err.message : '删除对话失败'
  }
}

function openFeedbackModalFromChat() {
  if (!chatResult.value || !chatForm.project_id) return
  openFeedbackModal({
    project_id: chatForm.project_id,
    session_id: chatResult.value.session_id,
    message_id: chatResult.value.message_id,
    references: chatResult.value.references
  }, chatResult.value.answer)
}

function openFeedbackModalFromRequirement() {
  if (!requirementResult.value || !requirementForm.project_id) return
  openFeedbackModal({
    project_id: requirementForm.project_id,
    session_id: requirementResult.value.session_id,
    message_id: requirementResult.value.message_id,
    references: requirementResult.value.references
  }, requirementResult.value.conclusion)
}

function openFeedbackModal(context: { project_id: string; session_id: string; message_id: string; references: ReferenceItem[] }, answer?: string) {
  feedbackContext.value = context
  feedbackForm.feedback_type = 'useful'
  feedbackForm.remark = ''
  feedbackForm.corrected_answer = answer ?? ''
  feedbackForm.expected_sources = JSON.stringify(context.references ?? [], null, 2)
  feedbackForm.convert_to_knowledge = false
  feedbackForm.target_knowledge_type = ''
  feedbackModalOpen.value = true
}

function closeFeedbackModal() {
  feedbackModalOpen.value = false
}

async function submitFeedback() {
  if (!feedbackContext.value) return
  feedbackError.value = ''
  try {
    let expectedSources: unknown[] = []
    try {
      expectedSources = feedbackForm.expected_sources.trim() ? JSON.parse(feedbackForm.expected_sources) : []
    } catch {
      throw new Error('期望引用 JSON 格式不正确')
    }
    const payload = {
      ...feedbackContext.value,
      feedback_type: feedbackForm.feedback_type,
      remark: feedbackForm.remark,
      corrected_answer: feedbackForm.corrected_answer,
      expected_sources: expectedSources,
      convert_to_knowledge: feedbackForm.convert_to_knowledge,
      target_knowledge_type: feedbackForm.target_knowledge_type || null
    }
    await requestApi<FeedbackRecord>('/api/feedback', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    closeFeedbackModal()
  } catch (err) {
    feedbackError.value = err instanceof Error ? err.message : '反馈提交失败'
  }
}

async function loadFeedback() {
  feedbackLoading.value = true
  feedbackError.value = ''
  try {
    const params = new URLSearchParams()
    if (feedbackFilters.project_id) params.set('project_id', feedbackFilters.project_id)
    if (feedbackFilters.feedback_type) params.set('feedback_type', feedbackFilters.feedback_type)
    if (feedbackFilters.review_status) params.set('review_status', feedbackFilters.review_status)
    if (feedbackFilters.keyword) params.set('keyword', feedbackFilters.keyword)
    if (feedbackFilters.start_time) params.set('start_time', feedbackFilters.start_time)
    if (feedbackFilters.end_time) params.set('end_time', feedbackFilters.end_time)
    feedbackRecords.value = await requestApi<FeedbackRecord[]>(`/api/feedback${params.toString() ? `?${params}` : ''}`)
    resetPage('feedbackRecords')
  } catch (err) {
    feedbackError.value = err instanceof Error ? err.message : '反馈加载失败'
  } finally {
    feedbackLoading.value = false
  }
}

async function openFeedbackDetail(record: FeedbackRecord) {
  feedbackError.value = ''
  try {
    activeFeedback.value = await requestApi<FeedbackRecord>(`/api/feedback/${record.id}`)
  } catch (err) {
    feedbackError.value = err instanceof Error ? err.message : '反馈详情加载失败'
  }
}

function openFeedbackReview(record: FeedbackRecord, status = 'accepted') {
  activeFeedback.value = record
  feedbackReviewForm.review_status = status
  feedbackReviewForm.reviewer = ''
  feedbackReviewForm.review_remark = ''
  feedbackReviewForm.target_knowledge_type = record.target_knowledge_type || ''
  feedbackReviewModalOpen.value = true
}

function closeFeedbackReview() {
  feedbackReviewModalOpen.value = false
}

async function saveFeedbackReview() {
  if (!activeFeedback.value?.id) return
  feedbackError.value = ''
  try {
    await requestApi<FeedbackRecord>(`/api/feedback/${activeFeedback.value.id}/review`, {
      method: 'PUT',
      body: JSON.stringify(feedbackReviewForm)
    })
    closeFeedbackReview()
    await loadFeedback()
    await openFeedbackDetail(activeFeedback.value)
  } catch (err) {
    feedbackError.value = err instanceof Error ? err.message : '反馈复核失败'
  }
}

async function convertFeedback(record: FeedbackRecord) {
  feedbackError.value = ''
  try {
    if (!record.target_knowledge_type) {
      openFeedbackReview(record, 'accepted')
      feedbackError.value = '请先选择目标知识类型，再执行转知识库'
      return
    }
    await requestApi<FeedbackRecord>(`/api/feedback/${record.id}/review`, {
      method: 'PUT',
      body: JSON.stringify({
        review_status: 'accepted',
        reviewer: '',
        review_remark: record.review_remark || '转知识库前自动通过',
        target_knowledge_type: record.target_knowledge_type
      })
    })
    await requestApi<FeedbackRecord>(`/api/feedback/${record.id}/convert`, { method: 'POST' })
    await loadFeedback()
    await openFeedbackDetail(record)
  } catch (err) {
    feedbackError.value = err instanceof Error ? err.message : '反馈转知识库失败'
  }
}

async function loadModelConfigs() {
  modelConfigLoading.value = true
  modelConfigError.value = ''
  try {
    const params = new URLSearchParams()
    if (modelConfigFilters.provider) params.set('provider', modelConfigFilters.provider)
    if (modelConfigFilters.model_type) params.set('model_type', modelConfigFilters.model_type)
    if (modelConfigFilters.enabled) params.set('enabled', modelConfigFilters.enabled)
    modelConfigs.value = await requestApi<ModelConfigRecord[]>(`/api/model-configs${params.toString() ? `?${params}` : ''}`)
    resetPage('modelConfigs')
  } catch (err) {
    modelConfigError.value = err instanceof Error ? err.message : '模型配置加载失败'
  } finally {
    modelConfigLoading.value = false
  }
}

function openModelConfigModal(record?: ModelConfigRecord) {
  editingModelConfig.value = record ?? null
  modelConfigTestResult.value = null
  modelConfigForm.config_name = record?.config_name ?? ''
  modelConfigForm.provider = record?.provider ?? 'deepseek'
  modelConfigForm.model_type = record?.model_type ?? 'chat'
  modelConfigForm.base_url = record?.base_url ?? 'https://api.deepseek.com'
  modelConfigForm.api_key = ''
  modelConfigForm.model_name = record?.model_name ?? 'deepseek-chat'
  modelConfigForm.dimension = Number(record?.dimension ?? 1536)
  modelConfigForm.temperature = Number(record?.temperature ?? 0.2)
  modelConfigForm.max_tokens = Number(record?.max_tokens ?? 2048)
  modelConfigForm.enabled = record?.enabled ?? true
  modelConfigForm.default_config = record?.default_config ?? false
  modelConfigForm.remark = record?.remark ?? ''
  modelConfigModalOpen.value = true
}

function closeModelConfigModal() {
  modelConfigModalOpen.value = false
}

async function saveModelConfig() {
  modelConfigError.value = ''
  try {
    const payload = { ...modelConfigForm }
    const url = editingModelConfig.value ? `/api/model-configs/${editingModelConfig.value.id}` : '/api/model-configs'
    const method = editingModelConfig.value ? 'PUT' : 'POST'
    await requestApi<ModelConfigRecord>(url, {
      method,
      body: JSON.stringify(payload)
    })
    closeModelConfigModal()
    await loadModelConfigs()
  } catch (err) {
    modelConfigError.value = err instanceof Error ? err.message : '模型配置保存失败'
  }
}

async function deleteModelConfig(record: ModelConfigRecord) {
  if (!window.confirm(`确认删除模型配置「${record.config_name}」？`)) return
  modelConfigError.value = ''
  try {
    await requestApi<void>(`/api/model-configs/${record.id}`, { method: 'DELETE' })
    await loadModelConfigs()
  } catch (err) {
    modelConfigError.value = err instanceof Error ? err.message : '模型配置删除失败'
  }
}

async function setDefaultModelConfig(record: ModelConfigRecord) {
  modelConfigError.value = ''
  try {
    await requestApi<ModelConfigRecord>(`/api/model-configs/${record.id}/set-default`, { method: 'POST' })
    await loadModelConfigs()
  } catch (err) {
    modelConfigError.value = err instanceof Error ? err.message : '默认模型设置失败'
  }
}

async function testModelConfig(record: ModelConfigRecord) {
  modelConfigError.value = ''
  modelConfigTestResult.value = null
  try {
    modelConfigTestResult.value = await requestApi<DataRecord>(`/api/model-configs/${record.id}/test`, { method: 'POST' })
  } catch (err) {
    modelConfigError.value = err instanceof Error ? err.message : '模型连接测试失败'
  }
}

async function loadPromptTemplates() {
  promptTemplateLoading.value = true
  promptTemplateError.value = ''
  try {
    const params = new URLSearchParams()
    if (promptTemplateFilters.template_type) params.set('template_type', promptTemplateFilters.template_type)
    if (promptTemplateFilters.mode) params.set('mode', promptTemplateFilters.mode)
    if (promptTemplateFilters.enabled) params.set('enabled', promptTemplateFilters.enabled)
    promptTemplates.value = await requestApi<PromptTemplateRecord[]>(`/api/prompt-templates${params.toString() ? `?${params}` : ''}`)
    resetPage('promptTemplates')
  } catch (err) {
    promptTemplateError.value = err instanceof Error ? err.message : 'Prompt 模板加载失败'
  } finally {
    promptTemplateLoading.value = false
  }
}

function openPromptTemplateModal(record?: PromptTemplateRecord) {
  editingPromptTemplate.value = record ?? null
  promptTemplateForm.template_name = record?.template_name ?? ''
  promptTemplateForm.template_type = record?.template_type ?? 'doc_qa'
  promptTemplateForm.mode = record?.mode ?? 'doc_qa'
  promptTemplateForm.system_prompt = record?.system_prompt ?? ''
  promptTemplateForm.user_prompt_template = record?.user_prompt_template ?? ''
  promptTemplateForm.output_format = record?.output_format ?? 'markdown'
  promptTemplateForm.enabled = record?.enabled ?? true
  promptTemplateForm.default_template = record?.default_template ?? false
  promptTemplateForm.remark = record?.remark ?? ''
  promptTemplateModalOpen.value = true
}

function closePromptTemplateModal() {
  promptTemplateModalOpen.value = false
}

async function savePromptTemplate() {
  promptTemplateError.value = ''
  try {
    const payload = { ...promptTemplateForm }
    const url = editingPromptTemplate.value ? `/api/prompt-templates/${editingPromptTemplate.value.id}` : '/api/prompt-templates'
    const method = editingPromptTemplate.value ? 'PUT' : 'POST'
    await requestApi<PromptTemplateRecord>(url, {
      method,
      body: JSON.stringify(payload)
    })
    closePromptTemplateModal()
    await loadPromptTemplates()
  } catch (err) {
    promptTemplateError.value = err instanceof Error ? err.message : 'Prompt 模板保存失败'
  }
}

async function deletePromptTemplate(record: PromptTemplateRecord) {
  if (!window.confirm(`确认删除 Prompt 模板「${record.template_name}」？`)) return
  promptTemplateError.value = ''
  try {
    await requestApi<void>(`/api/prompt-templates/${record.id}`, { method: 'DELETE' })
    await loadPromptTemplates()
  } catch (err) {
    promptTemplateError.value = err instanceof Error ? err.message : 'Prompt 模板删除失败'
  }
}

async function setDefaultPromptTemplate(record: PromptTemplateRecord) {
  promptTemplateError.value = ''
  try {
    await requestApi<PromptTemplateRecord>(`/api/prompt-templates/${record.id}/set-default`, { method: 'POST' })
    await loadPromptTemplates()
  } catch (err) {
    promptTemplateError.value = err instanceof Error ? err.message : '默认 Prompt 设置失败'
  }
}

function openPromptTemplateTest(record: PromptTemplateRecord) {
  activePromptTemplate.value = record
  promptTemplateTestResult.value = null
  promptTemplateTestForm.mode = record.mode || 'doc_qa'
  promptTemplateTestOpen.value = true
}

function closePromptTemplateTest() {
  promptTemplateTestOpen.value = false
}

async function testPromptTemplate() {
  if (!activePromptTemplate.value) return
  promptTemplateError.value = ''
  try {
    promptTemplateTestResult.value = await requestApi<DataRecord>(`/api/prompt-templates/${activePromptTemplate.value.id}/test`, {
      method: 'POST',
      body: JSON.stringify(promptTemplateTestForm)
    })
  } catch (err) {
    promptTemplateError.value = err instanceof Error ? err.message : 'Prompt 模板测试失败'
  }
}

function openReviewModal(record: DataRecord) {
  const result = parseLatestResult(record.latest_result)
  if (!result?.id) return
  activeEvalResult.value = result
  reviewForm.passed = Boolean(result.passed)
  reviewForm.score = Number(result.score ?? 80)
  reviewForm.remark = String(result.remark ?? '')
  reviewModalOpen.value = true
}

function closeReviewModal() {
  reviewModalOpen.value = false
}

async function saveReview() {
  if (!activeEvalResult.value?.id) return
  try {
    await requestApi<DataRecord>(`/api/eval-results/${activeEvalResult.value.id}/review`, {
      method: 'PUT',
      body: JSON.stringify(reviewForm)
    })
    closeReviewModal()
    await loadEvalCases()
  } catch (err) {
    evalError.value = err instanceof Error ? err.message : '评测复核保存失败'
  }
}

function formatCell(value: unknown) {
  if (value === true) return '是'
  if (value === false) return '否'
  if (value == null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(value: string) {
  return value
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\[([Rr]\d+)\]/g, '<span class="answer-cite">[$1]</span>')
}

function renderMarkdown(value: string) {
  const escaped = escapeHtml(value || '')
  const lines = escaped.split(/\r?\n/)
  const blocks: string[] = []
  let paragraph: string[] = []
  let list: string[] = []
  let listType: 'ul' | 'ol' = 'ul'
  let code: string[] = []
  let inCode = false

  const flushParagraph = () => {
    if (!paragraph.length) return
    blocks.push(`<p>${renderInlineMarkdown(paragraph.join(' '))}</p>`)
    paragraph = []
  }
  const flushList = () => {
    if (!list.length) return
    blocks.push(`<${listType}>${list.map(item => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</${listType}>`)
    list = []
    listType = 'ul'
  }
  const flushCode = () => {
    if (!code.length) return
    blocks.push(`<pre><code>${code.join('\n')}</code></pre>`)
    code = []
  }

  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('```')) {
      if (inCode) {
        flushCode()
        inCode = false
      } else {
        flushParagraph()
        flushList()
        inCode = true
      }
      continue
    }
    if (inCode) {
      code.push(line)
      continue
    }
    if (!trimmed) {
      flushParagraph()
      flushList()
      continue
    }
    const heading = trimmed.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      flushParagraph()
      flushList()
      const level = Math.min(heading[1].length + 2, 5)
      blocks.push(`<h${level}>${renderInlineMarkdown(heading[2])}</h${level}>`)
      continue
    }
    const listItem = trimmed.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      flushParagraph()
      if (list.length && listType !== 'ul') flushList()
      listType = 'ul'
      list.push(listItem[1])
      continue
    }
    const orderedListItem = trimmed.match(/^\d+[.)、]\s+(.+)$/)
    if (orderedListItem) {
      flushParagraph()
      if (list.length && listType !== 'ol') flushList()
      listType = 'ol'
      list.push(orderedListItem[1])
      continue
    }
    flushList()
    paragraph.push(trimmed)
  }
  flushParagraph()
  flushList()
  flushCode()
  return blocks.join('')
}

function formatProviderName(value: unknown) {
  const provider = String(value ?? '').trim().toLowerCase()
  if (!provider) return ''
  const labels: Record<string, string> = {
    deepseek: 'DeepSeek',
    openai: 'OpenAI',
    openai_compatible: 'OpenAI 兼容模型',
  }
  return labels[provider] ?? provider
}

function formatSearchStrategy(value: unknown) {
  const strategy = String(value ?? '').trim().toLowerCase()
  const labels: Record<string, string> = {
    keyword: '关键词检索',
    vector: '向量检索',
    hybrid: '混合检索',
    fallback_keyword: '关键词降级'
  }
  return labels[strategy] ?? strategy
}

function formatReference(reference: ReferenceItem) {
  return `${reference.source_type} / ${reference.title ?? reference.source_title}`
}

function openReferenceModal(reference: ReferenceItem | SearchDebugHit | SearchDebugChunk) {
  activeReference.value = reference
  referenceModalOpen.value = true
}

function closeReferenceModal() {
  referenceModalOpen.value = false
}

function referenceTitle(reference: ReferenceItem | SearchDebugHit | SearchDebugChunk | null) {
  if (!reference) return ''
  if ('document_title' in reference && reference.document_title) return `${reference.document_title}${'chunk_index' in reference ? ` #${reference.chunk_index}` : ''}`
  if ('title' in reference && reference.title) return reference.title
  if ('source_title' in reference && reference.source_title) return reference.source_title
  return '引用详情'
}

function referenceContent(reference: ReferenceItem | SearchDebugHit | SearchDebugChunk | null) {
  if (!reference) return ''
  if ('content' in reference && reference.content) return String(reference.content)
  if ('quote' in reference && reference.quote) return reference.quote
  if ('content_preview' in reference && reference.content_preview) return reference.content_preview
  return ''
}

function parseLatestResult(value: unknown): EvalResult | null {
  if (!value) return null
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as EvalResult
    } catch {
      return null
    }
  }
  return value as EvalResult
}

function parseTextList(value: string): string[] {
  const text = value.trim()
  if (!text) return []
  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text)
      return Array.isArray(parsed) ? parsed.map(String).filter(Boolean) : []
    } catch {
      return []
    }
  }
  return text.split(/[\n,，]/).map((item) => item.trim()).filter(Boolean)
}

function textListValue(value: unknown): string {
  if (!value) return ''
  if (Array.isArray(value)) return value.join('\n')
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.join('\n')
    } catch {
      return value
    }
    return value
  }
  return JSON.stringify(value)
}

function parseReferences(value: unknown): ReferenceItem[] {
  if (!value) return []
  if (Array.isArray(value)) return value as ReferenceItem[]
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as ReferenceItem[]
    } catch {
      return []
    }
  }
  return []
}

function projectName(id: unknown) {
  return projectOptions.value.find(project => project.id === String(id))?.name ?? '-'
}

function statusClass(value: unknown) {
  return `status-${String(value ?? 'unknown')}`
}

function fieldLabel(module: ModuleConfig, key: string) {
  const labels: Record<string, string> = {
    parse_status: '解析状态',
    index_status: '索引状态',
    chunk_count: '切片数',
    last_parsed_at: '最近解析',
    last_indexed_at: '最近索引',
    parse_error: '解析错误',
    index_error: '索引错误',
    storage_type: '存储',
    storage_bucket: 'Bucket',
    storage_object_key: 'Object Key',
    content_type: 'Content-Type',
    updated_at: '更新时间',
    enabled: '启用'
  }
  return module.fields.find(field => field.key === key)?.label ?? labels[key] ?? key
}

function updateFieldValue(key: string, event: Event) {
  const target = event.target as HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
  formState[key] = target.value
}

function updateCheckboxValue(key: string, event: Event) {
  const target = event.target as HTMLInputElement
  formState[key] = target.checked
}

onMounted(async () => {
  startFlowAnimation()
  void loadProjects().then(() => {
    const firstProject = projectOptions.value[0]?.id ?? ''
    chatForm.project_id = firstProject
    requirementForm.project_id = firstProject
    filters.project_id = firstProject
    uploadForm.project_id = firstProject
    searchForm.project_id = firstProject
    evalForm.project_id = firstProject
    evalBatchFilters.project_id = firstProject
    void loadDashboard()
  })

  try {
    const response = await fetch('/api/health')
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    health.value = await response.json()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '健康检查请求失败'
  } finally {
    loading.value = false
  }
})

watch(activeKey, () => {
  if (activeModule.value) {
    void loadProjects()
    void loadCurrentModule()
  }
  if (activeKey.value === 'eval') {
    void loadProjects().then(loadEvalCases)
  }
  if (activeKey.value === 'eval-runs') {
    void loadProjects().then(loadEvalRuns)
  }
  if (activeKey.value === 'model-configs') {
    void loadModelConfigs()
  }
  if (activeKey.value === 'prompt-templates') {
    void loadPromptTemplates()
  }
  if (activeKey.value === 'chat-history') {
    void loadProjects().then(loadChatSessions)
  }
  if (activeKey.value === 'feedback') {
    void loadProjects().then(loadFeedback)
  }
  if (activeKey.value === 'tasks') {
    void loadTasks()
  }
  if (activeKey.value === 'dashboard') {
    void loadDashboard()
  }
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrameId)
  window.clearInterval(taskPollTimer)
  resizeObserver?.disconnect()
  removeMouseMoveListener?.()
  shaderRuntime = null
})
</script>

<template>
  <main class="app-shell">
    <div class="flow-background" aria-hidden="true">
      <canvas ref="flowCanvas" class="flow-canvas"></canvas>
    </div>

    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">水</span>
        <div>
          <strong>智水顾问</strong>
          <small>water-ai-consultant</small>
        </div>
      </div>
      <nav>
        <div v-for="group in navGroups" :key="group.label" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button v-for="item in group.items" :key="item.key" type="button" :class="{ active: activeKey === item.key }" @click="activate(item.key)">
            <span class="nav-tick"></span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">AI 水务项目智能顾问平台</p>
          <h1>{{ navItems.find(item => item.key === activeKey)?.label || '项目基础骨架已就绪' }}</h1>
        </div>
        <div class="titleblock">
          <div class="titleblock-row"><span>项目代号</span><span>{{ currentProjectSummary.code }}</span></div>
          <div class="titleblock-row"><span>状态</span><span><i class="dot"></i>{{ currentProjectSummary.status }}</span></div>
          <div class="titleblock-row"><span>行业</span><span>{{ currentProjectSummary.industry }}</span></div>
          <div class="titleblock-row"><span>更新</span><span>{{ currentProjectSummary.updated }}</span></div>
        </div>
        <a href="/swagger-ui/index.html" target="_blank" rel="noreferrer">接口文档</a>
      </header>
      <svg class="contour" viewBox="0 0 1200 18" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 9 Q 30 2, 60 9 T 120 9 T 180 9 T 240 9 T 300 9 T 360 9 T 420 9 T 480 9 T 540 9 T 600 9 T 660 9 T 720 9 T 780 9 T 840 9 T 900 9 T 960 9 T 1020 9 T 1080 9 T 1140 9 T 1200 9" fill="none" stroke="currentColor" stroke-width="1" />
      </svg>

      <template v-if="activeKey === 'dashboard'">
        <section class="hero-panel">
          <div>
            <p class="panel-label">运营总览</p>
            <h2>AI 水务顾问工作台</h2>
            <p class="hero-copy">集中查看知识库、文档解析、问答评测和异步任务状态，快速判断当前项目资料是否足够支撑售前、实施和产品决策。</p>
          </div>
          <div class="metric-strip">
            <article v-for="metric in dashboardMetrics" :key="metric.label" :class="['metric-card', metric.tone]">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </div>
        </section>

        <section class="dashboard-grid">
          <article class="status-panel service-card">
            <div>
              <p class="panel-label">服务状态</p>
              <h2 v-if="loading">检查中...</h2>
              <h2 v-else-if="error">连接失败</h2>
              <h2 v-else>{{ health?.data?.status }}</h2>
            </div>
            <dl>
              <div>
                <dt>服务</dt>
                <dd>{{ health?.data?.service ?? '-' }}</dd>
              </div>
              <div>
                <dt>最近检查</dt>
                <dd>{{ health?.data?.time ?? '-' }}</dd>
              </div>
              <div>
                <dt>Trace ID</dt>
                <dd>{{ health?.trace_id ?? '-' }}</dd>
              </div>
            </dl>
            <p v-if="error" class="error-text">{{ error }}</p>
            <p v-if="dashboardError" class="error-text">{{ dashboardError }}</p>
          </article>

          <article class="status-panel quick-panel">
            <div>
              <p class="panel-label">快捷入口</p>
              <h2>常用工作流</h2>
            </div>
            <div class="quick-actions">
              <button v-for="action in quickActions" :key="action.key" type="button" @click="jumpTo(action.key)">
                {{ action.label }}
              </button>
            </div>
          </article>
        </section>

        <section class="dashboard-grid lower">
          <article class="status-panel recent-task-panel">
            <div class="section-title-row">
              <div>
                <p class="panel-label">最近任务</p>
                <h2>{{ dashboardLoading ? '加载中...' : '解析与索引动态' }}</h2>
              </div>
              <button type="button" class="ghost-action" @click="jumpTo('tasks')">任务中心</button>
            </div>
            <div class="recent-task-list">
              <button v-for="task in dashboardTasks" :key="task.id" type="button" @click="openTaskModal(task.id)">
                <span :class="['status-pill', statusClass(task.status)]">{{ task.status }}</span>
                <strong>{{ task.task_type }}</strong>
                <em>{{ task.progress }}%</em>
                <small>{{ task.message || task.error_message || '-' }}</small>
              </button>
              <p v-if="dashboardTasks.length === 0">暂无任务记录</p>
            </div>
          </article>

          <article class="status-panel quality-panel">
            <p class="panel-label">知识库质量提示</p>
            <h2>先补资料，再问结论</h2>
            <p>问答和需求分析只基于文档切片、页面说明、能力清单、接口、数据表和历史需求案例。资料不足时系统会拒答，避免凭空判断。</p>
            <div class="quality-points">
              <span>引用溯源已开启</span>
              <span>关键词降级可用</span>
              <span>评测用例 {{ dashboardEvalCases.length }} 条</span>
            </div>
          </article>
        </section>

        <section class="module-grid capability-grid">
          <article v-for="capability in capabilityCards" :key="capability.title">
            <span>{{ capability.status }}</span>
            <h3>{{ capability.title }}</h3>
            <p>{{ capability.desc }}</p>
          </article>
        </section>
      </template>

      <section v-else-if="activeKey === 'chat'" class="ai-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">{{ chatRuntimeLabel }}</p>
            <h2>智能问答</h2>
          </div>
          <button class="primary-action" type="button" :disabled="chatLoading" @click="sendChat">
            {{ chatLoading ? '生成中...' : '发送问题' }}
          </button>
        </div>

        <div class="ai-form">
          <label>
            <span>项目</span>
            <select v-model="chatForm.project_id">
              <option value="">请选择项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模式</span>
            <select v-model="chatForm.mode">
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="business_qa">业务问答</option>
              <option value="requirement_check">需求初步判断</option>
            </select>
          </label>
          <label class="wide">
            <span>问题</span>
            <textarea v-model="chatForm.question" rows="5" placeholder="请输入问题"></textarea>
          </label>
        </div>

        <p v-if="chatError" class="error-text">{{ chatError }}</p>
        <section v-if="chatResult" class="answer-card">
          <div class="answer-main">
            <p class="panel-label">回答</p>
            <div class="answer-markdown" v-html="renderMarkdown(chatResult.answer)"></div>
            <div class="answer-meta">
              <span>置信度 {{ chatResult.confidence }}</span>
              <span>{{ formatProviderName(chatResult.model_provider || chatResult.llm_provider) || '-' }}</span>
              <span>{{ formatSearchStrategy(chatResult.search_strategy) || '-' }}</span>
              <span>Trace {{ chatResult.trace_id }}</span>
            </div>
            <details class="debug-details">
              <summary>调试信息</summary>
              <dl>
                <div><dt>模型 Provider</dt><dd>{{ chatResult.model_provider || chatResult.llm_provider || '-' }}</dd></div>
                <div><dt>模型名称</dt><dd>{{ chatResult.model_name || '-' }}</dd></div>
                <div><dt>Prompt 模板</dt><dd>{{ chatResult.prompt_template_name || '内置模板' }}</dd></div>
              </dl>
            </details>
            <p v-if="chatResult.insufficient_answer || chatResult.references.length === 0" class="error-text">
              当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。
            </p>
            <div class="row-actions">
              <button type="button" @click="openFeedbackModalFromChat">提交反馈</button>
            </div>
          </div>
          <div class="reference-list">
            <p class="panel-label">引用来源</p>
            <article v-for="reference in chatResult.references" :key="`${reference.source_type}-${reference.source_id}`" class="clickable-result" @click="openReferenceModal(reference)">
              <strong>{{ formatReference(reference) }}</strong>
              <span>{{ reference.source_locator }}</span>
              <p>{{ reference.content_preview || reference.quote }}</p>
            </article>
            <p v-if="chatResult.references.length === 0" class="error-text">当前资料不足，无法确认</p>
          </div>
        </section>
      </section>

      <section v-else-if="activeKey === 'requirement-check'" class="ai-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">能力清单和历史案例优先</p>
            <h2>需求可行性分析</h2>
          </div>
          <button class="primary-action" type="button" :disabled="requirementLoading" @click="checkRequirement">
            {{ requirementLoading ? '分析中...' : '开始分析' }}
          </button>
        </div>

        <div class="ai-form">
          <label>
            <span>项目</span>
            <select v-model="requirementForm.project_id">
              <option value="">请选择项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模块</span>
            <input v-model="requirementForm.module_name" placeholder="可选，如 项目管理" />
          </label>
          <label class="wide">
            <span>需求描述</span>
            <textarea v-model="requirementForm.requirement_desc" rows="5" placeholder="请输入客户新需求"></textarea>
          </label>
        </div>

        <p v-if="requirementError" class="error-text">{{ requirementError }}</p>
        <section v-if="requirementResult" class="analysis-grid">
          <article>
            <span>可行性等级</span>
            <strong>{{ requirementResult.feasibility_level }}</strong>
          </article>
          <article>
            <span>工作量</span>
            <strong>{{ requirementResult.workload_level }}</strong>
          </article>
          <article class="wide">
            <span>结论</span>
            <p>{{ requirementResult.conclusion }}</p>
          </article>
          <article class="wide">
            <span>需求理解</span>
            <p>{{ requirementResult.requirement_understanding }}</p>
          </article>
          <article class="wide">
            <span>推荐方案</span>
            <p>{{ requirementResult.recommended_solution }}</p>
          </article>
          <article>
            <span>模型</span>
            <p>{{ requirementResult.llm_provider || '-' }}</p>
          </article>
          <article>
            <span>检索策略</span>
            <p>{{ requirementResult.search_strategy || '-' }}</p>
          </article>
          <article class="wide">
            <span>调试信息</span>
            <details class="debug-details">
              <summary>模型与 Prompt</summary>
              <dl>
                <div><dt>模型 Provider</dt><dd>{{ requirementResult.model_provider || requirementResult.llm_provider || '-' }}</dd></div>
                <div><dt>模型名称</dt><dd>{{ requirementResult.model_name || '-' }}</dd></div>
                <div><dt>Prompt 模板</dt><dd>{{ requirementResult.prompt_template_name || '内置模板' }}</dd></div>
                <div><dt>Trace</dt><dd>{{ requirementResult.trace_id }}</dd></div>
              </dl>
            </details>
          </article>
          <article>
            <span>影响模块</span>
            <p>{{ requirementResult.impact_modules.join('、') || '-' }}</p>
          </article>
          <article>
            <span>风险点</span>
            <p>{{ requirementResult.risk_points.join('；') || '-' }}</p>
          </article>
          <article class="wide reference-list">
            <span>References</span>
            <div v-for="reference in requirementResult.references" :key="`${reference.source_type}-${reference.source_id}`" class="clickable-result" @click="openReferenceModal(reference)">
              <strong>{{ formatReference(reference) }}</strong>
              <p>{{ reference.content_preview || reference.quote }}</p>
            </div>
            <p v-if="requirementResult.references.length === 0" class="error-text">当前资料不足，无法确认</p>
          </article>
          <article class="wide">
            <span>反馈</span>
            <div class="row-actions">
              <button type="button" @click="openFeedbackModalFromRequirement">提交反馈</button>
            </div>
          </article>
        </section>
      </section>

      <section v-else-if="activeKey === 'search-debug'" class="ai-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">不调用大模型</p>
            <h2>检索调试</h2>
          </div>
          <button class="primary-action" type="button" :disabled="searchLoading" @click="runSearchDebug">
            {{ searchLoading ? '检索中...' : '开始检索' }}
          </button>
        </div>

        <div class="ai-form debug-form">
          <label>
            <span>项目</span>
            <select v-model="searchForm.project_id">
              <option value="">请选择项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模式</span>
            <select v-model="searchForm.mode">
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="business_qa">业务问答</option>
              <option value="requirement_check">需求分析</option>
            </select>
          </label>
          <label>
            <span>Top K</span>
            <input v-model.number="searchForm.top_k" min="1" max="50" type="number" />
          </label>
          <label class="wide">
            <span>问题</span>
            <textarea v-model="searchForm.question" rows="4" placeholder="输入要排查的用户问题"></textarea>
          </label>
        </div>
        <p v-if="searchError" class="error-text">{{ searchError }}</p>

        <section v-if="searchResult" class="debug-summary">
          <article>
            <span>命中总数</span>
            <strong>{{ searchResult.total_hits }}</strong>
          </article>
          <article>
            <span>策略</span>
            <p>{{ searchResult.search_strategy }}</p>
          </article>
          <article>
            <span>Embedding</span>
            <p>{{ searchResult.embedding_provider }} / 向量：{{ searchResult.vector_enabled ? '启用' : '未启用' }}</p>
          </article>
          <article>
            <span>关键词降级</span>
            <p>{{ searchResult.keyword_fallback_used ? '已发生或已补充' : '未发生' }}</p>
          </article>
          <article>
            <span>查询</span>
            <p>{{ searchResult.query }}</p>
          </article>
        </section>

        <section v-if="searchResult" class="debug-groups">
          <article class="debug-group">
            <h3>文档 Chunk（{{ searchResult.chunks.length }}）</h3>
            <div v-for="chunk in pagedItems(searchResult.chunks, 'searchChunks')" :key="chunk.chunk_id" class="debug-item clickable-result" @click="openReferenceModal(chunk)">
              <div><strong>{{ chunk.document_title }} #{{ chunk.chunk_index }}</strong><span>{{ chunk.score }}</span></div>
              <small>{{ projectName(searchForm.project_id) }} / {{ chunk.module_name || '-' }} / {{ chunk.source_type }} / {{ chunk.score_type }} / {{ chunk.index_status || '-' }}</small>
              <p>{{ chunk.content_preview }}</p>
            </div>
            <div v-if="searchResult.chunks.length > pagination.searchChunks.pageSize" class="pagination-bar compact-pagination">
              <span>{{ pageStart(searchResult.chunks.length, 'searchChunks') }}-{{ pageEnd(searchResult.chunks.length, 'searchChunks') }} / {{ searchResult.chunks.length }}</span>
              <button type="button" :disabled="currentPage(searchResult.chunks.length, 'searchChunks') === 1" @click="setPage('searchChunks', currentPage(searchResult.chunks.length, 'searchChunks') - 1, searchResult.chunks.length)">上一页</button>
              <strong>{{ currentPage(searchResult.chunks.length, 'searchChunks') }} / {{ pageTotal(searchResult.chunks.length, 'searchChunks') }}</strong>
              <button type="button" :disabled="currentPage(searchResult.chunks.length, 'searchChunks') === pageTotal(searchResult.chunks.length, 'searchChunks')" @click="setPage('searchChunks', currentPage(searchResult.chunks.length, 'searchChunks') + 1, searchResult.chunks.length)">下一页</button>
            </div>
          </article>
          <article v-for="group in searchDebugGroups" :key="group.title" class="debug-group">
            <h3>{{ group.title }}（{{ group.list.length }}）</h3>
            <div v-for="hit in pagedItems(group.list, group.pageKey)" :key="`${hit.source_type}-${hit.source_id}`" class="debug-item clickable-result" @click="openReferenceModal(hit)">
              <div><strong>{{ hit.title }}</strong><span>{{ hit.score }}</span></div>
              <small>{{ hit.project_name || projectName(hit.project_id) }} / {{ hit.module_name || '-' }} / {{ hit.source_type }}</small>
              <p>{{ hit.content_preview }}</p>
            </div>
            <div v-if="group.list.length > paginationState(group.pageKey).pageSize" class="pagination-bar compact-pagination">
              <span>{{ pageStart(group.list.length, group.pageKey) }}-{{ pageEnd(group.list.length, group.pageKey) }} / {{ group.list.length }}</span>
              <button type="button" :disabled="currentPage(group.list.length, group.pageKey) === 1" @click="setPage(group.pageKey, currentPage(group.list.length, group.pageKey) - 1, group.list.length)">上一页</button>
              <strong>{{ currentPage(group.list.length, group.pageKey) }} / {{ pageTotal(group.list.length, group.pageKey) }}</strong>
              <button type="button" :disabled="currentPage(group.list.length, group.pageKey) === pageTotal(group.list.length, group.pageKey)" @click="setPage(group.pageKey, currentPage(group.list.length, group.pageKey) + 1, group.list.length)">下一页</button>
            </div>
          </article>
        </section>
      </section>

      <section v-else-if="activeKey === 'eval'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">半自动评测</p>
            <h2>问答评测</h2>
          </div>
          <div class="row-actions">
            <button class="ghost-action" type="button" @click="loadEvalCases">刷新</button>
            <button class="ghost-action" type="button" @click="runEvalBatch">批量运行</button>
            <button class="primary-action" type="button" @click="openEvalModal()">新增用例</button>
          </div>
        </div>
        <div class="filter-bar">
          <label>
            <span>项目</span>
            <select v-model="evalForm.project_id">
              <option value="">全部项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>默认模式</span>
            <select v-model="evalForm.expected_mode">
              <option value="business_qa">业务问答</option>
              <option value="api">接口问答</option>
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="requirement_check">需求分析</option>
            </select>
          </label>
          <label>
            <span>批量标签</span>
            <input v-model="evalBatchFilters.tags" placeholder="alarm / api" />
          </label>
          <button type="button" @click="loadEvalCases">查询</button>
        </div>
        <p v-if="evalError" class="error-text">{{ evalError }}</p>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>问题</th>
                <th>模式</th>
                <th>期望</th>
                <th>自动评分</th>
                <th>命中情况</th>
                <th>最近回答</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="evalLoading"><td colspan="6">加载中...</td></tr>
              <tr v-else-if="evalCases.length === 0"><td colspan="6">暂无评测用例</td></tr>
              <template v-else>
                <tr v-for="record in pagedItems(evalCases, 'evalCases')" :key="String(record.id)">
                  <td>{{ record.question }}</td>
                  <td>{{ record.expected_mode }}</td>
                  <td>
                    <p>等级：{{ record.expected_feasibility_level || '-' }}</p>
                    <small>拒答：{{ record.expected_refusal ? '是' : '否' }} / {{ textListValue(record.tags) || '-' }}</small>
                  </td>
                  <td>
                    <template v-if="parseLatestResult(record.latest_result)">
                      <strong>{{ parseLatestResult(record.latest_result)?.auto_score ?? parseLatestResult(record.latest_result)?.score ?? '-' }}</strong>
                      <span v-if="parseLatestResult(record.latest_result)?.auto_passed === true" class="status-pill status-ready">自动通过</span>
                      <span v-else-if="parseLatestResult(record.latest_result)?.auto_passed === false" class="status-pill status-failed">自动不通过</span>
                      <small>
                        关键词 {{ parseLatestResult(record.latest_result)?.keyword_score ?? '-' }} /
                        来源 {{ parseLatestResult(record.latest_result)?.source_score ?? '-' }} /
                        拒答 {{ parseLatestResult(record.latest_result)?.refusal_score ?? '-' }} /
                        等级 {{ parseLatestResult(record.latest_result)?.feasibility_score ?? '-' }}
                      </small>
                    </template>
                    <span v-else>-</span>
                  </td>
                  <td>
                    <small>命中关键词：{{ textListValue(parseLatestResult(record.latest_result)?.matched_keywords) || '-' }}</small>
                    <small>缺失关键词：{{ textListValue(parseLatestResult(record.latest_result)?.missing_keywords) || '-' }}</small>
                    <small>缺失来源：{{ textListValue(parseLatestResult(record.latest_result)?.missing_sources) || '-' }}</small>
                  </td>
                  <td>
                    <p>{{ parseLatestResult(record.latest_result)?.actual_answer || '-' }}</p>
                    <div class="mini-reference-list">
                      <button v-for="reference in parseReferences(parseLatestResult(record.latest_result)?.actual_references)" :key="`${reference.source_type}-${reference.source_id}`" type="button" @click="openReferenceModal(reference)">
                        {{ formatReference(reference) }}
                      </button>
                    </div>
                    <button v-if="parseLatestResult(record.latest_result)" type="button" class="ghost-action" @click="openReviewModal(record)">复核</button>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button type="button" @click="runEvalCase(record)">运行</button>
                      <button type="button" @click="openEvalModal(record)">编辑</button>
                      <button type="button" class="danger-action" @click="deleteEvalCase(record)">删除</button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <div v-if="evalCases.length > pagination.evalCases.pageSize" class="pagination-bar">
          <span>显示 {{ pageStart(evalCases.length, 'evalCases') }}-{{ pageEnd(evalCases.length, 'evalCases') }} / {{ evalCases.length }}</span>
          <label>
            <span>每页</span>
            <select v-model.number="pagination.evalCases.pageSize" @change="resetPage('evalCases')">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
          <button type="button" :disabled="currentPage(evalCases.length, 'evalCases') === 1" @click="setPage('evalCases', currentPage(evalCases.length, 'evalCases') - 1, evalCases.length)">上一页</button>
          <strong>{{ currentPage(evalCases.length, 'evalCases') }} / {{ pageTotal(evalCases.length, 'evalCases') }}</strong>
          <button type="button" :disabled="currentPage(evalCases.length, 'evalCases') === pageTotal(evalCases.length, 'evalCases')" @click="setPage('evalCases', currentPage(evalCases.length, 'evalCases') + 1, evalCases.length)">下一页</button>
        </div>
      </section>

      <section v-else-if="activeKey === 'eval-runs'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">自动评分回归</p>
            <h2>评测运行看板</h2>
          </div>
          <div class="row-actions">
            <button class="ghost-action" type="button" @click="loadEvalRuns">刷新</button>
            <button class="primary-action" type="button" @click="runEvalBatch">运行批量评测</button>
          </div>
        </div>
        <div class="filter-bar">
          <label>
            <span>项目</span>
            <select v-model="evalBatchFilters.project_id">
              <option value="">全部项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模式</span>
            <select v-model="evalBatchFilters.expected_mode">
              <option value="">全部模式</option>
              <option value="business_qa">业务问答</option>
              <option value="api">接口问答</option>
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="requirement_check">需求分析</option>
            </select>
          </label>
          <label>
            <span>标签</span>
            <input v-model="evalBatchFilters.tags" placeholder="api / alarm / refusal" />
          </label>
          <button type="button" @click="loadEvalRuns">查询</button>
        </div>
        <p v-if="evalRunError" class="error-text">{{ evalRunError }}</p>
        <div class="split-panel">
          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th>运行名称</th>
                  <th>状态</th>
                  <th>通过率</th>
                  <th>平均分</th>
                  <th>失败</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="evalRunLoading"><td colspan="6">加载中...</td></tr>
                <tr v-else-if="evalRuns.length === 0"><td colspan="6">暂无评测运行</td></tr>
                <template v-else>
                  <tr v-for="run in pagedItems(evalRuns, 'evalRuns')" :key="run.id">
                    <td>
                      <strong>{{ run.run_name }}</strong>
                      <small>{{ run.project_name || projectName(run.project_id) }} / {{ run.model_provider || '-' }} {{ run.model_name || '' }}</small>
                    </td>
                    <td><span class="status-pill" :class="statusClass(run.status)">{{ run.status }}</span></td>
                    <td>{{ run.pass_rate ?? 0 }}%</td>
                    <td>{{ run.average_score ?? 0 }}</td>
                    <td>{{ run.failed_cases ?? 0 }} / {{ run.total_cases ?? 0 }}</td>
                    <td>
                      <div class="row-actions">
                        <button type="button" @click="openEvalRun(run)">详情</button>
                        <button type="button" class="ghost-action" @click="rerunFailedEvalRun(run)">重跑失败</button>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
            <div v-if="evalRuns.length > pagination.evalRuns.pageSize" class="pagination-bar compact-pagination">
              <span>{{ pageStart(evalRuns.length, 'evalRuns') }}-{{ pageEnd(evalRuns.length, 'evalRuns') }} / {{ evalRuns.length }}</span>
              <label>
                <span>每页</span>
                <select v-model.number="pagination.evalRuns.pageSize" @change="resetPage('evalRuns')">
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <button type="button" :disabled="currentPage(evalRuns.length, 'evalRuns') === 1" @click="setPage('evalRuns', currentPage(evalRuns.length, 'evalRuns') - 1, evalRuns.length)">上一页</button>
              <strong>{{ currentPage(evalRuns.length, 'evalRuns') }} / {{ pageTotal(evalRuns.length, 'evalRuns') }}</strong>
              <button type="button" :disabled="currentPage(evalRuns.length, 'evalRuns') === pageTotal(evalRuns.length, 'evalRuns')" @click="setPage('evalRuns', currentPage(evalRuns.length, 'evalRuns') + 1, evalRuns.length)">下一页</button>
            </div>
          </div>
          <aside class="detail-card">
            <h3>{{ activeEvalRun?.run_name || '选择一次运行查看详情' }}</h3>
            <div v-if="activeEvalRun" class="metric-grid compact">
              <article>
                <span>通过率</span>
                <strong>{{ activeEvalRun.pass_rate ?? evalRunSummary?.pass_rate ?? 0 }}%</strong>
              </article>
              <article>
                <span>平均分</span>
                <strong>{{ activeEvalRun.average_score ?? evalRunSummary?.average_score ?? 0 }}</strong>
              </article>
              <article>
                <span>失败数</span>
                <strong>{{ activeEvalRun.failed_cases ?? evalRunSummary?.failed_count ?? 0 }}</strong>
              </article>
            </div>
            <div v-if="evalRunResults.length" class="result-stack">
              <article v-for="result in pagedItems(evalRunResults, 'evalRunResults')" :key="String(result.id)" class="result-card">
                <div class="result-card-header">
                  <strong>{{ result.question }}</strong>
                  <span :class="['status-pill', result.auto_passed ? 'status-ready' : 'status-failed']">
                    {{ result.auto_passed ? '通过' : '不通过' }} · {{ result.auto_score ?? 0 }}
                  </span>
                </div>
                <p>{{ result.actual_answer || result.error_message || '-' }}</p>
                <small>
                  关键词 {{ result.keyword_score ?? '-' }} / 来源 {{ result.source_score ?? '-' }} /
                  拒答 {{ result.refusal_score ?? '-' }} / 等级 {{ result.feasibility_score ?? '-' }}
                </small>
                <small>缺失：{{ textListValue(result.missing_keywords) || '-' }} {{ textListValue(result.missing_sources) || '' }}</small>
              </article>
              <div v-if="evalRunResults.length > pagination.evalRunResults.pageSize" class="pagination-bar compact-pagination">
                <span>{{ pageStart(evalRunResults.length, 'evalRunResults') }}-{{ pageEnd(evalRunResults.length, 'evalRunResults') }} / {{ evalRunResults.length }}</span>
                <button type="button" :disabled="currentPage(evalRunResults.length, 'evalRunResults') === 1" @click="setPage('evalRunResults', currentPage(evalRunResults.length, 'evalRunResults') - 1, evalRunResults.length)">上一页</button>
                <strong>{{ currentPage(evalRunResults.length, 'evalRunResults') }} / {{ pageTotal(evalRunResults.length, 'evalRunResults') }}</strong>
                <button type="button" :disabled="currentPage(evalRunResults.length, 'evalRunResults') === pageTotal(evalRunResults.length, 'evalRunResults')" @click="setPage('evalRunResults', currentPage(evalRunResults.length, 'evalRunResults') + 1, evalRunResults.length)">下一页</button>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section v-else-if="activeKey === 'model-configs'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">运行时模型选择</p>
            <h2>模型配置</h2>
          </div>
          <div class="row-actions">
            <button class="ghost-action" type="button" @click="loadModelConfigs">刷新</button>
            <button class="primary-action" type="button" @click="openModelConfigModal()">新增配置</button>
          </div>
        </div>
        <div class="filter-bar">
          <label>
            <span>Provider</span>
            <select v-model="modelConfigFilters.provider">
              <option value="">全部</option>
              <option value="deepseek">deepseek</option>
              <option value="openai">openai</option>
              <option value="openai_compatible">openai_compatible</option>
            </select>
          </label>
          <label>
            <span>模型类型</span>
            <select v-model="modelConfigFilters.model_type">
              <option value="">全部</option>
              <option value="chat">chat</option>
              <option value="embedding">embedding</option>
            </select>
          </label>
          <label>
            <span>启用</span>
            <select v-model="modelConfigFilters.enabled">
              <option value="">全部</option>
              <option value="true">启用</option>
              <option value="false">禁用</option>
            </select>
          </label>
          <button type="button" @click="loadModelConfigs">查询</button>
        </div>
        <p v-if="modelConfigError" class="error-text">{{ modelConfigError }}</p>
        <p v-if="modelConfigTestResult" class="success-text">测试结果：{{ formatCell(modelConfigTestResult) }}</p>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>配置名称</th>
                <th>Provider</th>
                <th>类型</th>
                <th>模型</th>
                <th>Base URL</th>
                <th>API Key</th>
                <th>默认</th>
                <th>启用</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="modelConfigLoading"><td colspan="9">加载中...</td></tr>
              <tr v-else-if="modelConfigs.length === 0"><td colspan="9">暂无模型配置</td></tr>
              <tr v-for="record in pagedItems(modelConfigs, 'modelConfigs')" v-else :key="record.id">
                <td>{{ record.config_name }}</td>
                <td>{{ record.provider }}</td>
                <td>{{ record.model_type }}</td>
                <td>{{ record.model_name || '-' }}<br /><small>{{ record.dimension ? `dim ${record.dimension}` : '' }}</small></td>
                <td>{{ record.base_url || '-' }}</td>
                <td>{{ record.api_key_masked || '-' }}</td>
                <td><span :class="['status-pill', record.default_config ? 'status-success' : '']">{{ record.default_config ? '默认' : '-' }}</span></td>
                <td>{{ record.enabled ? '是' : '否' }}</td>
                <td>
                  <div class="row-actions">
                    <button type="button" @click="testModelConfig(record)">测试</button>
                    <button type="button" @click="setDefaultModelConfig(record)">设默认</button>
                    <button type="button" @click="openModelConfigModal(record)">编辑</button>
                    <button type="button" class="danger-action" @click="deleteModelConfig(record)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="modelConfigs.length > pagination.modelConfigs.pageSize" class="pagination-bar">
          <span>显示 {{ pageStart(modelConfigs.length, 'modelConfigs') }}-{{ pageEnd(modelConfigs.length, 'modelConfigs') }} / {{ modelConfigs.length }}</span>
          <label>
            <span>每页</span>
            <select v-model.number="pagination.modelConfigs.pageSize" @change="resetPage('modelConfigs')">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
          <button type="button" :disabled="currentPage(modelConfigs.length, 'modelConfigs') === 1" @click="setPage('modelConfigs', currentPage(modelConfigs.length, 'modelConfigs') - 1, modelConfigs.length)">上一页</button>
          <strong>{{ currentPage(modelConfigs.length, 'modelConfigs') }} / {{ pageTotal(modelConfigs.length, 'modelConfigs') }}</strong>
          <button type="button" :disabled="currentPage(modelConfigs.length, 'modelConfigs') === pageTotal(modelConfigs.length, 'modelConfigs')" @click="setPage('modelConfigs', currentPage(modelConfigs.length, 'modelConfigs') + 1, modelConfigs.length)">下一页</button>
        </div>
      </section>

      <section v-else-if="activeKey === 'prompt-templates'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">变量化 Prompt</p>
            <h2>Prompt 模板</h2>
          </div>
          <div class="row-actions">
            <button class="ghost-action" type="button" @click="loadPromptTemplates">刷新</button>
            <button class="primary-action" type="button" @click="openPromptTemplateModal()">新增模板</button>
          </div>
        </div>
        <section class="variable-help">
          <strong>支持变量</strong>
          <p v-pre>{{question}}、{{project_name}}、{{mode}}、{{context}}、{{references}}、{{related_pages}}、{{related_capabilities}}、{{related_apis}}、{{related_tables}}、{{requirement_desc}}、{{module_name}}、{{current_time}}</p>
        </section>
        <div class="filter-bar">
          <label>
            <span>模板类型</span>
            <select v-model="promptTemplateFilters.template_type">
              <option value="">全部</option>
              <option value="doc_qa">doc_qa</option>
              <option value="page_help">page_help</option>
              <option value="business_qa">business_qa</option>
              <option value="requirement_check">requirement_check</option>
              <option value="chat">chat</option>
            </select>
          </label>
          <label>
            <span>Mode</span>
            <select v-model="promptTemplateFilters.mode">
              <option value="">全部</option>
              <option value="doc_qa">doc_qa</option>
              <option value="page_help">page_help</option>
              <option value="business_qa">business_qa</option>
              <option value="requirement_check">requirement_check</option>
            </select>
          </label>
          <label>
            <span>启用</span>
            <select v-model="promptTemplateFilters.enabled">
              <option value="">全部</option>
              <option value="true">启用</option>
              <option value="false">禁用</option>
            </select>
          </label>
          <button type="button" @click="loadPromptTemplates">查询</button>
        </div>
        <p v-if="promptTemplateError" class="error-text">{{ promptTemplateError }}</p>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>模板名称</th>
                <th>类型</th>
                <th>Mode</th>
                <th>输出</th>
                <th>默认</th>
                <th>启用</th>
                <th>备注</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="promptTemplateLoading"><td colspan="8">加载中...</td></tr>
              <tr v-else-if="promptTemplates.length === 0"><td colspan="8">暂无 Prompt 模板，系统将使用内置默认模板</td></tr>
              <tr v-for="record in pagedItems(promptTemplates, 'promptTemplates')" v-else :key="record.id">
                <td>{{ record.template_name }}</td>
                <td>{{ record.template_type }}</td>
                <td>{{ record.mode || '-' }}</td>
                <td>{{ record.output_format || '-' }}</td>
                <td><span :class="['status-pill', record.default_template ? 'status-success' : '']">{{ record.default_template ? '默认' : '-' }}</span></td>
                <td>{{ record.enabled ? '是' : '否' }}</td>
                <td>{{ record.remark || '-' }}</td>
                <td>
                  <div class="row-actions">
                    <button type="button" @click="openPromptTemplateTest(record)">测试</button>
                    <button type="button" @click="setDefaultPromptTemplate(record)">设默认</button>
                    <button type="button" @click="openPromptTemplateModal(record)">编辑</button>
                    <button type="button" class="danger-action" @click="deletePromptTemplate(record)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="promptTemplates.length > pagination.promptTemplates.pageSize" class="pagination-bar">
          <span>显示 {{ pageStart(promptTemplates.length, 'promptTemplates') }}-{{ pageEnd(promptTemplates.length, 'promptTemplates') }} / {{ promptTemplates.length }}</span>
          <label>
            <span>每页</span>
            <select v-model.number="pagination.promptTemplates.pageSize" @change="resetPage('promptTemplates')">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
          <button type="button" :disabled="currentPage(promptTemplates.length, 'promptTemplates') === 1" @click="setPage('promptTemplates', currentPage(promptTemplates.length, 'promptTemplates') - 1, promptTemplates.length)">上一页</button>
          <strong>{{ currentPage(promptTemplates.length, 'promptTemplates') }} / {{ pageTotal(promptTemplates.length, 'promptTemplates') }}</strong>
          <button type="button" :disabled="currentPage(promptTemplates.length, 'promptTemplates') === pageTotal(promptTemplates.length, 'promptTemplates')" @click="setPage('promptTemplates', currentPage(promptTemplates.length, 'promptTemplates') + 1, promptTemplates.length)">下一页</button>
        </div>
      </section>

      <section v-else-if="activeKey === 'chat-history'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">问答可观测性</p>
            <h2>对话记录</h2>
          </div>
          <button class="ghost-action" type="button" @click="loadChatSessions">刷新</button>
        </div>
        <div class="filter-bar">
          <label>
            <span>项目</span>
            <select v-model="chatHistoryFilters.project_id">
              <option value="">全部项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模式</span>
            <select v-model="chatHistoryFilters.mode">
              <option value="">全部模式</option>
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="business_qa">业务问答</option>
              <option value="requirement_check">需求分析</option>
            </select>
          </label>
          <label>
            <span>关键词</span>
            <input v-model="chatHistoryFilters.keyword" placeholder="问题或回答关键词" />
          </label>
          <label>
            <span>开始时间</span>
            <input v-model="chatHistoryFilters.start_time" type="datetime-local" />
          </label>
          <label>
            <span>结束时间</span>
            <input v-model="chatHistoryFilters.end_time" type="datetime-local" />
          </label>
          <button type="button" @click="loadChatSessions">查询</button>
        </div>
        <p v-if="chatHistoryError" class="error-text">{{ chatHistoryError }}</p>
        <div class="chat-history-metrics">
          <article v-for="metric in chatHistorySummary" :key="metric.label">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </article>
        </div>
        <div class="conversation-console">
          <aside class="session-rail">
            <div class="session-rail-head">
              <div>
                <p class="panel-label">会话队列</p>
                <h3>按更新时间排序</h3>
              </div>
              <span class="status-pill">{{ chatSessions.length }} 条</span>
            </div>
            <div class="session-list">
              <p v-if="chatHistoryLoading" class="muted-text">加载中...</p>
              <p v-else-if="chatSessions.length === 0" class="muted-text">暂无对话记录</p>
              <button v-for="session in pagedItems(chatSessions, 'chatSessions')" v-else :key="session.session_id" type="button" :class="['session-card', { active: activeChatSession?.session_id === session.session_id }]" @click="openChatSession(session)">
                <span class="status-pill">{{ session.mode }}</span>
                <strong>{{ session.last_question || '未命名会话' }}</strong>
                <small>{{ session.project_name || projectName(session.project_id) }}</small>
                <p>{{ session.last_answer_preview || '暂无回答摘要' }}</p>
                <em>{{ session.question_count }} 问 · {{ session.updated_at || '-' }}</em>
              </button>
            </div>
            <div v-if="chatSessions.length > pagination.chatSessions.pageSize" class="pagination-bar compact-pagination vertical-pagination">
              <span>{{ pageStart(chatSessions.length, 'chatSessions') }}-{{ pageEnd(chatSessions.length, 'chatSessions') }} / {{ chatSessions.length }}</span>
              <button type="button" :disabled="currentPage(chatSessions.length, 'chatSessions') === 1" @click="setPage('chatSessions', currentPage(chatSessions.length, 'chatSessions') - 1, chatSessions.length)">上一页</button>
              <strong>{{ currentPage(chatSessions.length, 'chatSessions') }} / {{ pageTotal(chatSessions.length, 'chatSessions') }}</strong>
              <button type="button" :disabled="currentPage(chatSessions.length, 'chatSessions') === pageTotal(chatSessions.length, 'chatSessions')" @click="setPage('chatSessions', currentPage(chatSessions.length, 'chatSessions') + 1, chatSessions.length)">下一页</button>
            </div>
            <button v-if="activeChatSession" type="button" class="danger-action full-action" @click="deleteChatSession(activeChatSession)">删除当前会话</button>
          </aside>

          <aside class="dialogue-timeline">
            <div class="section-title-row">
              <div>
                <p class="panel-label">消息时间线</p>
                <h3>{{ activeChatSession?.last_question || '选择左侧会话查看上下文' }}</h3>
              </div>
              <span v-if="activeChatSession" class="status-pill">{{ activeChatSession.mode }}</span>
            </div>
            <div v-if="activeChatSession" class="meta-grid">
              <span>项目：{{ activeChatSession.project_name || projectName(activeChatSession.project_id) }}</span>
              <span>会话：{{ activeChatSession.session_id }}</span>
            </div>
            <div class="chat-thread">
              <article v-for="message in pagedItems(chatSessionMessages, 'chatSessionMessages')" :key="message.id" :class="['message-card', message.role]">
                <div class="section-title-row">
                  <strong>{{ message.role === 'user' ? '用户问题' : 'AI 回答' }}</strong>
                  <small>{{ message.created_at || '-' }}</small>
                </div>
                <p>{{ message.content }}</p>
                <div v-if="message.role === 'assistant'" class="meta-grid">
                  <span>Trace：{{ message.trace_id || '-' }}</span>
                  <span>模型：{{ message.llm_provider || '-' }}</span>
                  <span>检索策略：{{ message.search_strategy || '-' }}</span>
                  <span>资料不足拒答：{{ message.insufficient_answer ? '是' : '否' }}</span>
                </div>
                <div v-if="message.references?.length" class="mini-reference-list">
                  <button v-for="reference in message.references" :key="`${reference.source_type}-${reference.source_id}`" type="button" @click="openReferenceModal(reference)">
                    {{ formatReference(reference) }}
                  </button>
                </div>
                <p v-else-if="message.role === 'assistant' && message.insufficient_answer" class="error-text">拒答原因：未检索到可支撑结论的引用来源。</p>
              </article>
              <p v-if="activeChatSession && chatSessionMessages.length === 0">暂无消息</p>
            </div>
            <div v-if="chatSessionMessages.length > pagination.chatSessionMessages.pageSize" class="pagination-bar compact-pagination">
              <span>{{ pageStart(chatSessionMessages.length, 'chatSessionMessages') }}-{{ pageEnd(chatSessionMessages.length, 'chatSessionMessages') }} / {{ chatSessionMessages.length }}</span>
              <label>
                <span>每页</span>
                <select v-model.number="pagination.chatSessionMessages.pageSize" @change="resetPage('chatSessionMessages')">
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <button type="button" :disabled="currentPage(chatSessionMessages.length, 'chatSessionMessages') === 1" @click="setPage('chatSessionMessages', currentPage(chatSessionMessages.length, 'chatSessionMessages') - 1, chatSessionMessages.length)">上一页</button>
              <strong>{{ currentPage(chatSessionMessages.length, 'chatSessionMessages') }} / {{ pageTotal(chatSessionMessages.length, 'chatSessionMessages') }}</strong>
              <button type="button" :disabled="currentPage(chatSessionMessages.length, 'chatSessionMessages') === pageTotal(chatSessionMessages.length, 'chatSessionMessages')" @click="setPage('chatSessionMessages', currentPage(chatSessionMessages.length, 'chatSessionMessages') + 1, chatSessionMessages.length)">下一页</button>
            </div>
          </aside>
        </div>
      </section>

      <section v-else-if="activeKey === 'feedback'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">问答修正与知识沉淀</p>
            <h2>反馈管理</h2>
          </div>
          <button class="ghost-action" type="button" @click="loadFeedback">刷新</button>
        </div>
        <div class="filter-bar">
          <label>
            <span>项目</span>
            <select v-model="feedbackFilters.project_id">
              <option value="">全部项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>反馈类型</span>
            <select v-model="feedbackFilters.feedback_type">
              <option value="">全部类型</option>
              <option value="useful">有用</option>
              <option value="not_useful">无用</option>
              <option value="wrong">回答错误</option>
              <option value="incomplete">回答不完整</option>
              <option value="no_source">引用不准确</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>
            <span>复核状态</span>
            <select v-model="feedbackFilters.review_status">
              <option value="">全部状态</option>
              <option value="pending">pending</option>
              <option value="accepted">accepted</option>
              <option value="rejected">rejected</option>
              <option value="converted">converted</option>
            </select>
          </label>
          <label>
            <span>关键词</span>
            <input v-model="feedbackFilters.keyword" placeholder="问题、回答、备注" />
          </label>
          <label>
            <span>开始时间</span>
            <input v-model="feedbackFilters.start_time" type="datetime-local" />
          </label>
          <label>
            <span>结束时间</span>
            <input v-model="feedbackFilters.end_time" type="datetime-local" />
          </label>
          <button type="button" @click="loadFeedback">查询</button>
        </div>
        <p v-if="feedbackError" class="error-text">{{ feedbackError }}</p>
        <div class="feedback-command">
          <div class="feedback-summary-strip">
            <article v-for="metric in feedbackSummary" :key="metric.label">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          </div>
          <div class="feedback-board">
            <p v-if="feedbackLoading" class="muted-text">加载中...</p>
            <p v-else-if="feedbackRecords.length === 0" class="muted-text">暂无反馈</p>
            <template v-else>
              <div class="feedback-queue-head">
                <div>
                  <p class="panel-label">复核队列</p>
                  <h3>按时间处理用户反馈</h3>
                </div>
                <div class="queue-status-counts">
                  <span v-for="column in feedbackColumns" :key="column.key" :class="['status-pill', statusClass(column.key)]">{{ column.title }} {{ column.records.length }}</span>
                </div>
              </div>
              <article v-for="record in pagedItems(feedbackRecords, 'feedbackRecords')" :key="record.id" :class="['feedback-ticket', { active: activeFeedback?.id === record.id }]">
                <button type="button" class="ticket-main" @click="openFeedbackDetail(record)">
                  <div class="ticket-title-row">
                    <strong>{{ record.question || '未关联问题' }}</strong>
                    <span :class="['status-pill', statusClass(record.review_status)]">{{ record.review_status }}</span>
                  </div>
                  <small>{{ record.project_name || projectName(record.project_id) }} / {{ record.feedback_type }}</small>
                  <p>{{ record.corrected_answer || record.remark || record.answer || '-' }}</p>
                  <em>{{ record.target_knowledge_type || '未选择知识类型' }} · {{ record.created_at || '-' }}</em>
                </button>
                <div class="ticket-actions">
                  <button type="button" @click="openFeedbackReview(record, 'accepted')">复核</button>
                  <button type="button" @click="convertFeedback(record)">转知识库</button>
                </div>
              </article>
              <div v-if="feedbackRecords.length > pagination.feedbackRecords.pageSize" class="pagination-bar compact-pagination">
                <span>{{ pageStart(feedbackRecords.length, 'feedbackRecords') }}-{{ pageEnd(feedbackRecords.length, 'feedbackRecords') }} / {{ feedbackRecords.length }}</span>
                <label>
                  <span>每页</span>
                  <select v-model.number="pagination.feedbackRecords.pageSize" @change="resetPage('feedbackRecords')">
                    <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
                  </select>
                </label>
                <button type="button" :disabled="currentPage(feedbackRecords.length, 'feedbackRecords') === 1" @click="setPage('feedbackRecords', currentPage(feedbackRecords.length, 'feedbackRecords') - 1, feedbackRecords.length)">上一页</button>
                <strong>{{ currentPage(feedbackRecords.length, 'feedbackRecords') }} / {{ pageTotal(feedbackRecords.length, 'feedbackRecords') }}</strong>
                <button type="button" :disabled="currentPage(feedbackRecords.length, 'feedbackRecords') === pageTotal(feedbackRecords.length, 'feedbackRecords')" @click="setPage('feedbackRecords', currentPage(feedbackRecords.length, 'feedbackRecords') + 1, feedbackRecords.length)">下一页</button>
              </div>
            </template>
          </div>

          <aside class="feedback-review-panel feedback-detail">
            <div class="section-title-row">
              <div>
                <p class="panel-label">复核处理单</p>
                <h3>{{ activeFeedback?.question || '选择一条反馈进入复核' }}</h3>
              </div>
              <span v-if="activeFeedback" :class="['status-pill', statusClass(activeFeedback.review_status)]">{{ activeFeedback.review_status }}</span>
            </div>
            <template v-if="activeFeedback">
              <dl>
                <div><dt>反馈类型</dt><dd>{{ activeFeedback.feedback_type }}</dd></div>
                <div><dt>模型</dt><dd>{{ activeFeedback.llm_provider || '-' }}</dd></div>
                <div><dt>检索策略</dt><dd>{{ activeFeedback.search_strategy || '-' }}</dd></div>
              </dl>
              <section>
                <span>AI 回答</span>
                <p>{{ activeFeedback.answer || '-' }}</p>
              </section>
              <section>
                <span>用户纠正</span>
                <p>{{ activeFeedback.corrected_answer || activeFeedback.remark || '-' }}</p>
              </section>
              <section>
                <span>引用来源</span>
                <div class="mini-reference-list">
                  <button v-for="reference in activeFeedback.references || []" :key="`${reference.source_type}-${reference.source_id}`" type="button" @click="openReferenceModal(reference)">
                    {{ formatReference(reference) }}
                  </button>
                </div>
                <p v-if="!activeFeedback.references?.length" class="error-text">无引用来源</p>
              </section>
              <section>
                <span>复核备注</span>
                <p>{{ activeFeedback.review_remark || '-' }}</p>
              </section>
              <div class="row-actions">
                <button type="button" @click="openFeedbackReview(activeFeedback, 'accepted')">通过</button>
                <button type="button" class="danger-action" @click="openFeedbackReview(activeFeedback, 'rejected')">驳回</button>
                <button type="button" @click="convertFeedback(activeFeedback)">转知识库</button>
              </div>
            </template>
          </aside>
        </div>
      </section>

      <section v-else-if="activeKey === 'tasks'" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">解析与索引任务</p>
            <h2>任务中心</h2>
          </div>
          <button class="ghost-action" type="button" @click="loadTasks">刷新</button>
        </div>
        <div class="filter-bar">
          <label>
            <span>任务类型</span>
            <select v-model="taskFilters.task_type">
              <option value="">全部类型</option>
              <option value="document_parse">document_parse</option>
              <option value="document_reparse">document_reparse</option>
              <option value="document_index">document_index</option>
              <option value="document_reindex">document_reindex</option>
              <option value="document_reindex_all">document_reindex_all</option>
            </select>
          </label>
          <label>
            <span>状态</span>
            <select v-model="taskFilters.status">
              <option value="">全部状态</option>
              <option value="pending">pending</option>
              <option value="running">running</option>
              <option value="success">success</option>
              <option value="failed">failed</option>
              <option value="cancelled">cancelled</option>
            </select>
          </label>
          <label>
            <span>业务 ID</span>
            <input v-model="taskFilters.biz_id" placeholder="biz_id" />
          </label>
          <button type="button" @click="loadTasks">查询</button>
        </div>
        <p v-if="taskError" class="error-text">{{ taskError }}</p>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>任务类型</th>
                <th>业务对象</th>
                <th>状态</th>
                <th>进度</th>
                <th>消息</th>
                <th>错误</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="taskLoading"><td colspan="8">加载中...</td></tr>
              <tr v-else-if="taskRecords.length === 0"><td colspan="8">暂无任务</td></tr>
              <tr v-for="task in pagedItems(taskRecords, 'taskRecords')" v-else :key="task.id">
                <td>{{ task.task_type }}</td>
                <td>{{ task.biz_type || '-' }}<br /><small>{{ task.biz_id || '-' }}</small></td>
                <td><span :class="['status-pill', statusClass(task.status)]">{{ task.status }}</span></td>
                <td>
                  <div class="task-progress"><span :style="{ width: `${task.progress || 0}%` }"></span></div>
                  <small>{{ task.progress || 0 }}%</small>
                </td>
                <td>{{ task.message || '-' }}</td>
                <td>{{ task.error_message || '-' }}</td>
                <td>{{ task.created_at || '-' }}</td>
                <td>
                  <div class="row-actions">
                    <button type="button" @click="openTaskModal(task.id)">详情</button>
                    <button v-if="task.status === 'failed'" type="button" @click="retryTask(task)">重试</button>
                    <button v-if="task.status === 'pending'" type="button" class="danger-action" @click="cancelTask(task)">取消</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="taskRecords.length > pagination.taskRecords.pageSize" class="pagination-bar">
          <span>显示 {{ pageStart(taskRecords.length, 'taskRecords') }}-{{ pageEnd(taskRecords.length, 'taskRecords') }} / {{ taskRecords.length }}</span>
          <label>
            <span>每页</span>
            <select v-model.number="pagination.taskRecords.pageSize" @change="resetPage('taskRecords')">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
          <button type="button" :disabled="currentPage(taskRecords.length, 'taskRecords') === 1" @click="setPage('taskRecords', currentPage(taskRecords.length, 'taskRecords') - 1, taskRecords.length)">上一页</button>
          <strong>{{ currentPage(taskRecords.length, 'taskRecords') }} / {{ pageTotal(taskRecords.length, 'taskRecords') }}</strong>
          <button type="button" :disabled="currentPage(taskRecords.length, 'taskRecords') === pageTotal(taskRecords.length, 'taskRecords')" @click="setPage('taskRecords', currentPage(taskRecords.length, 'taskRecords') + 1, taskRecords.length)">下一页</button>
        </div>
      </section>

      <section v-else-if="activeModule" class="manager-panel">
        <div class="manager-header">
          <div>
            <p class="panel-label">结构化数据管理</p>
            <h2>{{ activeModule.title }}</h2>
          </div>
          <button class="primary-action" type="button" @click="openCreateModal">新增</button>
        </div>

        <section v-if="activeKey === 'documents'" class="upload-panel">
          <div>
            <p class="panel-label">上传后自动解析切片</p>
            <h3>文档入库</h3>
          </div>
          <div class="upload-grid">
            <label>
              <span>项目</span>
              <select v-model="uploadForm.project_id">
                <option value="">请选择项目</option>
                <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
              </select>
            </label>
            <label>
              <span>模块</span>
              <input v-model="uploadForm.module_name" placeholder="如 巡检管理" />
            </label>
            <label>
              <span>类型</span>
              <select v-model="uploadForm.document_type">
                <option value="manual">操作手册</option>
                <option value="requirement">需求资料</option>
                <option value="api">接口说明</option>
                <option value="database">数据库说明</option>
              </select>
            </label>
            <label>
              <span>文件</span>
              <input type="file" accept=".txt,.md,.pdf,.doc,.docx,.xlsx,.csv,.json" @change="handleUploadFile" />
            </label>
            <button class="primary-action" type="button" :disabled="uploadLoading" @click="uploadDocument">
              {{ uploadLoading ? '处理中...' : '上传并解析' }}
            </button>
          </div>
          <p v-if="uploadError" class="error-text">{{ uploadError }}</p>
          <p v-if="uploadResult" class="success-text">
            已入库：{{ uploadResult.document.document_name }}，解析任务 {{ uploadResult.task_id || '-' }}
          </p>
        </section>

        <div class="filter-bar">
          <label v-if="activeModule.requiresProject">
            <span>项目</span>
            <select v-model="filters.project_id">
              <option value="">全部项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">
                {{ project.name }}
              </option>
            </select>
          </label>
          <label>
            <span>模块</span>
            <input v-model="filters.module_name" placeholder="module_name" />
          </label>
          <label>
            <span>关键词</span>
            <input v-model="filters.keyword" placeholder="名称、说明、关键词" />
          </label>
          <button type="button" @click="loadCurrentModule">查询</button>
        </div>

        <p v-if="dataError" class="error-text">{{ dataError }}</p>
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th v-for="column in activeModule.columns" :key="column">{{ fieldLabel(activeModule, column) }}</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dataLoading">
                <td :colspan="activeModule.columns.length + 1">加载中...</td>
              </tr>
              <tr v-else-if="records.length === 0">
                <td :colspan="activeModule.columns.length + 1">暂无数据</td>
              </tr>
              <template v-else>
                <tr v-for="record in pagedItems(records, 'records')" :key="String(record.id)">
                  <td v-for="column in activeModule.columns" :key="column">
                    <span v-if="column === 'parse_status' || column === 'index_status'" :class="['status-pill', statusClass(record[column])]">
                      {{ formatCell(record[column]) }}
                    </span>
                    <span v-else>{{ formatCell(record[column]) }}</span>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button v-if="activeKey === 'documents'" type="button" @click="parseDocument(record)">解析</button>
                      <button v-if="activeKey === 'documents'" type="button" @click="parseDocument(record)">重新解析</button>
                      <button v-if="activeKey === 'documents'" type="button" @click="reindexDocument(record)">重建索引</button>
                      <button v-if="activeKey === 'documents'" type="button" @click="openChunkModal(record)">切片</button>
                      <button v-if="activeKey === 'documents'" type="button" @click="openDocumentTasks(record)">任务</button>
                      <button v-if="activeKey === 'documents'" type="button" @click="downloadDocument(record)">下载</button>
                      <button type="button" @click="openEditModal(record)">编辑</button>
                      <button type="button" class="danger-action" @click="deleteRecord(record)">删除</button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <div v-if="records.length > pagination.records.pageSize" class="pagination-bar">
          <span>显示 {{ pageStart(records.length, 'records') }}-{{ pageEnd(records.length, 'records') }} / {{ records.length }}</span>
          <label>
            <span>每页</span>
            <select v-model.number="pagination.records.pageSize" @change="resetPage('records')">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
          <button type="button" :disabled="currentPage(records.length, 'records') === 1" @click="setPage('records', currentPage(records.length, 'records') - 1, records.length)">上一页</button>
          <strong>{{ currentPage(records.length, 'records') }} / {{ pageTotal(records.length, 'records') }}</strong>
          <button type="button" :disabled="currentPage(records.length, 'records') === pageTotal(records.length, 'records')" @click="setPage('records', currentPage(records.length, 'records') + 1, records.length)">下一页</button>
        </div>
      </section>
    </section>

    <div v-if="modalOpen && activeModule" class="modal-backdrop" @click.self="closeModal">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">{{ editingRecord ? '编辑' : '新增' }}</p>
            <h2>{{ activeModule.title }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeModal">关闭</button>
        </header>

        <form class="form-grid" @submit.prevent="saveRecord">
          <label v-for="field in activeModule.fields" :key="field.key" :class="{ wide: field.type === 'textarea' }">
            <span>{{ field.label }}</span>
            <input v-if="field.type === 'checkbox'" :checked="Boolean(formState[field.key])" type="checkbox" @change="updateCheckboxValue(field.key, $event)" />
            <select v-else-if="field.type === 'project'" :value="String(formState[field.key] ?? '')" :required="field.required" @change="updateFieldValue(field.key, $event)">
              <option value="">请选择项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">
                {{ project.name }}
              </option>
            </select>
            <select v-else-if="field.options" :value="String(formState[field.key] ?? '')" :required="field.required" @change="updateFieldValue(field.key, $event)">
              <option v-for="option in field.options" :key="option" :value="option">{{ option }}</option>
            </select>
            <textarea v-else-if="field.type === 'textarea'" :value="String(formState[field.key] ?? '')" :required="field.required" rows="4" @input="updateFieldValue(field.key, $event)"></textarea>
            <input v-else :value="String(formState[field.key] ?? '')" :type="field.type === 'number' ? 'number' : 'text'" :required="field.required" @input="updateFieldValue(field.key, $event)" />
          </label>

          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeModal">取消</button>
            <button type="submit" class="primary-action">保存</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="chunkModalOpen" class="modal-backdrop" @click.self="closeChunkModal">
      <section class="modal-panel chunk-panel">
        <header>
          <div>
            <p class="panel-label">ai_document_chunk</p>
            <h2>{{ chunkDocumentTitle }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeChunkModal">关闭</button>
        </header>
        <p v-if="chunkLoading">加载中...</p>
        <p v-else-if="chunkError" class="error-text">{{ chunkError }}</p>
        <div v-else class="chunk-list">
          <article v-for="chunk in pagedItems(documentChunks, 'documentChunks')" :key="chunk.id">
            <div>
              <strong>#{{ chunk.chunk_index }}</strong>
              <span>{{ chunk.section_title || '未识别章节' }} / {{ chunk.index_status || '-' }}</span>
            </div>
            <small>{{ chunk.source_locator }}</small>
            <p>{{ chunk.content }}</p>
          </article>
          <p v-if="documentChunks.length === 0">暂无切片</p>
          <div v-if="documentChunks.length > pagination.documentChunks.pageSize" class="pagination-bar compact-pagination">
            <span>{{ pageStart(documentChunks.length, 'documentChunks') }}-{{ pageEnd(documentChunks.length, 'documentChunks') }} / {{ documentChunks.length }}</span>
            <label>
              <span>每页</span>
              <select v-model.number="pagination.documentChunks.pageSize" @change="resetPage('documentChunks')">
                <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
              </select>
            </label>
            <button type="button" :disabled="currentPage(documentChunks.length, 'documentChunks') === 1" @click="setPage('documentChunks', currentPage(documentChunks.length, 'documentChunks') - 1, documentChunks.length)">上一页</button>
            <strong>{{ currentPage(documentChunks.length, 'documentChunks') }} / {{ pageTotal(documentChunks.length, 'documentChunks') }}</strong>
            <button type="button" :disabled="currentPage(documentChunks.length, 'documentChunks') === pageTotal(documentChunks.length, 'documentChunks')" @click="setPage('documentChunks', currentPage(documentChunks.length, 'documentChunks') + 1, documentChunks.length)">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <div v-if="taskModalOpen" class="modal-backdrop" @click.self="closeTaskModal">
      <section class="modal-panel chunk-panel">
        <header>
          <div>
            <p class="panel-label">ai_task</p>
            <h2>{{ activeTask?.task_type || '任务详情' }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeTaskModal">关闭</button>
        </header>
        <div v-if="activeTask" class="task-detail">
          <dl>
            <div><dt>任务 ID</dt><dd>{{ activeTask.id }}</dd></div>
            <div><dt>状态</dt><dd><span :class="['status-pill', statusClass(activeTask.status)]">{{ activeTask.status }}</span></dd></div>
            <div><dt>进度</dt><dd>{{ activeTask.progress }}%</dd></div>
            <div><dt>业务对象</dt><dd>{{ activeTask.biz_type || '-' }} / {{ activeTask.biz_id || '-' }}</dd></div>
            <div><dt>开始时间</dt><dd>{{ activeTask.started_at || '-' }}</dd></div>
            <div><dt>结束时间</dt><dd>{{ activeTask.finished_at || '-' }}</dd></div>
          </dl>
          <div class="task-progress large"><span :style="{ width: `${activeTask.progress || 0}%` }"></span></div>
          <p>{{ activeTask.message || '-' }}</p>
          <p v-if="activeTask.error_message" class="error-text">{{ activeTask.error_message }}</p>
          <section class="task-log-list">
            <h3>任务日志</h3>
            <article v-for="log in pagedItems(taskLogs, 'taskLogs')" :key="log.id" :class="`log-${log.log_level}`">
              <span>{{ log.created_at }}</span>
              <strong>{{ log.log_level }}</strong>
              <p>{{ log.message }}</p>
            </article>
            <p v-if="taskLogs.length === 0">暂无日志</p>
            <div v-if="taskLogs.length > pagination.taskLogs.pageSize" class="pagination-bar compact-pagination">
              <span>{{ pageStart(taskLogs.length, 'taskLogs') }}-{{ pageEnd(taskLogs.length, 'taskLogs') }} / {{ taskLogs.length }}</span>
              <label>
                <span>每页</span>
                <select v-model.number="pagination.taskLogs.pageSize" @change="resetPage('taskLogs')">
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <button type="button" :disabled="currentPage(taskLogs.length, 'taskLogs') === 1" @click="setPage('taskLogs', currentPage(taskLogs.length, 'taskLogs') - 1, taskLogs.length)">上一页</button>
              <strong>{{ currentPage(taskLogs.length, 'taskLogs') }} / {{ pageTotal(taskLogs.length, 'taskLogs') }}</strong>
              <button type="button" :disabled="currentPage(taskLogs.length, 'taskLogs') === pageTotal(taskLogs.length, 'taskLogs')" @click="setPage('taskLogs', currentPage(taskLogs.length, 'taskLogs') + 1, taskLogs.length)">下一页</button>
            </div>
          </section>
        </div>
        <p v-else>加载中...</p>
      </section>
    </div>

    <div v-if="modelConfigModalOpen" class="modal-backdrop" @click.self="closeModelConfigModal">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">{{ editingModelConfig ? '编辑模型' : '新增模型' }}</p>
            <h2>模型配置</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeModelConfigModal">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="saveModelConfig">
          <label>
            <span>配置名称</span>
            <input v-model="modelConfigForm.config_name" required />
          </label>
          <label>
            <span>Provider</span>
            <select v-model="modelConfigForm.provider">
              <option value="deepseek">deepseek</option>
              <option value="openai">openai</option>
              <option value="openai_compatible">openai_compatible</option>
            </select>
          </label>
          <label>
            <span>模型类型</span>
            <select v-model="modelConfigForm.model_type">
              <option value="chat">chat</option>
              <option value="embedding">embedding</option>
            </select>
          </label>
          <label>
            <span>模型名称</span>
            <input v-model="modelConfigForm.model_name" placeholder="deepseek-chat / text-embedding-3-small" />
          </label>
          <label class="wide">
            <span>Base URL</span>
            <input v-model="modelConfigForm.base_url" placeholder="https://api.deepseek.com 或兼容 OpenAI 服务地址" />
          </label>
          <label class="wide">
            <span>API Key</span>
            <input v-model="modelConfigForm.api_key" type="password" :placeholder="editingModelConfig ? '留空则保留原密钥' : '请输入真实模型 API Key'" />
          </label>
          <label>
            <span>向量维度</span>
            <input v-model.number="modelConfigForm.dimension" min="1" type="number" />
          </label>
          <label>
            <span>Temperature</span>
            <input v-model.number="modelConfigForm.temperature" min="0" max="2" step="0.1" type="number" />
          </label>
          <label>
            <span>Max Tokens</span>
            <input v-model.number="modelConfigForm.max_tokens" min="1" type="number" />
          </label>
          <label>
            <span>启用</span>
            <input v-model="modelConfigForm.enabled" type="checkbox" />
          </label>
          <label>
            <span>默认模型</span>
            <input v-model="modelConfigForm.default_config" type="checkbox" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model="modelConfigForm.remark" rows="3"></textarea>
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeModelConfigModal">取消</button>
            <button type="submit" class="primary-action">保存</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="promptTemplateModalOpen" class="modal-backdrop" @click.self="closePromptTemplateModal">
      <section class="modal-panel chunk-panel">
        <header>
          <div>
            <p class="panel-label">{{ editingPromptTemplate ? '编辑 Prompt' : '新增 Prompt' }}</p>
            <h2>Prompt 模板</h2>
          </div>
          <button type="button" class="ghost-action" @click="closePromptTemplateModal">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="savePromptTemplate">
          <label>
            <span>模板名称</span>
            <input v-model="promptTemplateForm.template_name" required />
          </label>
          <label>
            <span>模板类型</span>
            <select v-model="promptTemplateForm.template_type">
              <option value="doc_qa">doc_qa</option>
              <option value="page_help">page_help</option>
              <option value="business_qa">business_qa</option>
              <option value="requirement_check">requirement_check</option>
              <option value="chat">chat</option>
            </select>
          </label>
          <label>
            <span>Mode</span>
            <select v-model="promptTemplateForm.mode">
              <option value="">通用</option>
              <option value="doc_qa">doc_qa</option>
              <option value="page_help">page_help</option>
              <option value="business_qa">business_qa</option>
              <option value="requirement_check">requirement_check</option>
            </select>
          </label>
          <label>
            <span>输出格式</span>
            <select v-model="promptTemplateForm.output_format">
              <option value="markdown">markdown</option>
              <option value="json">json</option>
              <option value="structured_json">structured_json</option>
            </select>
          </label>
          <label class="wide">
            <span>System Prompt</span>
            <textarea v-model="promptTemplateForm.system_prompt" rows="5"></textarea>
          </label>
          <label class="wide">
            <span>User Prompt Template</span>
            <textarea v-model="promptTemplateForm.user_prompt_template" required rows="9"></textarea>
          </label>
          <label>
            <span>启用</span>
            <input v-model="promptTemplateForm.enabled" type="checkbox" />
          </label>
          <label>
            <span>默认模板</span>
            <input v-model="promptTemplateForm.default_template" type="checkbox" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model="promptTemplateForm.remark" rows="3"></textarea>
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closePromptTemplateModal">取消</button>
            <button type="submit" class="primary-action">保存</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="promptTemplateTestOpen" class="modal-backdrop" @click.self="closePromptTemplateTest">
      <section class="modal-panel chunk-panel">
        <header>
          <div>
            <p class="panel-label">模板渲染测试</p>
            <h2>{{ activePromptTemplate?.template_name }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closePromptTemplateTest">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="testPromptTemplate">
          <label>
            <span>项目名称</span>
            <input v-model="promptTemplateTestForm.project_name" />
          </label>
          <label>
            <span>Mode</span>
            <input v-model="promptTemplateTestForm.mode" />
          </label>
          <label class="wide">
            <span>Question</span>
            <textarea v-model="promptTemplateTestForm.question" rows="2"></textarea>
          </label>
          <label class="wide">
            <span>Requirement Desc</span>
            <textarea v-model="promptTemplateTestForm.requirement_desc" rows="2"></textarea>
          </label>
          <label class="wide">
            <span>References</span>
            <textarea v-model="promptTemplateTestForm.references" rows="3"></textarea>
          </label>
          <label class="wide">
            <span>Context</span>
            <textarea v-model="promptTemplateTestForm.context" rows="4"></textarea>
          </label>
          <label>
            <span>Module Name</span>
            <input v-model="promptTemplateTestForm.module_name" />
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closePromptTemplateTest">取消</button>
            <button type="submit" class="primary-action">测试渲染</button>
          </footer>
        </form>
        <section v-if="promptTemplateTestResult" class="render-preview">
          <article>
            <strong>System Prompt</strong>
            <p>{{ promptTemplateTestResult.rendered_system_prompt }}</p>
          </article>
          <article>
            <strong>User Prompt</strong>
            <p>{{ promptTemplateTestResult.rendered_user_prompt }}</p>
          </article>
        </section>
      </section>
    </div>

    <div v-if="feedbackModalOpen" class="modal-backdrop" @click.self="closeFeedbackModal">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">回答反馈</p>
            <h2>提交问答反馈</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeFeedbackModal">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="submitFeedback">
          <label>
            <span>反馈类型</span>
            <select v-model="feedbackForm.feedback_type">
              <option value="useful">有用</option>
              <option value="not_useful">无用</option>
              <option value="wrong">回答错误</option>
              <option value="incomplete">回答不完整</option>
              <option value="no_source">引用不准确</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>
            <span>转知识库</span>
            <input v-model="feedbackForm.convert_to_knowledge" type="checkbox" />
          </label>
          <label v-if="feedbackForm.convert_to_knowledge">
            <span>目标知识类型</span>
            <select v-model="feedbackForm.target_knowledge_type">
              <option value="">请选择</option>
              <option value="document_faq">文档 FAQ</option>
              <option value="page">页面说明</option>
              <option value="capability">能力清单</option>
              <option value="api">接口说明</option>
              <option value="db_table">数据表说明</option>
              <option value="requirement_case">历史需求案例</option>
            </select>
          </label>
          <label class="wide">
            <span>反馈备注</span>
            <textarea v-model="feedbackForm.remark" rows="3" placeholder="说明哪里有问题，或为什么有用"></textarea>
          </label>
          <label class="wide">
            <span>纠正答案</span>
            <textarea v-model="feedbackForm.corrected_answer" rows="5" placeholder="可填写期望回答，后续可转入知识库草稿"></textarea>
          </label>
          <label class="wide">
            <span>期望引用 JSON</span>
            <textarea v-model="feedbackForm.expected_sources" rows="4"></textarea>
          </label>
          <p v-if="feedbackError" class="error-text">{{ feedbackError }}</p>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeFeedbackModal">取消</button>
            <button type="submit" class="primary-action">提交反馈</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="feedbackReviewModalOpen" class="modal-backdrop" @click.self="closeFeedbackReview">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">反馈复核</p>
            <h2>{{ activeFeedback?.question || '复核反馈' }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeFeedbackReview">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="saveFeedbackReview">
          <label>
            <span>复核状态</span>
            <select v-model="feedbackReviewForm.review_status">
              <option value="pending">pending</option>
              <option value="accepted">accepted</option>
              <option value="rejected">rejected</option>
              <option value="converted">converted</option>
            </select>
          </label>
          <label>
            <span>复核人</span>
            <input v-model="feedbackReviewForm.reviewer" placeholder="可选" />
          </label>
          <label>
            <span>目标知识类型</span>
            <select v-model="feedbackReviewForm.target_knowledge_type">
              <option value="">不转知识库</option>
              <option value="document_faq">文档 FAQ</option>
              <option value="page">页面说明</option>
              <option value="capability">能力清单</option>
              <option value="api">接口说明</option>
              <option value="db_table">数据表说明</option>
              <option value="requirement_case">历史需求案例</option>
            </select>
          </label>
          <label class="wide">
            <span>复核备注</span>
            <textarea v-model="feedbackReviewForm.review_remark" rows="4"></textarea>
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeFeedbackReview">取消</button>
            <button type="submit" class="primary-action">保存复核</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="referenceModalOpen" class="modal-backdrop" @click.self="closeReferenceModal">
      <section class="modal-panel chunk-panel">
        <header>
          <div>
            <p class="panel-label">引用原文</p>
            <h2>{{ referenceTitle(activeReference) }}</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeReferenceModal">关闭</button>
        </header>
        <div class="reference-detail">
          <dl>
            <div><dt>来源类型</dt><dd>{{ activeReference && 'source_type' in activeReference ? activeReference.source_type : '-' }}</dd></div>
            <div><dt>模块</dt><dd>{{ activeReference && 'module_name' in activeReference ? activeReference.module_name || '-' : '-' }}</dd></div>
            <div><dt>分数</dt><dd>{{ activeReference && 'score' in activeReference ? activeReference.score : '-' }}</dd></div>
            <div><dt>分数类型</dt><dd>{{ activeReference && 'score_type' in activeReference ? activeReference.score_type || '-' : '-' }}</dd></div>
            <div><dt>索引状态</dt><dd>{{ activeReference && 'index_status' in activeReference ? activeReference.index_status || '-' : '-' }}</dd></div>
          </dl>
          <p>{{ referenceContent(activeReference) }}</p>
        </div>
      </section>
    </div>

    <div v-if="evalModalOpen" class="modal-backdrop" @click.self="closeEvalModal">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">{{ editingEvalCase ? '编辑评测' : '新增评测' }}</p>
            <h2>评测用例</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeEvalModal">关闭</button>
        </header>
        <form class="form-grid" @submit.prevent="saveEvalCase">
          <label>
            <span>项目</span>
            <select v-model="evalForm.project_id" required>
              <option value="">请选择项目</option>
              <option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.name }}</option>
            </select>
          </label>
          <label>
            <span>模式</span>
            <select v-model="evalForm.expected_mode">
              <option value="business_qa">业务问答</option>
              <option value="api">接口问答</option>
              <option value="doc_qa">文档问答</option>
              <option value="page_help">页面操作</option>
              <option value="requirement_check">需求分析</option>
            </select>
          </label>
          <label class="wide">
            <span>问题</span>
            <textarea v-model="evalForm.question" required rows="3"></textarea>
          </label>
          <label class="wide">
            <span>期望回答</span>
            <textarea v-model="evalForm.expected_answer" rows="3"></textarea>
          </label>
          <label class="wide">
            <span>期望来源 JSON</span>
            <textarea v-model="evalForm.expected_sources" rows="3"></textarea>
          </label>
          <label class="wide">
            <span>期望关键词（逗号或换行分隔）</span>
            <textarea v-model="evalForm.expected_keywords" rows="3"></textarea>
          </label>
          <label class="wide">
            <span>期望来源标题</span>
            <textarea v-model="evalForm.expected_source_titles" rows="3"></textarea>
          </label>
          <label class="wide">
            <span>期望来源类型</span>
            <textarea v-model="evalForm.expected_source_types" rows="2" placeholder="API&#10;PAGE&#10;CAPABILITY"></textarea>
          </label>
          <label>
            <span>期望可行性等级</span>
            <select v-model="evalForm.expected_feasibility_level">
              <option value="">无</option>
              <option value="A">A</option>
              <option value="B">B</option>
              <option value="C">C</option>
              <option value="D">D</option>
              <option value="E">E</option>
            </select>
          </label>
          <label>
            <span>期望拒答</span>
            <input v-model="evalForm.expected_refusal" type="checkbox" />
          </label>
          <label>
            <span>回答类型</span>
            <input v-model="evalForm.expected_answer_type" placeholder="factual / feasibility / refusal" />
          </label>
          <label>
            <span>难度</span>
            <select v-model="evalForm.difficulty">
              <option value="">未设置</option>
              <option value="easy">easy</option>
              <option value="medium">medium</option>
              <option value="hard">hard</option>
            </select>
          </label>
          <label class="wide">
            <span>标签</span>
            <textarea v-model="evalForm.tags" rows="2" placeholder="api&#10;alarm"></textarea>
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model="evalForm.remark" rows="2"></textarea>
          </label>
          <label>
            <span>启用</span>
            <input v-model="evalForm.enabled" type="checkbox" />
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeEvalModal">取消</button>
            <button type="submit" class="primary-action">保存</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="reviewModalOpen" class="modal-backdrop" @click.self="closeReviewModal">
      <section class="modal-panel">
        <header>
          <div>
            <p class="panel-label">人工复核</p>
            <h2>评测结果</h2>
          </div>
          <button type="button" class="ghost-action" @click="closeReviewModal">关闭</button>
        </header>
        <div class="review-answer">
          <p>{{ activeEvalResult?.actual_answer }}</p>
          <div class="metric-grid compact">
            <article>
              <span>自动分</span>
              <strong>{{ activeEvalResult?.auto_score ?? activeEvalResult?.score ?? '-' }}</strong>
            </article>
            <article>
              <span>关键词</span>
              <strong>{{ activeEvalResult?.keyword_score ?? '-' }}</strong>
            </article>
            <article>
              <span>来源</span>
              <strong>{{ activeEvalResult?.source_score ?? '-' }}</strong>
            </article>
            <article>
              <span>拒答</span>
              <strong>{{ activeEvalResult?.refusal_score ?? '-' }}</strong>
            </article>
          </div>
          <small>命中关键词：{{ textListValue(activeEvalResult?.matched_keywords) || '-' }}</small>
          <small>缺失来源：{{ textListValue(activeEvalResult?.missing_sources) || '-' }}</small>
        </div>
        <form class="form-grid" @submit.prevent="saveReview">
          <label>
            <span>是否通过</span>
            <select v-model="reviewForm.passed">
              <option :value="true">通过</option>
              <option :value="false">不通过</option>
            </select>
          </label>
          <label>
            <span>评分</span>
            <input v-model.number="reviewForm.score" min="0" max="100" type="number" />
          </label>
          <label class="wide">
            <span>备注</span>
            <textarea v-model="reviewForm.remark" rows="4"></textarea>
          </label>
          <footer class="form-actions">
            <button type="button" class="ghost-action" @click="closeReviewModal">取消</button>
            <button type="submit" class="primary-action">保存复核</button>
          </footer>
        </form>
      </section>
    </div>
  </main>
</template>
