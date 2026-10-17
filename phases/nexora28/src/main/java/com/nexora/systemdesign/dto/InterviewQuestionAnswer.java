package com.nexora.systemdesign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionAnswer {
    private String questionId;
    private String question;
    private String category;
    private String shortAnswer;
    private String inDepthExplanation;
    private String nexoraImplementationProof;
    private List<String> interviewKeywords;
}
