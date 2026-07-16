package com.waterai.consultant.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sun.net.httpserver.HttpServer;
import com.waterai.consultant.ai.SpringAiModelFactory;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ModelRuntimeServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRunStructuredRequirementThroughEnvironmentSpringAiConfig() throws Exception {
        startServer("""
                {"requirement_understanding":"接入告警并建单","feasibility_level":"B","conclusion":"配置后可支持",\
                "matched_capabilities":["告警自动建单"],"missing_capabilities":[],"related_pages":[],\
                "related_apis":["告警推送"],"related_tables":[],"impact_modules":["告警中心"],\
                "risk_points":["需要接口联调"],"recommended_solution":"先配置再联调","workload_level":"中"}
                """);
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        doReturn(List.of()).when(jdbcTemplate).query(
                anyString(), any(MapSqlParameterSource.class), any(ColumnMapRowMapper.class));
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        ModelRuntimeService service = new ModelRuntimeService(
                jdbcTemplate,
                objectMapper,
                new SpringAiModelFactory(),
                "openai_compatible",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-key",
                "test-model",
                512,
                "none",
                "",
                "",
                "",
                1536
        );
        KnowledgeEvidence evidence = new KnowledgeEvidence(
                "CAPABILITY", UUID.randomUUID(), "告警自动建单", "configurable",
                "告警中心", "支持命中规则后自动建单", BigDecimal.TEN);

        ModelInvocationResult result = service.analyzeRequirement(
                "只基于证据输出结构化结果", "分析告警自动建单需求", "接入告警并建单", "告警中心", List.of(evidence));
        JsonNode json = objectMapper.readTree(result.content());

        assertThat(result.provider()).isEqualTo("openai_compatible");
        assertThat(json.path("feasibility_level").asText()).isEqualTo("B");
        assertThat(json.path("conclusion").asText()).isEqualTo("配置后可支持");
    }

    private void startServer(String content) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "");
            String body = """
                    {"id":"chatcmpl-test","object":"chat.completion","created":1720000000,"model":"test-model",
                    "choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}],
                    "usage":{"prompt_tokens":5,"completion_tokens":5,"total_tokens":10}}
                    """.formatted(escaped);
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }
}
