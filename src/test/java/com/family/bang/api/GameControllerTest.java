package com.family.bang.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void completeApiFlowProtectsSecretsAndRoles() throws Exception {
        JsonNode game = json.readTree(mvc.perform(post("/api/games"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String code = game.get("gameCode").asText();
        String hostToken = game.get("hostToken").asText();
        mvc.perform(get("/api/games/codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameCodes").isArray())
                .andExpect(jsonPath("$.gameCodes", org.hamcrest.Matchers.hasItem(code)))
                .andExpect(jsonPath("$.hostToken").doesNotExist());
        List<String> playerTokens = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            JsonNode player = json.readTree(mvc.perform(post("/api/games/{code}/players", code)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"playerName\":\"Player " + i + "\"}"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
            playerTokens.add(player.get("playerToken").asText());
        }

        mvc.perform(get("/api/games/{code}/lobby", code).header("Authorization", "Bearer " + playerTokens.get(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.players", hasSize(4)))
                .andExpect(jsonPath("$.hostToken").doesNotExist()).andExpect(jsonPath("$.playerToken").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
        mvc.perform(post("/api/games/{code}/deal", code).header("Authorization", "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/games/{code}/deal", code).header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/games/{code}/role", code).header("Authorization", "Bearer " + playerTokens.get(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").isString())
                .andExpect(jsonPath("$.playerName").doesNotExist());
        mvc.perform(get("/api/games/{code}/role", code).header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isUnauthorized());
    }
}
