package com.nexora.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Educational breakdown of core containerization fundamentals for SDE interviews.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerConceptExplanation {
    private String concept;
    private String definition;
    private String whyItMatters;
    private String bestPractice;
}
