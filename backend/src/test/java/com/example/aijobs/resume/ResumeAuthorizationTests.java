package com.example.aijobs.resume;

import com.example.aijobs.auth.JwtService;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.mock.web.MockMultipartFile;
import com.example.aijobs.resume.entity.Resume;
import java.nio.charset.StandardCharsets;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-key-with-at-least-thirty-two-bytes",
        "app.jwt.expiration=PT2H"
})
@AutoConfigureMockMvc
class ResumeAuthorizationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;
    @MockBean private JobPostingMapper jobMapper;
    @MockBean private ResumeMapper resumeMapper;
    @MockBean private PlatformTransactionManager transactionManager;

    @Test
    void anonymousUserCannotListResumes() throws Exception {
        mockMvc.perform(get("/api/student/resumes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrCannotCreateResume() throws Exception {
        String token = tokenFor(7L, "hr1", "HR");

        mockMvc.perform(post("/api/student/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanCreateDraft() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(post("/api/student/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(post("/api/student/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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
                {"title":"Java 后端简历","education":"本科","workExperience":"实习经历",
                 "projectExperience":"项目经历","skills":"Java, Spring Boot","selfEvaluation":"认真负责"}
                """;
    }

    @Test
    void studentPreviewsTextWithoutAnyResumeDatabaseWrite() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        mockMvc.perform(multipart("/api/student/resumes/import-preview")
                        .file(textFile()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("英语 教学设计"))
                .andExpect(jsonPath("$.data.format").value("TXT"));
        verifyNoInteractions(resumeMapper, jobMapper);
    }

    @Test
    void anonymousHrAndAdminCannotPreviewCvFiles() throws Exception {
        mockMvc.perform(multipart("/api/student/resumes/import-preview").file(textFile()))
                .andExpect(status().isUnauthorized());
        for (String role : List.of("HR", "ADMIN")) {
            String token = tokenFor(7L, "other", role);
            mockMvc.perform(multipart("/api/student/resumes/import-preview")
                            .file(textFile()).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(resumeMapper);
    }

    @Test
    void missingFileAndUnsupportedFileReturnUnifiedErrors() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        mockMvc.perform(multipart("/api/student/resumes/import-preview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(multipart("/api/student/resumes/import-preview")
                        .file(new MockMultipartFile("file", "cv.docx", "application/octet-stream", new byte[]{1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void oversizedUploadReturns413AndNoWrite() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        mockMvc.perform(multipart("/api/student/resumes/import-preview")
                        .file(new MockMultipartFile("file", "cv.txt", "text/plain", new byte[2 * 1024 * 1024 + 1]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(resumeMapper);
    }

    @Test
    void confirmedTextCreatesOnlyAuthenticatedStudentsDraft() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        mockMvc.perform(post("/api/student/resumes").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"导入简历\",\"sourceText\":\"英语 教学设计\",\"studentId\":99,\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.sourceText").value("英语 教学设计"));
    }

    @Test
    void cannotBypassTextLimitThroughSaveEndpoint() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");
        mockMvc.perform(post("/api/student/resumes").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"CV\",\"sourceText\":\"" + "a".repeat(20001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resumeMapper);
    }

    @Test
    void anotherStudentCannotReadOrOverwriteImportedText() throws Exception {
        String token = tokenFor(99L, "student2", "STUDENT");
        Resume resume = new Resume();
        resume.setId(7L);
        resume.setStudentId(42L);
        resume.setSourceText("private imported text");
        when(resumeMapper.selectById(7L)).thenReturn(resume);
        mockMvc.perform(get("/api/student/resumes/7").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/student/resumes/7").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"title\":\"CV\",\"sourceText\":\"changed\"}"))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile textFile() {
        return new MockMultipartFile("file", "cv.txt", "text/plain", "英语 教学设计".getBytes(StandardCharsets.UTF_8));
    }
}
