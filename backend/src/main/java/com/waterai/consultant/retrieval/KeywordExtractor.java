package com.waterai.consultant.retrieval;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordExtractor {

    private static final Pattern LATIN_TOKEN = Pattern.compile("[a-zA-Z0-9_./:-]{2,}");
    private static final Pattern CJK_BLOCK = Pattern.compile("[\\p{IsHan}]{2,}");

    public List<String> extract(String text) {
        Set<String> terms = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        addMatches(terms, LATIN_TOKEN.matcher(normalized));

        Matcher cjkMatcher = CJK_BLOCK.matcher(normalized);
        while (cjkMatcher.find()) {
            String block = cjkMatcher.group();
            terms.add(block);
            // 中文问题通常没有空格，加入 2-4 字片段，提升“新增项目/水表/泵站”等短词命中率。
            for (int size = 2; size <= 4; size++) {
                for (int i = 0; i + size <= block.length(); i++) {
                    terms.add(block.substring(i, i + size));
                }
            }
        }

        return terms.stream()
                .filter(term -> term.length() >= 2)
                .limit(80)
                .toList();
    }

    private void addMatches(Set<String> terms, Matcher matcher) {
        while (matcher.find()) {
            terms.add(matcher.group());
        }
    }
}

