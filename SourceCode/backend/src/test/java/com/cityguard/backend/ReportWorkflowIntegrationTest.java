package com.cityguard.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ReportWorkflowIntegrationTest {
    MockMvc mvc;
    @Autowired WebApplicationContext webContext;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() { mvc = webAppContextSetup(webContext).build(); }

    @Test
    void userCanRegisterCreateVoteCommentAndListOwnReport() throws Exception {
        String username = "test_" + UUID.randomUUID();
        String authJson = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andReturn().getResponse().getContentAsString();
        JsonNode auth = objectMapper.readTree(authJson).path("data");
        String userId = auth.path("userId").asText();

        String reportJson = mvc.perform(post("/api/v1/reports").header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"req-" + username + "\",\"type\":\"TRAFFIC_JAM\",\"description\":\"Traffic is slow\",\"addressSnapshot\":\"Test street\",\"latitude\":10.8012,\"longitude\":106.7145}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.creatorId").value(userId)).andReturn().getResponse().getContentAsString();
        String reportId = objectMapper.readTree(reportJson).path("data").path("id").asText();

        mvc.perform(post("/api/v1/reports/" + reportId + "/vote").header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"CONFIRMED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userVote").value("CONFIRMED"));
        mvc.perform(post("/api/v1/reports/" + reportId + "/comments").header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Still happening\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.comments[0].content").value("Still happening"));
        mvc.perform(get("/api/v1/reports/" + reportId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(reportId));
        mvc.perform(get("/api/v1/reports/mine").header("X-User-Id", userId)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(reportId));
        mvc.perform(get("/api/v1/reports").param("bbox", "106.70,10.79,106.72,10.81")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
}
