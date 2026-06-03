package com.pokemonportfolio.catalog.service;

import java.util.List;

public class OfficialCardSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final List<Integer> PAGE_SIZE_OPTIONS = List.of(25, 50, 100);

    private final String keyword;
    private final String cardName;
    private final String setName;
    private final String cardNumber;
    private final String rarity;
    private final int page;
    private final int pageSize;

    public OfficialCardSearchRequest(
            String keyword,
            String cardName,
            String setName,
            String cardNumber,
            String rarity,
            Integer page,
            Integer pageSize) {
        this.keyword = blankToNull(keyword);
        this.cardName = blankToNull(cardName);
        this.setName = blankToNull(setName);
        this.cardNumber = blankToNull(cardNumber);
        this.rarity = blankToNull(rarity);
        this.page = normalizePage(page);
        this.pageSize = normalizePageSize(pageSize);
    }

    public static OfficialCardSearchRequest keyword(String keyword) {
        return new OfficialCardSearchRequest(keyword, null, null, null, null, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
    }

    public boolean hasCriteria() {
        return keyword != null || cardName != null || setName != null || cardNumber != null || rarity != null;
    }

    public boolean hasAdvancedFilters() {
        return cardName != null || setName != null || cardNumber != null || rarity != null;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCardName() {
        return cardName;
    }

    public String getSetName() {
        return setName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getRarity() {
        return rarity;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<Integer> getPageSizeOptions() {
        return PAGE_SIZE_OPTIONS;
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || !PAGE_SIZE_OPTIONS.contains(pageSize)) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
