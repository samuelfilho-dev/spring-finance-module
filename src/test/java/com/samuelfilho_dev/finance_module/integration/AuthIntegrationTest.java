package com.samuelfilho_dev.finance_module.integration;

import com.jayway.jsonpath.JsonPath;
import tools.jackson.databind.ObjectMapper;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.utils.AESService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.mongodb.uri=mongodb://127.0.0.1:27017/finance_module_it",
        "de.flapdoodle.mongodb.embedded.version=7.0.5",
        "app.secret.admin-password=adminPassword123",
        "app.secret.mfa-secret=12345678901234567890123456789012"
})
class AuthIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AESService aesService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserAndCompleteMfaFlow() throws Exception {
        var email = "integration.user@test.com";
        var password = "StrongPassword@123";

        MvcResult createdResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest("Integration User", email, password, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setupToken", not(isEmptyString())))
                .andReturn();

        JsonPath.read(createdResult.getResponse().getContentAsString(), "$.setupToken");
        var user = userRepository.findUserByEmail(email).orElseThrow();
        var secret = aesService.decrypt(user.getMfaSecret());
        var code = new DefaultCodeGenerator().generate(secret, System.currentTimeMillis() / 30000L);

        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MfaRequest(email, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
        var validCode = new DefaultCodeGenerator().generate(secret, System.currentTimeMillis() / 30000L);

        MvcResult tokenResult = mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MfaRequest(email, validCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        var accessToken = JsonPath.read(tokenResult.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void shouldAllowAdminToListUsers() throws Exception {
        var admin = userRepository.save(User.builder()
                .name("Admin")
                .email("admin@admin.com.br")
                .password("encoded-password")
                .role("ROLE_ADMIN")
                .mfaSecret(aesService.encrypt("JBSWY3DPEHPK3PXP"))
                .build());

        var token = jwtService.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(admin.getEmail()));
    }
}
