package com.example.aijobs.assistant;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.mapper.ResumeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
class RecruitmentChatAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;
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
    void anonymousCannotChat() throws Exception {
        mockMvc.perform(post("/api/student/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void studentCannotUseHrChat() throws Exception {
        mockMvc.perform(post("/api/hr/assistant/chat").header("Authorization", "Bearer " + tokenFor("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void hrCannotUseStudentChat() throws Exception {
        mockMvc.perform(post("/api/student/assistant/chat").header("Authorization", "Bearer " + tokenFor("HR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void studentReceivesExplicitDisabledFallback() throws Exception {
        when(deepSeekChatClient.generate(anyList())).thenReturn(new DeepSeekChatResult(
                DeepSeekChatResult.Status.DISABLED, "", "deepseek-flash", null, null, null));
        mockMvc.perform(post("/api/student/assistant/chat").header("Authorization", "Bearer " + tokenFor("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.generated").value(false))
                .andExpect(jsonPath("$.data.modelStatus").value("DISABLED"))
                .andExpect(jsonPath("$.data.humanReviewRequired").value(true));
        verifyNoInteractions(applicationMapper, jobMapper, resumeMapper, matchMapper);
    }

    @Test
    void hrReceivesExplicitDisabledFallback() throws Exception {
        when(deepSeekChatClient.generate(anyList())).thenReturn(new DeepSeekChatResult(
                DeepSeekChatResult.Status.DISABLED, "", "deepseek-flash", null, null, null));
        mockMvc.perform(post("/api/hr/assistant/chat").header("Authorization", "Bearer " + tokenFor("HR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("HR"))
                .andExpect(jsonPath("$.data.generated").value(false));
    }

    @Test
    void overlongMessageIsRejectedBeforeCallingModel() throws Exception {
        mockMvc.perform(post("/api/student/assistant/chat").header("Authorization", "Bearer " + tokenFor("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void blankMessageIsRejectedBeforeCallingModel() throws Exception {
        mockMvc.perform(post("/api/student/assistant/chat").header("Authorization", "Bearer " + tokenFor("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void unknownConversationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/student/assistant/chat").header("Authorization", "Bearer " + tokenFor("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Synthetic question\",\"conversationId\":\"chat_unknown\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void loginJsonExposesDatabaseRolesForFrontendRouting() throws Exception {
        PlatformUser user = new PlatformUser();
        user.setId(42L);
        user.setUsername("synthetic-hr");
        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode("synthetic-password"));
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(roleMapper.selectCodesByUserId(42L)).thenReturn(List.of("HR"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"synthetic-hr\",\"password\":\"synthetic-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andExpect(jsonPath("$.data.roles[0]").value("HR"));
        verifyNoInteractions(deepSeekChatClient);
    }

    @Test
    void changedContextReturnsUnifiedConflictWithoutAnotherModelCall() throws Exception {
        when(deepSeekChatClient.generate(anyList())).thenReturn(new DeepSeekChatResult(
                DeepSeekChatResult.Status.DISABLED, "", "deepseek-flash", null, null, null));
        String token = tokenFor("HR");
        String body = mockMvc.perform(post("/api/hr/assistant/chat").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Synthetic question\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String conversationId = objectMapper.readTree(body).path("data").path("conversationId").asText();
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(42L);
        job.setTitle("Synthetic job");
        when(jobMapper.selectById(9L)).thenReturn(job);

        mockMvc.perform(post("/api/hr/assistant/chat").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecruitmentChatRequest(
                                conversationId, "Synthetic new context", null, 9L, null))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        verify(deepSeekChatClient, times(1)).generate(anyList());
    }

    private String tokenFor(String role) {
        PlatformUser user = new PlatformUser();
        user.setId(42L);
        user.setUsername("synthetic-chat-test");
        user.setStatus("ACTIVE");
        when(userMapper.selectById(42L)).thenReturn(user);
        when(roleMapper.selectCodesByUserId(42L)).thenReturn(List.of(role));
        return jwtService.issue(42L, user.getUsername()).value();
    }
}
