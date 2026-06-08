package com.pokemonportfolio.config.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalLayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageUsesPremiumOwnerTerminalTheme() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("login-body")))
                .andExpect(content().string(containsString("Portfolio Exchange")))
                .andExpect(content().string(containsString("Owner Terminal")))
                .andExpect(content().string(containsString("Portfolio Login")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void authenticatedPagesUseSharedSidebarShell() throws Exception {
        for (String path : List.of(
                "/dashboard",
                "/portfolio",
                "/catalog/search",
                "/cards/new",
                "/sealed-products",
                "/pricing/manual-entry",
                "/settings/exchange-rates",
                "/trades",
                "/grading",
                "/alerts",
                "/settings/providers")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Portfolio Exchange")))
                    .andExpect(content().string(containsString("Official Catalogue")))
                    .andExpect(content().string(containsString("Exchange Rates")))
                    .andExpect(content().string(containsString("Logout")))
                    .andExpect(content().string(not(containsString("POKEMON_API_RAPIDAPI_KEY"))));
        }
    }
}
