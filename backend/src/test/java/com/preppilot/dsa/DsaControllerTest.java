package com.preppilot.dsa;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DsaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProblemRepository problems;

    String auth;

    @BeforeEach
    void register() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dsa-http@test.dev\",\"password\":\"hunter22!\"}"))
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(body).get("token").asText();
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/dsa/problems")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsAndFiltersProblems() throws Exception {
        mvc.perform(get("/api/dsa/problems").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));
        mvc.perform(get("/api/dsa/problems").param("category", "LINKED_LISTS").param("difficulty", "EASY").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].category").value("LINKED_LISTS"))
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void hintThenSolveFlowOverHttp() throws Exception {
        Long id = problems.findBySlug("reverse-linked-list").orElseThrow().getId();

        mvc.perform(get("/api/dsa/problems/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statement", notNullValue()))
                .andExpect(jsonPath("$.progress").doesNotExist());

        mvc.perform(post("/api/dsa/problems/" + id + "/hints").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"attempt\":\"while loop?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hint.depth").value(1))
                .andExpect(jsonPath("$.hint.maxDepth").value(3))
                .andExpect(jsonPath("$.progress.hintsUsed").value(1));

        mvc.perform(post("/api/dsa/problems/" + id + "/solve").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.status").value("SOLVED"))
                .andExpect(jsonPath("$.escalated").value(false))
                .andExpect(jsonPath("$.streakDays").value(1));

        mvc.perform(get("/api/dsa/progress").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solvedCount").value(1))
                .andExpect(jsonPath("$.recommendedTiers.LINKED_LISTS").value("EASY"));
    }

    @Test
    void unknownProblemIs404() throws Exception {
        mvc.perform(post("/api/dsa/problems/999999/hints").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }
}
