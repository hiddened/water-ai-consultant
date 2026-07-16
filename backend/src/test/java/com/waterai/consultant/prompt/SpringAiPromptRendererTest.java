package com.waterai.consultant.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiPromptRendererTest {

    private final SpringAiPromptRenderer renderer = new SpringAiPromptRenderer();

    @Test
    void shouldRenderVariablesWithoutBreakingJsonBraces() {
        String result = renderer.render(
                "问题：{{question}}\n示例 JSON：{\"level\":\"D\"}",
                Map.of("question", "是否支持无人机巡检？")
        );

        assertThat(result)
                .contains("是否支持无人机巡检？")
                .contains("{\"level\":\"D\"}");
    }
}
