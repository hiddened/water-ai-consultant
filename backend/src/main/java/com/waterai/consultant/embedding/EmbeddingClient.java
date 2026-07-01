package com.waterai.consultant.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embedText(String text);

    List<List<Double>> embedBatch(List<String> texts);

    String provider();

    int dimension();

    boolean realEmbedding();
}
