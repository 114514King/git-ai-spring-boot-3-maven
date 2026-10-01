package com.example.aijobs.externaljobs;

import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.job.mapper.JobPostingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H",
        "app.external-jobs.enabled=true"
})
@AutoConfigureMockMvc
class ExternalJobsHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private JobPostingMapper jobMapper;
    @MockBean private PlatformTransactionManager transactionManager;
    @MockBean private JobicyClient client;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void anonymousReaderReceivesUnifiedResponseWithSourceMetadata() throws Exception {
        when(client.fetch()).thenReturn(List.of(new ExternalJobResponse(
                "101", "Software Developer", "Acme", "United States", "full-time",
                "USD 120000 – 150000 / 年", Instant.parse("2026-09-30T04:30:00Z"),
                "Build services", List.of("Engineering"), "https://jobicy.com/jobs/101-developer")));

        mockMvc.perform(get("/api/external-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.data.source").value("JOBICY"))
                .andExpect(jsonPath("$.data.sourceName").value("Jobicy"))
                .andExpect(jsonPath("$.data.jobs[0].id").value("101"))
                .andExpect(jsonPath("$.data.jobs[0].sourceUrl").value("https://jobicy.com/jobs/101-developer"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.stale").value(false))
                .andExpect(jsonPath("$.data.lastSyncedAt").isNotEmpty());

        mockMvc.perform(get("/api/external-jobs").param("keyword", "no matching job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        verify(client, times(1)).fetch();
    }

    @Test
    void invalidPagingAndFiltersReturnUniformBadRequestBeforeUpstreamCall() throws Exception {
        for (var request : List.of(
                get("/api/external-jobs").param("page", "0"),
                get("/api/external-jobs").param("pageSize", "0"),
                get("/api/external-jobs").param("pageSize", "51"),
                get("/api/external-jobs").param("keyword", "a".repeat(101)),
                get("/api/external-jobs").param("location", "a".repeat(101)))) {
            mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(not(emptyString())))
                    .andExpect(jsonPath("$.data").value(nullValue()));
        }
        verifyNoInteractions(client);
    }

    @Test
    void nonnumericPaginationReturnsUniformBadRequest() throws Exception {
        mockMvc.perform(get("/api/external-jobs").param("page", "first"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(client);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void upstreamFailureWithoutCacheReturnsUniformServiceUnavailable() throws Exception {
        when(client.fetch()).thenThrow(new IOException("upstream unavailable"));

        mockMvc.perform(get("/api/external-jobs"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verify(client, times(1)).fetch();
    }

    @Test
    void anonymousReaderCannotPostToExternalJobsRoute() throws Exception {
        mockMvc.perform(post("/api/external-jobs").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(client);
    }
}
