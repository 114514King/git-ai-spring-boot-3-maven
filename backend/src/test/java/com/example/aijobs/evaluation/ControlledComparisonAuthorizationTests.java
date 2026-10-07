package com.example.aijobs.evaluation;

import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
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
    @MockBean private DeepSeekChatClient deepSeekChatClient;
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
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void studentCannotReadControlledComparison() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison")
                        .header("Authorization", "Bearer " + tokenFor("STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void hrCannotReadControlledComparison() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison")
                        .header("Authorization", "Bearer " + tokenFor("HR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void adminCanReadControlledComparison() throws Exception {
        when(deepSeekChatClient.generate(anyList())).thenReturn(new DeepSeekChatResult(
                DeepSeekChatResult.Status.DISABLED, "", "deepseek-flash", null, null, null));

        mockMvc.perform(get("/api/admin/evaluations/day30-controlled-comparison")
                        .header("Authorization", "Bearer " + tokenFor("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationId").value("day30-controlled-comparison-2026-10-06"))
                .andExpect(jsonPath("$.data.runMode").value("rule-plus-fallback"))
                .andExpect(jsonPath("$.data.scenarioCount").value(3))
                .andExpect(jsonPath("$.data.fallbackCount").value(3))
                .andExpect(jsonPath("$.data.results[0].jobRequirements").value("English, lesson planning, classroom management"))
                .andExpect(jsonPath("$.data.results[0].resumeText").value("英语教学设计与备课经验，参与课后答疑；尚未写出班级秩序维护案例。"))
                .andExpect(jsonPath("$.data.results[0].ruleScore").value(66.67))
                .andExpect(jsonPath("$.data.results[0].missingSkills[0]").value("Classroom management"))
                .andExpect(jsonPath("$.data.humanReviewRequired").value(true));
        verify(deepSeekChatClient, times(3)).generate(anyList());
        verifyNoInteractions(resumeMapper, jobMapper, matchMapper, applicationMapper);
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
