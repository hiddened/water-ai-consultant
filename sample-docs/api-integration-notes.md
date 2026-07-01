# 接口接入说明

## 创建巡检任务接口

创建巡检任务接口为 POST /api/inspection/tasks。请求字段包括 plant_id、task_name、assignee_id、due_time 和 task_type。

接口返回任务 id、任务状态 task_status 和创建时间 created_at。调用方需要具备内部系统登录态，并确保处理人 assignee_id 在当前项目下有效。

## 数据校验

plant_id 必须关联有效水厂基础数据。task_status 由系统状态字典维护，不建议外部系统直接传入任意状态值。
