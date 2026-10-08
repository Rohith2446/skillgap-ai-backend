package com.skillgapai;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillGapWorkflowIntegrationTest {
    private static final String EMAIL = "flow@example.com";
    private static final String PASSWORD = "Secure-Password-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsAuthenticatedCareerReadinessWorkflow() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"flow@example.com","password":"Secure-Password-2026","fullName":"Test Student","role":"STUDENT"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"Secure-Password-2026","fullName":"Public Admin","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/target-role").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"Backend Developer\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/profile/target-role").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"Unlisted Role\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/progress").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentComplete").value(false))
                .andExpect(jsonPath("$.totalModules").value(0));

        mockMvc.perform(get("/api/interview/questions").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/assessment").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Java\":0,\"SQL\":0,\"Communication\":0,\"Problem solving\":0,\"Data analysis\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/assessment").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/assessment").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Java\":0,\"SQL\":0,\"Communication\":0,\"Problem solving\":0,\"Data analysis\":0,\"Cloud fundamentals\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentComplete").value(true));

        mockMvc.perform(put("/api/assessment").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Java\":4,\"SQL\":1,\"Communication\":2,\"Problem solving\":3,\"Data analysis\":1,\"Cloud fundamentals\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole").value("Backend Developer"))
                .andExpect(jsonPath("$.analysisMethod").value("DETERMINISTIC_FALLBACK"))
                .andExpect(jsonPath("$.criticalSkills").isArray());

        MvcResult roadmapResult = mockMvc.perform(post("/api/roadmap/generate").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();
        JsonNode firstTask = objectMapper.readTree(roadmapResult.getResponse().getContentAsString()).get(0);
        long taskId = firstTask.get("id").asLong();

        mockMvc.perform(get("/api/progress").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentComplete").value(true))
                .andExpect(jsonPath("$.totalModules").value(3))
                .andExpect(jsonPath("$.completionPercent").value(0));

        mockMvc.perform(put("/api/roadmap/" + taskId).with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/progress").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedModules").value(1))
                .andExpect(jsonPath("$.completionPercent").value(33));

        mockMvc.perform(get("/api/interview/questions").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/api/interview/evaluate").with(httpBasic(EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"I would review the logs and metrics first. For example, I would compare the deployment window with error rates, isolate the failing dependency, and roll back if customer impact continued.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationMethod").value("RULE_BASED_PRACTICE_FEEDBACK"));

        mockMvc.perform(get("/api/auth/me").with(httpBasic(EMAIL, "incorrect-password")))
                .andExpect(status().isUnauthorized());
    }
}