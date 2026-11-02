package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseworkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Should retrieve the seeded caseworkers")
    @Test
    void shouldRetrieveCaseworkers() throws Exception {
        mockMvc.perform(get("/caseworkers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.name == 'Danger Mouse')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Count Duckula')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Super Ted')]").exists());
    }
}
