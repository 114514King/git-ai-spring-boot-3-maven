package com.example.aijobs.evaluation;

import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
class ControlledComparisonAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private ControlledComparisonService comparisonService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private UserRoleMapper userRoleMapper;
    @MockBean private JobApplicationMapper applicationMapper;
    @MockBean private JobPostingMapper jobMapper;
    @MockBean private ResumeMapper resumeMapper;
    @MockBean private AiMatchResultMapper matchMapper;
    @MockBean private PlatformTransactionManager transactionManager;

    @Test
    void anonymousUserCannotReadControlledComparison() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotReadControlledComparison() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison")
                        .header("Authorization", "Bearer " + tokenFor("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadControlledComparison() throws Exception {
        when(comparisonService.runDay30Comparison()).thenReturn(new ControlledComparisonReport(
                "day30-controlled-comparison-2026-10-06",
                "rule-plus-fallback",
                "local-skill-coverage-v1",
                "skill-dictionary-v1",
                0,
                0,
                0,
                true,
                "ok",
                List.of(),
                List.of("synthetic only")));

        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison")
                        .header("Authorization", "Bearer " + tokenFor("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationId").value("day30-controlled-comparison-2026-10-06"))
                .andExpect(jsonPath("$.data.humanReviewRequired").value(true));
    }

    private String tokenFor(String role) {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setStatus("ACTIVE");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of(role));
        return jwtService.issue(1L, "tester").value();
    }
}
