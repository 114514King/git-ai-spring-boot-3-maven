package com.example.aijobs.application;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
class JobApplicationAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private JobApplicationMapper applicationMapper;
    @MockBean private JobPostingMapper jobMapper;
    @MockBean private ResumeMapper resumeMapper;
    @MockBean private AiMatchResultMapper matchMapper;
    @MockBean private PlatformTransactionManager transactionManager;

    @Test
    void anonymousUserCannotSubmitApplication() throws Exception {
        mockMvc.perform(post("/api/student/applications")
                        .contentType("application/json")
                        .content("{\"jobId\":9,\"resumeId\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrCannotSubmitApplication() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");

        mockMvc.perform(post("/api/student/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"jobId\":9,\"resumeId\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanSubmitApplication() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume());
        when(applicationMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(post("/api/student/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"jobId\":9,\"resumeId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void studentCannotListHrApplications() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(get("/api/hr/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanReadCandidateRecommendations() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");
        when(jobMapper.selectList(any())).thenReturn(List.of(publishedJob()));
        when(applicationMapper.selectList(any())).thenReturn(List.of(application()));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume());

        mockMvc.perform(get("/api/hr/applications/recommendations?jobId=9")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicationId").value(11))
                .andExpect(jsonPath("$.data[0].scoreSource").value("local-candidate-ranker-v1"));
    }

    @Test
    void studentCannotReadCandidateRecommendations() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(get("/api/hr/applications/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanGenerateInterviewKit() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");
        JobPosting job = publishedJob();
        job.setTitle("Java Backend Engineer");
        job.setRequirements("Java Spring Boot MySQL");
        Resume resume = publishedResume();
        resume.setTitle("Java Resume");
        resume.setSkills("Java Spring Boot");
        when(applicationMapper.selectById(11L)).thenReturn(application());
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(resumeMapper.selectById(5L)).thenReturn(resume);

        mockMvc.perform(post("/api/hr/applications/11/interview-kit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(11))
                .andExpect(jsonPath("$.data.modelName").value("local-interview-kit-v1"))
                .andExpect(jsonPath("$.data.questions.length()").value(4));
    }

    @Test
    void studentCannotGenerateInterviewKit() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(post("/api/hr/applications/11/interview-kit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanUpdateApplicationStatus() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");
        when(applicationMapper.selectById(11L)).thenReturn(application());
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());

        mockMvc.perform(patch("/api/hr/applications/11/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"REVIEWING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWING"));
    }

    @Test
    void invalidHrStatusIsRejected() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");

        mockMvc.perform(patch("/api/hr/applications/11/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"WITHDRAWN\"}"))
                .andExpect(status().isBadRequest());
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

    private JobPosting publishedJob() {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(7L);
        job.setStatus("PUBLISHED");
        return job;
    }

    private Resume publishedResume() {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(42L);
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private com.example.aijobs.application.entity.JobApplication application() {
        var application = new com.example.aijobs.application.entity.JobApplication();
        application.setId(11L);
        application.setJobId(9L);
        application.setStudentId(42L);
        application.setResumeId(5L);
        application.setStatus("SUBMITTED");
        return application;
    }
}
