package com.tradingsim.api;

import com.tradingsim.application.ResourceNotFoundException;
import com.tradingsim.application.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimulationService simulationService;

    @Test
    void previewCsv_returnsStandardBadRequestPayload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", "bad-data".getBytes());
        when(simulationService.previewCsv(any())).thenThrow(new IllegalArgumentException("Invalid CSV file."));

        mockMvc.perform(multipart("/api/v1/simulations/csv/preview").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid CSV file."))
                .andExpect(jsonPath("$.error").value("Invalid CSV file."))
                .andExpect(jsonPath("$.path").value("/api/v1/simulations/csv/preview"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    void compareRuns_withInvalidRunId_returnsValidationErrorPayload() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/runs/compare")
                        .param("leftRunId", "0")
                        .param("rightRunId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("leftRunId"))
                .andExpect(jsonPath("$.validationErrors[0].message").exists());
    }

    @Test
    void getRunById_whenMissing_returnsNotFoundPayload() throws Exception {
        when(simulationService.getRunById(999L))
                .thenThrow(new ResourceNotFoundException("Simulation run not found for id=999"));

        mockMvc.perform(get("/api/v1/simulations/runs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Simulation run not found for id=999"))
                .andExpect(jsonPath("$.path").value("/api/v1/simulations/runs/999"));
    }
}
