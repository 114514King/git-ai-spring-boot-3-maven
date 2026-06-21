package com.example.aijobs.job;

import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.job.mapper.JobPostingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
class JobAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private JobPostingMapper jobMapper;

    @Test
    void anonymousUserCanBrowsePublishedJobs() throws Exception {
        when(jobMapper.selectList(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void studentCannotCreateJob() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(post("/api/hr/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanCreateDraft() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");

        mockMvc.perform(post("/api/hr/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.hrId").value(7))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
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

    private String validRequest() {
        return """
                {"title":"Java 开发工程师","companyName":"示例科技","city":"上海",
                 "employmentType":"FULL_TIME","salaryMin":10000,"salaryMax":15000,
                 "description":"负责后端开发","requirements":"熟悉 Spring Boot"}
                """;
    }
}
