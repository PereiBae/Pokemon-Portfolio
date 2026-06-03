package com.pokemonportfolio.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardService cardService;

    @Test
    @WithUserDetails("owner@example.com")
    void addPageRendersCardOptionWithSetName() throws Exception {
        CardForm form = new CardForm();
        form.setName("Umbreon");
        form.setSetName("VS1 Add Page Test");
        form.setCardNumber("ADD-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);

        Card card = cardService.createManualCard(form);

        mockMvc.perform(get("/portfolio/add").param("cardId", card.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Umbreon")))
                .andExpect(content().string(containsString("VS1 Add Page Test")))
                .andExpect(content().string(containsString("Alternate Art")));
    }
}
