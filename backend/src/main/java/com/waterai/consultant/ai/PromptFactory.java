package com.waterai.consultant.ai;

import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PromptFactory {

    public String buildChatPrompt(String mode, String question, List<KnowledgeEvidence> evidences) {
        return """
                你是 AI 水务项目智能顾问，服务对象是售前、产品、实施、开发、运维人员。
                回答规则：
                1. 只能基于 evidence 中的项目资料回答。
                2. 如果 evidence 为空，必须回答“当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。”。
                3. 不要编造项目功能、接口、字段或实施经验。
                4. 回答要简洁，并指出关键依据。

                mode: %s
                question: %s

                evidence:
                %s
                """.formatted(mode, question, formatEvidence(evidences));
    }

    public String buildRequirementPrompt(String requirementDesc, String moduleName, List<KnowledgeEvidence> evidences) {
        return """
                你是 AI 水务项目智能顾问，请基于 evidence 做需求可行性分析。
                输出必须是 JSON，字段包括：
                requirement_understanding, feasibility_level, conclusion, missing_capabilities,
                impact_modules, risk_points, recommended_solution, workload_level。
                可行性等级只能是 A/B/C/D/E：
                A 现有功能已支持；B 配置即可支持；C 需要二次开发；D 资料不足无法判断；E 不建议实现。
                严格规则：
                1. 只能基于 references 和 evidence 回答，不允许纯靠模型猜测。
                2. 如果 evidence 为空，feasibility_level 必须是 D，conclusion 必须是“当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。”。
                3. 如果只命中文档切片但没有能力清单依据，feasibility_level 只能是 C 或 D，不能输出 A 或 B。
                4. 只有命中能力清单和类似历史需求时，才允许判断为 A、B 或 C。

                requirement_desc: %s
                module_name: %s

                evidence:
                %s
                """.formatted(requirementDesc, moduleName == null ? "" : moduleName, formatEvidence(evidences));
    }

    private String formatEvidence(List<KnowledgeEvidence> evidences) {
        AtomicInteger index = new AtomicInteger(1);
        StringBuilder builder = new StringBuilder();
        for (KnowledgeEvidence evidence : evidences) {
            builder.append("[R").append(index.getAndIncrement()).append("] ")
                    .append(evidence.sourceType()).append(" / ")
                    .append(evidence.sourceTitle()).append(" / ")
                    .append(evidence.sourceLocator()).append("\n")
                    .append(evidence.content()).append("\n\n");
        }
        return builder.isEmpty() ? "(empty)" : builder.toString();
    }
}
