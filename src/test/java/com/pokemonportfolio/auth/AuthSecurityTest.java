package com.pokemonportfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.auth.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.owner.username}")
    private String ownerUsername;

    @Value("${app.owner.bootstrap-password}")
    private String ownerPassword;

    @Test
    void protectedRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void ownerCanLogInWithHashedPassword() throws Exception {
        var owner = appUserRepository.findByUsername(ownerUsername).orElseThrow();

        assertThat(owner.isEnabled()).isTrue();
        assertThat(owner.getRole()).isEqualTo("ROLE_OWNER");
        assertThat(owner.getPasswordHash()).doesNotContain(ownerPassword);
        assertThat(passwordEncoder.matches(ownerPassword, owner.getPasswordHash())).isTrue();

        mockMvc.perform(formLogin("/login")
                        .user(ownerUsername)
                        .password(ownerPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
