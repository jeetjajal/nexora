package com.nexora.phase27;

import com.nexora.loadtesting.dto.BeforeAfterOptimizationReport;
import com.nexora.loadtesting.dto.LoadTestResult;
import com.nexora.loadtesting.dto.LoadTestScenario;
import com.nexora.loadtesting.service.LoadTestingService;
import com.nexora.loadtesting.service.LoadTestingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadTestingServiceTest {

    private LoadTestingService loadTestingService;

    @BeforeEach
    void setUp() {
        loadTestingService = new LoadTestingServiceImpl();
    }

    @Test
    @DisplayName("Should retrieve all 7 required core user flow scenarios")
    void testGetAllScenarios() {
        List<LoadTestScenario> scenarios = loadTestingService.getAllScenarios();
        assertThat(scenarios).hasSize(7);
        assertThat(scenarios).extracting(LoadTestScenario::getScenarioId)
                .containsExactlyInAnyOrder(
                        "login",
                        "product_search",
                        "product_details",
                        "cart_ops",
                        "order_creation",
                        "payment_verification",
                        "order_tracking"
                );
    }

    @Test
    @DisplayName("Should execute simulated benchmark and calculate latency distributions")
    void testRunScenarioLoadTest() {
        LoadTestResult result = loadTestingService.runScenarioLoadTest("order_creation", 100, 10);

        assertThat(result.getScenarioId()).isEqualTo("order_creation");
        assertThat(result.getVirtualUsers()).isEqualTo(100);
        assertThat(result.getDurationSeconds()).isEqualTo(10);
        assertThat(result.getTotalRequests()).isGreaterThan(0);
        assertThat(result.getRequestsPerSecond()).isGreaterThan(500.0);
        assertThat(result.getP50LatencyMs()).isLessThan(result.getP95LatencyMs());
        assertThat(result.getP95LatencyMs()).isLessThan(result.getP99LatencyMs());
        assertThat(result.getBottlenecksIdentified()).isNotEmpty();
        assertThat(result.getOptimizationsApplied()).isNotEmpty();
    }

    @Test
    @DisplayName("Should provide Before vs After optimization reports with architectural takeaways")
    void testGetOptimizationReports() {
        List<BeforeAfterOptimizationReport> reports = loadTestingService.getOptimizationReports();
        assertThat(reports).hasSize(3);

        for (BeforeAfterOptimizationReport report : reports) {
            assertThat(report.getAfterThroughputRps()).isGreaterThan(report.getBeforeThroughputRps());
            assertThat(report.getAfterP95LatencyMs()).isLessThan(report.getBeforeP95LatencyMs());
            assertThat(report.getAfterErrorRatePercent()).isLessThanOrEqualTo(report.getBeforeErrorRatePercent());
            assertThat(report.getArchitecturalTakeaways()).isNotEmpty();
        }
    }
}
