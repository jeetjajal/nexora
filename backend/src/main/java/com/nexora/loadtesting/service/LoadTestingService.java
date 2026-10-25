package com.nexora.loadtesting.service;

import com.nexora.loadtesting.dto.BeforeAfterOptimizationReport;
import com.nexora.loadtesting.dto.LoadTestResult;
import com.nexora.loadtesting.dto.LoadTestScenario;

import java.util.List;

public interface LoadTestingService {
    List<LoadTestScenario> getAllScenarios();
    LoadTestResult runScenarioLoadTest(String scenarioId, int virtualUsers, int durationSeconds);
    List<BeforeAfterOptimizationReport> getOptimizationReports();
}
