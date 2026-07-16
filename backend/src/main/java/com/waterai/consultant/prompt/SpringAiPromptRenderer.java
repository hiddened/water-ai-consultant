package com.waterai.consultant.prompt;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SpringAiPromptRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    public String render(String template, Map<String, String> variables) {
        String source = template == null ? "" : template;
        String springAiTemplate = VARIABLE_PATTERN.matcher(source)
                .replaceAll(result -> "«" + result.group(1) + "»");
        Map<String, Object> values = new LinkedHashMap<>();
        variables.forEach((key, value) -> values.put(key, value == null || value.isBlank() ? "暂无" : value));
        try {
            // 使用独立分隔符避免 JSON 中的花括号被误识别成 Prompt 变量。
            return PromptTemplate.builder()
                    .template(springAiTemplate)
                    .variables(values)
                    .renderer(StTemplateRenderer.builder()
                            .startDelimiterToken('«')
                            .endDelimiterToken('»')
                            .build())
                    .build()
                    .render();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Prompt 模板渲染失败：" + ex.getMessage());
        }
    }
}
