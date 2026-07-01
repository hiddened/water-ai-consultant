package com.waterai.consultant.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EvalScoringService {

    private static final String INSUFFICIENT_TEXT = "当前资料不足，无法确认";

    private final ObjectMapper objectMapper;

    public EvalScoringService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvalScoreResult score(Map<String, Object> evalCase,
                                 String actualAnswer,
                                 Object references,
                                 String actualFeasibilityLevel) {
        List<String> expectedKeywords = readStringList(evalCase.get("expected_keywords"));
        List<String> expectedSourceTitles = readStringList(evalCase.get("expected_source_titles"));
        List<String> expectedSourceTypes = readStringList(evalCase.get("expected_source_types"));
        List<String> acceptedLevels = expectedFeasibilityLevels(evalCase.get("expected_feasibility_level"));
        boolean expectedRefusal = Boolean.TRUE.equals(evalCase.get("expected_refusal"));
        List<Map<String, Object>> actualReferences = readReferenceList(references);

        KeywordScore keyword = keywordScore(expectedKeywords, actualAnswer);
        SourceScore source = sourceScore(expectedRefusal, expectedSourceTitles, expectedSourceTypes, actualReferences);
        BigDecimal refusalScore = refusalScore(expectedRefusal, actualAnswer);
        BigDecimal feasibilityScore = feasibilityScore(acceptedLevels, actualFeasibilityLevel);

        boolean requirementMode = "requirement_check".equals(String.valueOf(evalCase.get("expected_mode")));
        BigDecimal autoScore = totalScore(requirementMode, keyword.score(), source.score(), refusalScore, feasibilityScore);

        // 自动通过必须同时满足总分、拒答预期和来源约束，避免“答得像但没依据”被误判通过。
        boolean autoPassed = autoScore.compareTo(BigDecimal.valueOf(70)) >= 0;
        if (expectedRefusal && refusalScore.compareTo(BigDecimal.valueOf(100)) < 0) {
            autoPassed = false;
        }
        if (!expectedSourceTitles.isEmpty() && source.score().compareTo(BigDecimal.ZERO) == 0) {
            autoPassed = false;
        }

        Map<String, Object> detail = Map.of(
                "keyword_rule", "expected_keywords 命中比例",
                "source_rule", "expected_source_titles/source_types 命中比例；无期望但有引用给基础分",
                "refusal_rule", "expected_refusal=true 时必须包含固定资料不足拒答句",
                "feasibility_rule", "requirement_check 按期望等级匹配，支持多个等级",
                "weights", requirementMode ? Map.of("keyword", 30, "source", 30, "refusal", 20, "feasibility", 20)
                        : Map.of("keyword", 40, "source", 40, "refusal", 20, "feasibility", 0)
        );

        return new EvalScoreResult(
                autoScore,
                keyword.score(),
                source.score(),
                refusalScore,
                feasibilityScore,
                actualReferences.size(),
                keyword.matched(),
                source.matched(),
                keyword.missing(),
                source.missing(),
                autoPassed,
                detail
        );
    }

    private KeywordScore keywordScore(List<String> expectedKeywords, String answer) {
        if (expectedKeywords.isEmpty()) {
            return new KeywordScore(BigDecimal.valueOf(100), List.of(), List.of());
        }
        String normalizedAnswer = normalize(answer);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String keyword : expectedKeywords) {
            if (normalizedAnswer.contains(normalize(keyword))) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }
        return new KeywordScore(ratio(matched.size(), expectedKeywords.size()), matched, missing);
    }

    private SourceScore sourceScore(boolean expectedRefusal,
                                    List<String> expectedTitles,
                                    List<String> expectedTypes,
                                    List<Map<String, Object>> references) {
        if (expectedTitles.isEmpty() && expectedTypes.isEmpty()) {
            if (expectedRefusal) {
                return new SourceScore(references.isEmpty() ? BigDecimal.valueOf(100) : BigDecimal.ZERO, List.of(), references.isEmpty() ? List.of() : List.of("拒答用例不应返回引用"));
            }
            return new SourceScore(references.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(70), List.of(), List.of());
        }
        Set<String> actualTitles = new LinkedHashSet<>();
        Set<String> actualTypes = new LinkedHashSet<>();
        for (Map<String, Object> reference : references) {
            addIfPresent(actualTitles, reference, "title");
            addIfPresent(actualTitles, reference, "source_title");
            addIfPresent(actualTitles, reference, "document_title");
            addIfPresent(actualTypes, reference, "source_type");
            addIfPresent(actualTypes, reference, "sourceType");
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String title : expectedTitles) {
            if (containsFuzzy(actualTitles, title)) {
                matched.add(title);
            } else {
                missing.add(title);
            }
        }
        for (String type : expectedTypes) {
            if (containsExact(actualTypes, type)) {
                matched.add(type);
            } else {
                missing.add(type);
            }
        }
        int expectedTotal = expectedTitles.size() + expectedTypes.size();
        return new SourceScore(ratio(matched.size(), expectedTotal), matched, missing);
    }

    private BigDecimal refusalScore(boolean expectedRefusal, String answer) {
        boolean actuallyRefused = answer != null && answer.contains(INSUFFICIENT_TEXT);
        if (expectedRefusal) {
            return actuallyRefused ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return actuallyRefused ? BigDecimal.ZERO : BigDecimal.valueOf(100);
    }

    private BigDecimal feasibilityScore(List<String> expectedLevels, String actualLevel) {
        if (expectedLevels.isEmpty()) {
            return BigDecimal.valueOf(100);
        }
        if (actualLevel == null || actualLevel.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalizedActual = actualLevel.trim().toUpperCase(Locale.ROOT);
        boolean matched = expectedLevels.stream()
                .map(level -> level.trim().toUpperCase(Locale.ROOT))
                .anyMatch(level -> level.equals(normalizedActual));
        return matched ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
    }

    private BigDecimal totalScore(boolean requirementMode,
                                  BigDecimal keywordScore,
                                  BigDecimal sourceScore,
                                  BigDecimal refusalScore,
                                  BigDecimal feasibilityScore) {
        if (requirementMode) {
            return weighted(keywordScore, 30)
                    .add(weighted(sourceScore, 30))
                    .add(weighted(refusalScore, 20))
                    .add(weighted(feasibilityScore, 20))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // 非需求分析没有可行性等级，将 20% 权重平均回关键词和来源。
        return weighted(keywordScore, 40)
                .add(weighted(sourceScore, 40))
                .add(weighted(refusalScore, 20))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal weighted(BigDecimal score, int weight) {
        return score.multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(int matched, int total) {
        if (total <= 0) {
            return BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf(matched)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private List<String> expectedFeasibilityLevels(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String text && text.trim().startsWith("[")) {
            return readStringList(text);
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? List.of() : List.of(text);
    }

    private List<String> readStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        try {
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).filter(item -> !item.isBlank()).toList();
            }
            String text = String.valueOf(value);
            if (text.isBlank()) {
                return List.of();
            }
            if (text.trim().startsWith("[")) {
                return objectMapper.readValue(text, new TypeReference<List<String>>() {
                });
            }
            return List.of(text);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<Map<String, Object>> readReferenceList(Object references) {
        if (references == null) {
            return List.of();
        }
        try {
            if (references instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<List<Map<String, Object>>>() {
                });
            }
            return objectMapper.readValue(String.valueOf(references), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void addIfPresent(Set<String> values, Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null && !String.valueOf(value).isBlank()) {
            values.add(String.valueOf(value));
        }
    }

    private boolean containsFuzzy(Set<String> actualValues, String expected) {
        String normalizedExpected = normalize(expected);
        return actualValues.stream().map(this::normalize).anyMatch(value -> value.contains(normalizedExpected) || normalizedExpected.contains(value));
    }

    private boolean containsExact(Set<String> actualValues, String expected) {
        String normalizedExpected = normalize(expected);
        return actualValues.stream().map(this::normalize).anyMatch(normalizedExpected::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private record KeywordScore(BigDecimal score, List<String> matched, List<String> missing) {
    }

    private record SourceScore(BigDecimal score, List<String> matched, List<String> missing) {
    }
}
