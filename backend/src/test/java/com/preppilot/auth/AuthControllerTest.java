package com.preppilot.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void registerThenLoginThenMe() throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Kay@Example.com\",\"password\":\"hunter22!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value("kay@example.com"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kay@example.com"));

        JsonNode login = json.readTree(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kay@example.com\",\"password\":\"hunter22!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String req = "{\"email\":\"dup@example.com\",\"password\":\"hunter22!\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pw@example.com\",\"password\":\"hunter22!\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pw@example.com\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRejectMissingOrBadToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void shortPasswordIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
