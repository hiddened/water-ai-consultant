package com.waterai.consultant.structured;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "结构化资料管理")
@RestController
@RequestMapping("/api")
public class StructuredDataController {

    private final StructuredDataService service;
    private final TraceIdProvider traceIdProvider;

    public StructuredDataController(StructuredDataService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "项目列表")
    @GetMapping("/projects")
    public ApiResponse<List<Map<String, Object>>> listProjects(@RequestParam(name = "project_id", required = false) String projectId,
                                                               @RequestParam(name = "module_name", required = false) String moduleName,
                                                               @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("projects", projectId, moduleName, keyword));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> getProject(@PathVariable UUID id) {
        return ok(service.get("projects", id));
    }

    @Operation(summary = "新建项目")
    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> createProject(@RequestBody Map<String, Object> request) {
        return ok(service.create("projects", request));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> updateProject(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("projects", id, request));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable UUID id) {
        service.delete("projects", id);
        return ok(null);
    }

    @GetMapping("/documents")
    public ApiResponse<List<Map<String, Object>>> listDocuments(@RequestParam(name = "project_id", required = false) String projectId,
                                                                @RequestParam(name = "module_name", required = false) String moduleName,
                                                                @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("documents", projectId, moduleName, keyword));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<Map<String, Object>> getDocument(@PathVariable UUID id) {
        return ok(service.get("documents", id));
    }

    @PostMapping("/documents")
    public ApiResponse<Map<String, Object>> createDocument(@RequestBody Map<String, Object> request) {
        return ok(service.create("documents", request));
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<Map<String, Object>> updateDocument(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("documents", id, request));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id) {
        service.delete("documents", id);
        return ok(null);
    }

    @GetMapping("/pages")
    public ApiResponse<List<Map<String, Object>>> listPages(@RequestParam(name = "project_id", required = false) String projectId,
                                                            @RequestParam(name = "module_name", required = false) String moduleName,
                                                            @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("pages", projectId, moduleName, keyword));
    }

    @GetMapping("/pages/{id}")
    public ApiResponse<Map<String, Object>> getPage(@PathVariable UUID id) {
        return ok(service.get("pages", id));
    }

    @PostMapping("/pages")
    public ApiResponse<Map<String, Object>> createPage(@RequestBody Map<String, Object> request) {
        return ok(service.create("pages", request));
    }

    @PutMapping("/pages/{id}")
    public ApiResponse<Map<String, Object>> updatePage(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("pages", id, request));
    }

    @DeleteMapping("/pages/{id}")
    public ApiResponse<Void> deletePage(@PathVariable UUID id) {
        service.delete("pages", id);
        return ok(null);
    }

    @GetMapping("/capabilities")
    public ApiResponse<List<Map<String, Object>>> listCapabilities(@RequestParam(name = "project_id", required = false) String projectId,
                                                                   @RequestParam(name = "module_name", required = false) String moduleName,
                                                                   @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("capabilities", projectId, moduleName, keyword));
    }

    @GetMapping("/capabilities/{id}")
    public ApiResponse<Map<String, Object>> getCapability(@PathVariable UUID id) {
        return ok(service.get("capabilities", id));
    }

    @PostMapping("/capabilities")
    public ApiResponse<Map<String, Object>> createCapability(@RequestBody Map<String, Object> request) {
        return ok(service.create("capabilities", request));
    }

    @PutMapping("/capabilities/{id}")
    public ApiResponse<Map<String, Object>> updateCapability(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("capabilities", id, request));
    }

    @DeleteMapping("/capabilities/{id}")
    public ApiResponse<Void> deleteCapability(@PathVariable UUID id) {
        service.delete("capabilities", id);
        return ok(null);
    }

    @GetMapping("/apis")
    public ApiResponse<List<Map<String, Object>>> listApis(@RequestParam(name = "project_id", required = false) String projectId,
                                                           @RequestParam(name = "module_name", required = false) String moduleName,
                                                           @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("apis", projectId, moduleName, keyword));
    }

    @GetMapping("/apis/{id}")
    public ApiResponse<Map<String, Object>> getApi(@PathVariable UUID id) {
        return ok(service.get("apis", id));
    }

    @PostMapping("/apis")
    public ApiResponse<Map<String, Object>> createApi(@RequestBody Map<String, Object> request) {
        return ok(service.create("apis", request));
    }

    @PutMapping("/apis/{id}")
    public ApiResponse<Map<String, Object>> updateApi(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("apis", id, request));
    }

    @DeleteMapping("/apis/{id}")
    public ApiResponse<Void> deleteApi(@PathVariable UUID id) {
        service.delete("apis", id);
        return ok(null);
    }

    @GetMapping("/db-tables")
    public ApiResponse<List<Map<String, Object>>> listDbTables(@RequestParam(name = "project_id", required = false) String projectId,
                                                               @RequestParam(name = "module_name", required = false) String moduleName,
                                                               @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("db-tables", projectId, moduleName, keyword));
    }

    @GetMapping("/db-tables/{id}")
    public ApiResponse<Map<String, Object>> getDbTable(@PathVariable UUID id) {
        return ok(service.get("db-tables", id));
    }

    @PostMapping("/db-tables")
    public ApiResponse<Map<String, Object>> createDbTable(@RequestBody Map<String, Object> request) {
        return ok(service.create("db-tables", request));
    }

    @PutMapping("/db-tables/{id}")
    public ApiResponse<Map<String, Object>> updateDbTable(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("db-tables", id, request));
    }

    @DeleteMapping("/db-tables/{id}")
    public ApiResponse<Void> deleteDbTable(@PathVariable UUID id) {
        service.delete("db-tables", id);
        return ok(null);
    }

    @GetMapping("/requirement-cases")
    public ApiResponse<List<Map<String, Object>>> listRequirementCases(@RequestParam(name = "project_id", required = false) String projectId,
                                                                       @RequestParam(name = "module_name", required = false) String moduleName,
                                                                       @RequestParam(name = "keyword", required = false) String keyword) {
        return ok(service.list("requirement-cases", projectId, moduleName, keyword));
    }

    @GetMapping("/requirement-cases/{id}")
    public ApiResponse<Map<String, Object>> getRequirementCase(@PathVariable UUID id) {
        return ok(service.get("requirement-cases", id));
    }

    @PostMapping("/requirement-cases")
    public ApiResponse<Map<String, Object>> createRequirementCase(@RequestBody Map<String, Object> request) {
        return ok(service.create("requirement-cases", request));
    }

    @PutMapping("/requirement-cases/{id}")
    public ApiResponse<Map<String, Object>> updateRequirementCase(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        return ok(service.update("requirement-cases", id, request));
    }

    @DeleteMapping("/requirement-cases/{id}")
    public ApiResponse<Void> deleteRequirementCase(@PathVariable UUID id) {
        service.delete("requirement-cases", id);
        return ok(null);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }

}
