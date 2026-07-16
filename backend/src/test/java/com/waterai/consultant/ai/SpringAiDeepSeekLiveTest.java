package com.waterai.consultant.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import com.waterai.consultant.model.RequirementModelOutput;
import org.springframework.ai.converter.BeanOutputConverter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiDeepSeekLiveTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+" )
    void shouldCallDeepSeekThroughSpringAi() {
        String baseUrl = environment("DEEPSEEK_BASE_URL", "https://api.deepseek.com");
        String model = environment("DEEPSEEK_MODEL", "deepseek-chat");
        ChatClient client = new SpringAiModelFactory().createChatClient(
                "deepseek",
                baseUrl,
                System.getenv("DEEPSEEK_API_KEY"),
                model,
                64,
                BigDecimal.ZERO,
                false
        );

        String answer = client.prompt()
                .system("你是连接测试助手。")
                .user("只回复：连接成功")
                .call()
                .content();

        assertThat(answer).isNotBlank();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldParseStructuredRequirementResultFromDeepSeek() {
        ChatClient client = new SpringAiModelFactory().createChatClient(
                "deepseek",
                environment("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
                System.getenv("DEEPSEEK_API_KEY"),
                environment("DEEPSEEK_MODEL", "deepseek-chat"),
                256,
                BigDecimal.ZERO,
                true
        );
        BeanOutputConverter<RequirementModelOutput> converter = new BeanOutputConverter<>(RequirementModelOutput.class);

        RequirementModelOutput result = client.prompt()
                .system("你是需求分析测试助手，只输出 JSON。")
                .user("将 feasibility_level 设为 D，conclusion 设为资料不足，其他字段按 schema 填写。\n" + converter.getFormat())
                .call()
                .entity(converter);

        assertThat(result.feasibilityLevel()).isEqualTo("D");
        assertThat(result.conclusion()).isNotBlank();
    }

    private String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
