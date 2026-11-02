package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.reform.dev.repositories.TaskStatusRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @DisplayName("Should return configured task statuses in display order")
    @Test
    void shouldRetrieveTaskStatuses() throws Exception {
        var expectedStatuses = taskStatusRepository.findAllByOrderByDisplayOrderAsc();

        var result = mockMvc.perform(get("/task-statuses"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(expectedStatuses.size()));

        for (int i = 0; i < expectedStatuses.size(); i++) {
            var expectedStatus = expectedStatuses.get(i);

            result
                .andExpect(jsonPath("$[" + i + "].code").value(expectedStatus.getCode()))
                .andExpect(jsonPath("$[" + i + "].label").value(expectedStatus.getLabel()))
                .andExpect(jsonPath("$[" + i + "].displayOrder").value(expectedStatus.getDisplayOrder()))
                .andExpect(jsonPath("$[" + i + "].isDefault").value(expectedStatus.isDefault()));
        }
    }
}
