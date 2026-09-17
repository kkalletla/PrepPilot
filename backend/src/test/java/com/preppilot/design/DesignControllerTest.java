package com.preppilot.design;

import static org.hamcrest.Matchers.hasSize;
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
class DesignControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DesignQuestionRepository questions;

    String auth;

    @BeforeEach
    void register() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"design-http@test.dev\",\"password\":\"hunter22!\"}"))
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(body).get("token").asText();
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/design/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsQuestions() throws Exception {
        mvc.perform(get("/api/design/questions").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].seniority").value("MID"));
    }

    @Test
    void stagedSessionOverHttp() throws Exception {
        Long qid = questions.findBySlug("url-shortener").orElseThrow().getId();
        String created = mvc.perform(post("/api/design/sessions").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"questionId\":" + qid + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("REQUIREMENTS"))
                .andExpect(jsonPath("$.transcript", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long sid = json.readTree(created).get("id").asLong();

        mvc.perform(post("/api/design/sessions/" + sid + "/answers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"read heavy, 10k qps, unique 7 char codes, expiry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback.stage").value("REQUIREMENTS"))
                .andExpect(jsonPath("$.feedback.stageScore").value(10))
                .andExpect(jsonPath("$.session.stage").value("COMPONENTS"))
                .andExpect(jsonPath("$.session.rubric").doesNotExist());

        for (String a : new String[]{"api, base62, cache, database", "table, key, nosql, created", "shard, replica, cache, collision, analytics"}) {
            mvc.perform(post("/api/design/sessions/" + sid + "/answers").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"" + a + "\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/design/sessions/" + sid).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("COMPLETE"))
                .andExpect(jsonPath("$.rubric.overall").isNumber())
                .andExpect(jsonPath("$.rubric.dimensions.SCALABILITY").value(10));

        mvc.perform(post("/api/design/sessions/" + sid + "/answers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"more\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void blankAnswerIsBadRequestAndUnknownSessionIs404() throws Exception {
        mvc.perform(post("/api/design/sessions/1/answers").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/design/sessions/999999").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }
}
