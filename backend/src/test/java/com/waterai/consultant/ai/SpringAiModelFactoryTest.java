package com.waterai.consultant.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.MapOutputConverter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiModelFactoryTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldInvokeOpenAiCompatibleEndpointThroughSpringAi() throws IOException {
        startServer("Spring AI 调用成功");

        SpringAiModelFactory factory = new SpringAiModelFactory();
        ChatClient client = factory.createChatClient(
                "openai_compatible",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-key",
                "test-model",
                256,
                null,
                false
        );

        String answer = client.prompt()
                .system("只回答测试结果")
                .user("测试 Spring AI 调用")
                .call()
                .content();

        assertThat(answer).isEqualTo("Spring AI 调用成功");
    }

    @Test
    void shouldConvertStructuredRequirementResultThroughSpringAi() throws IOException {
        startServer("{\"feasibility_level\":\"B\",\"conclusion\":\"配置后可支持\"}");
        SpringAiModelFactory factory = new SpringAiModelFactory();
        ChatClient client = factory.createChatClient(
                "openai_compatible",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-key",
                "test-model",
                256,
                null,
                true
        );
        MapOutputConverter converter = new MapOutputConverter();

        Map<String, Object> result = client.prompt()
                .system("输出需求可行性 JSON")
                .user("分析需求\n" + converter.getFormat())
                .call()
                .entity(converter);

        assertThat(result)
                .containsEntry("feasibility_level", "B")
                .containsEntry("conclusion", "配置后可支持");
    }

    private void startServer(String content) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"");
            String responseBody = """
                    {
                      "id":"chatcmpl-test",
                      "object":"chat.completion",
                      "created":1720000000,
                      "model":"test-model",
                      "choices":[{
                        "index":0,
                        "message":{"role":"assistant","content":"%s"},
                        "finish_reason":"stop"
                      }],
                      "usage":{"prompt_tokens":3,"completion_tokens":3,"total_tokens":6}
                    }
                    """.formatted(escapedContent);
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }
}
