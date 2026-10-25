package com.nexora.phase28;

import com.nexora.systemdesign.dto.FailureScenarioMatrix;
import com.nexora.systemdesign.dto.InterviewQuestionAnswer;
import com.nexora.systemdesign.dto.ScaleToMillionBlueprint;
import com.nexora.systemdesign.dto.SystemArchitectureDiagram;
import com.nexora.systemdesign.service.SystemDesignService;
import com.nexora.systemdesign.service.SystemDesignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDesignServiceTest {

    private SystemDesignService systemDesignService;

    @BeforeEach
    void setUp() {
        systemDesignService = new SystemDesignServiceImpl();
    }

    @Test
    @DisplayName("Should contain all 16 questions from Final SDE Interview Checklist")
    void testInterviewChecklistCompleteness() {
        List<InterviewQuestionAnswer> checklist = systemDesignService.getInterviewChecklist();
        assertThat(checklist).hasSize(16);

        // Verify key questions are answered
        assertThat(checklist).extracting(InterviewQuestionAnswer::getQuestionId)
                .contains(
                        "q1_spring_boot_mvc",
                        "q2_mysql_isolation",
                        "q3_jwt_auth",
                        "q4_tamper_proof_totals",
                        "q5_payment_verification",
                        "q6_duplicate_payments",
                        "q7_inventory_concurrency",
                        "q8_redis_caching",
                        "q9_kafka_events",
                        "q10_rest_vs_websocket",
                        "q11_search_engine",
                        "q12_ai_tool_calling",
                        "q13_monolith_vs_microservices",
                        "q14_docker_aws_k8s",
                        "q15_metrics_latency",
                        "q16_scale_to_one_million"
                );

        for (InterviewQuestionAnswer qa : checklist) {
            assertThat(qa.getShortAnswer()).isNotEmpty();
            assertThat(qa.getInDepthExplanation()).isNotEmpty();
            assertThat(qa.getNexoraImplementationProof()).isNotEmpty();
            assertThat(qa.getInterviewKeywords()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should provide automated mitigation and consistency guarantees for all failure modes")
    void testFailureScenariosMatrix() {
        List<FailureScenarioMatrix> matrix = systemDesignService.getFailureScenarios();
        assertThat(matrix).hasSize(5);

        assertThat(matrix).extracting(FailureScenarioMatrix::getFailureEvent)
                .contains(
                        "MySQL Primary Database Crashes",
                        "Redis Cluster Node Outage",
                        "Apache Kafka Broker Unavailable",
                        "Payment Gateway Succeeds but Order Creation Fails",
                        "Flash Sale Traffic Spike (10,000 Concurrent Users)"
                );

        for (FailureScenarioMatrix scenario : matrix) {
            assertThat(scenario.getAutomatedMitigation()).isNotEmpty();
            assertThat(scenario.getRecoveryProtocol()).isNotEmpty();
            assertThat(scenario.getDataConsistencyGuarantee()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should provide 1 Million Users scaling roadmap and architecture blueprint")
    void testScaleToMillionBlueprint() {
        ScaleToMillionBlueprint scale = systemDesignService.getScaleToMillionBlueprint();
        assertThat(scale.getDailyActiveUsers()).isEqualTo(1000000.0);
        assertThat(scale.getPeakRpsEstimate()).isGreaterThanOrEqualTo(4000.0);
        assertThat(scale.getScalingTiers()).containsKey("Tier 3 (1,000,000 DAU)");
        assertThat(scale.getDatabaseScalingTactics()).isNotEmpty();

        SystemArchitectureDiagram arch = systemDesignService.getArchitectureDiagram();
        assertThat(arch.getGatewayLayers()).isNotEmpty();
        assertThat(arch.getApplicationServices()).isNotEmpty();
        assertThat(arch.getDataFlowSummary()).isNotEmpty();
    }
}
