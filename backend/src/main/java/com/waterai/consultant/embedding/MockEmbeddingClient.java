package com.waterai.consultant.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingClient implements EmbeddingClient {

    private final int dimension;

    public MockEmbeddingClient(@Value("${app.embedding.dimension}") int dimension) {
        this.dimension = dimension;
    }

    @Override
    public List<Double> embedText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] seed = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            List<Double> vector = new ArrayList<>(dimension);
            for (int i = 0; i < dimension; i++) {
                int value = seed[i % seed.length] & 0xff;
                vector.add((value / 127.5d) - 1.0d);
            }
            return vector;
        } catch (Exception ex) {
            throw new IllegalStateException("Mock embedding 生成失败", ex);
        }
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        return texts.stream().map(this::embedText).toList();
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public boolean realEmbedding() {
        return false;
    }
}
