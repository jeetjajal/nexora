package com.nexora.systemdesign.service;

import com.nexora.systemdesign.dto.FailureScenarioMatrix;
import com.nexora.systemdesign.dto.InterviewQuestionAnswer;
import com.nexora.systemdesign.dto.ScaleToMillionBlueprint;
import com.nexora.systemdesign.dto.SystemArchitectureDiagram;

import java.util.List;

public interface SystemDesignService {
    List<InterviewQuestionAnswer> getInterviewChecklist();
    SystemArchitectureDiagram getArchitectureDiagram();
    ScaleToMillionBlueprint getScaleToMillionBlueprint();
    List<FailureScenarioMatrix> getFailureScenarios();
}
