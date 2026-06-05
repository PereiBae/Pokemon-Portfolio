package com.pokemonportfolio.config.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.config.service.ProviderSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderSettingsService providerSettingsService;

    @Test
    @WithUserDetails("owner@example.com")
    void providerSettingsPageShowsDefaultProviderStatus() throws Exception {
        mockMvc.perform(get("/settings/providers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mock Pricing Provider")))
                .andExpect(content().string(containsString("Manual Price Entry")))
                .andExpect(content().string(containsString("TCGPlayer")))
                .andExpect(content().string(containsString("Test Pokemon API")))
                .andExpect(content().string(containsString("Disabled by default")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void postProviderToggleUpdatesProviderState() throws Exception {
        mockMvc.perform(post("/settings/providers")
                        .with(csrf())
                        .param("providerKey", ProviderSettingsService.MOCK_PROVIDER)
                        .param("enabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/providers"));

        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MOCK_PROVIDER)).isFalse();
    }
}
