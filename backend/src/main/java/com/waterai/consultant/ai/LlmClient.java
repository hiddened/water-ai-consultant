package com.waterai.consultant.ai;

import com.waterai.consultant.retrieval.KnowledgeEvidence;

import java.util.List;

public interface LlmClient {

    String answer(String prompt, String question, String mode, List<KnowledgeEvidence> evidences);

    String analyzeRequirement(String prompt, String requirementDesc, String moduleName, List<KnowledgeEvidence> evidences);
}

