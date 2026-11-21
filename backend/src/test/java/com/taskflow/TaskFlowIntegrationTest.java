package com.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"jwt.secret=", "debug=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void authenticatesAndEnforcesOwnershipAcrossTheTaskLifecycle() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());

        String email = UUID.randomUUID() + "@example.com";
        String owner = register("Project Owner", email);
        String other = register("Other User", UUID.randomUUID() + "@example.com");
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.createObjectNode().put("email", email).put("password", "integration-password").toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());

        String project = "/api/projects/" + read(mvc.perform(post("/api/projects").header("Authorization", owner)
                        .contentType("application/json").content("{\"name\":\"Release plan\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/projects").header("Authorization", other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(project + "/summary").header("Authorization", other)).andExpect(status().isForbidden());

        ObjectNode input = json.createObjectNode().put("title", "Ship release").put("priority", "HIGH")
                .put("dueDate", LocalDate.now().minusDays(1).toString());
        mvc.perform(post(project + "/tasks").header("Authorization", owner).contentType("application/json").content(input.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors[0].field").value("dueDate"));

        input.put("dueDate", LocalDate.now().plusDays(1).toString());
        String task = "/api/tasks/" + read(mvc.perform(post(project + "/tasks").header("Authorization", owner)
                        .contentType("application/json").content(input.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get(task).header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Ship release"));
        mvc.perform(get(task).header("Authorization", other)).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").doesNotExist());

        input.put("title", "Review notes").put("priority", "MEDIUM");
        mvc.perform(post(project + "/tasks").header("Authorization", owner).contentType("application/json").content(input.toString()))
                .andExpect(status().isCreated());
        mvc.perform(get(project + "/tasks").header("Authorization", owner)
                        .param("sortBy", "priority").param("direction", "desc").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));
        mvc.perform(get(project + "/tasks").header("Authorization", owner).param("status", "INVALID"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors[0].field").value("status"));
        mvc.perform(patch(task + "/status").header("Authorization", other)
                        .contentType("application/json").content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());

        input.put("title", "Ship release").put("priority", "HIGH").put("status", "IN_PROGRESS")
                .put("dueDate", LocalDate.now().minusDays(1).toString());
        mvc.perform(put(task).header("Authorization", owner).contentType("application/json").content(input.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.overdue").value(true));
        mvc.perform(get(project + "/summary").header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.overdueCount").value(1));
        mvc.perform(patch(task + "/status").header("Authorization", owner)
                        .contentType("application/json").content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.overdue").value(false));
        mvc.perform(get(project + "/tasks").header("Authorization", owner).param("status", "DONE")
                        .param("priority", "HIGH").param("assigneeId", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(task + "/activities").header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].newStatus").value("DONE"));
        mvc.perform(delete(task).header("Authorization", owner)).andExpect(status().isNoContent());
        mvc.perform(get(project + "/summary").header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.overdueCount").value(0))
                .andExpect(jsonPath("$.totalTasks").value(1));
    }

    private String register(String name, String email) throws Exception {
        String body = json.createObjectNode().put("name", name).put("email", email)
                .put("password", "integration-password").toString();
        return "Bearer " + read(mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode read(String body) throws Exception {
        return json.readTree(body);
    }
}
