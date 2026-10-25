package com.nexora.phase24;

import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.aws.controller.AwsController;
import com.nexora.aws.dto.AwsArchitectureTopology;
import com.nexora.aws.dto.AwsCostEstimate;
import com.nexora.aws.dto.AwsServiceSpec;
import com.nexora.aws.service.AwsArchitectureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AwsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsArchitectureService awsArchitectureService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/aws/services returns service catalog")
    void testGetServices() throws Exception {
        AwsServiceSpec spec = AwsServiceSpec.builder()
                .serviceName("Amazon RDS for MySQL")
                .awsProduct("Amazon RDS")
                .publiclyAccessible(false)
                .build();

        when(awsArchitectureService.getSelectedServices()).thenReturn(Collections.singletonList(spec));

        mockMvc.perform(get("/api/v1/aws/services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].serviceName").value("Amazon RDS for MySQL"))
                .andExpect(jsonPath("$.data[0].publiclyAccessible").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/aws/topology returns VPC topology")
    void testGetTopology() throws Exception {
        AwsArchitectureTopology topology = AwsArchitectureTopology.builder()
                .vpcCidr("10.0.0.0/16")
                .region("ap-south-1")
                .availabilityZones(Collections.singletonList("ap-south-1a"))
                .build();

        when(awsArchitectureService.getTopology()).thenReturn(topology);

        mockMvc.perform(get("/api/v1/aws/topology")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vpcCidr").value("10.0.0.0/16"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/costs returns monthly estimate")
    void testGetCostEstimate() throws Exception {
        AwsCostEstimate cost = AwsCostEstimate.builder()
                .environment("Production")
                .monthlyTotalUsd(274.60)
                .monthlyTotalInr(22929.10)
                .build();

        when(awsArchitectureService.getMonthlyCostEstimate()).thenReturn(cost);

        mockMvc.perform(get("/api/v1/aws/costs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalUsd").value(274.60));
    }
}
