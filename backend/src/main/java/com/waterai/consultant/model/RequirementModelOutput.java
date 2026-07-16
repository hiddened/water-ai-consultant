package com.waterai.consultant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RequirementModelOutput(
        @JsonProperty("requirement_understanding") String requirementUnderstanding,
        @JsonProperty("feasibility_level") String feasibilityLevel,
        String conclusion,
        @JsonProperty("matched_capabilities") List<String> matchedCapabilities,
        @JsonProperty("missing_capabilities") List<String> missingCapabilities,
        @JsonProperty("related_pages") List<String> relatedPages,
        @JsonProperty("related_apis") List<String> relatedApis,
        @JsonProperty("related_tables") List<String> relatedTables,
        @JsonProperty("impact_modules") List<String> impactModules,
        @JsonProperty("risk_points") List<String> riskPoints,
        @JsonProperty("recommended_solution") String recommendedSolution,
        @JsonProperty("workload_level") String workloadLevel
) {
}
