package com.example.aijobs.auth;

import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
class RoleAuthorizationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private PlatformUserMapper userMapper;
    @MockBean private RoleMapper roleMapper;

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/access/student"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void studentCanAccessStudentEndpoint() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(get("/api/access/student")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"));
    }

    @Test
    void studentCannotAccessHrEndpoint() throws Exception {
        String token = tokenFor(42L, "student1", "STUDENT");

        mockMvc.perform(get("/api/access/hr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无权访问该资源"));
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
}
