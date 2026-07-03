package com.example.aijobs.match;

import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
class AiMatchAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private ResumeMapper resumeMapper;
    @MockBean private JobPostingMapper jobMapper;
    @MockBean private JobApplicationMapper applicationMapper;
    @MockBean private AiMatchResultMapper matchMapper;
    @MockBean private PlatformTransactionManager transactionManager;

    @Test
    void anonymousUserCannotGenerateStudentMatch() throws Exception {
        mockMvc.perform(post("/api/student/matches")
                        .contentType("application/json")
                        .content("{\"resumeId\":5,\"jobId\":9}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrCannotGenerateStudentMatch() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");

        mockMvc.perform(post("/api/student/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"resumeId\":5,\"jobId\":9}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanGenerateMatch() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume());
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());
        when(matchMapper.selectOne(any())).thenReturn(null);

        mockMvc.perform(post("/api/student/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"resumeId\":5,\"jobId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value(5))
                .andExpect(jsonPath("$.data.jobId").value(9))
                .andExpect(jsonPath("$.data.modelName").value("local-keyword-match-v2"))
                .andExpect(jsonPath("$.data.strengthSummary").exists())
                .andExpect(jsonPath("$.data.gapSummary").exists())
                .andExpect(jsonPath("$.data.actionSuggestions").exists());
    }

    @Test
    void studentCannotListHrMatches() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(get("/api/hr/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanGenerateMatchForSubmittedResume() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume());
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        when(matchMapper.selectOne(any())).thenReturn(null);

        mockMvc.perform(post("/api/hr/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"resumeId\":5,\"jobId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(33.33));
    }

    private String tokenFor(Long userId, String username, String role) {
        PlatformUser user = new PlatformUser();
        user.setId(userId);
        user.setUsername(username);
        user.setStatus("ACTIVE");
        when(userMapper.selectById(userId)).thenReturn(user);
        when(roleMapper.selectCodesByUserId(userId)).thenReturn(List.of(role));
        return jwtService.issue(userId, username).value();
    }

    private Resume publishedResume() {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(42L);
        resume.setTitle("Java 后端简历");
        resume.setSkills("Java Spring Boot MySQL");
        resume.setProjectExperience("招聘平台后端开发");
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private JobPosting publishedJob() {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(7L);
        job.setTitle("Java 开发工程师");
        job.setDescription("负责 Java 后端服务");
        job.setRequirements("Spring Boot 经验");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("PUBLISHED");
        return job;
    }
}
