package com.pokemonportfolio.catalog.service;

import java.util.List;

public class OfficialCardSearchPage {

    private final List<OfficialCardSearchResult> results;
    private final int page;
    private final int pageSize;
    private final Integer totalCount;

    public OfficialCardSearchPage(
            List<OfficialCardSearchResult> results,
            int page,
            int pageSize,
            Integer totalCount) {
        this.results = List.copyOf(results);
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public static OfficialCardSearchPage empty(OfficialCardSearchRequest request) {
        return new OfficialCardSearchPage(List.of(), request.getPage(), request.getPageSize(), null);
    }

    public List<OfficialCardSearchResult> getResults() {
        return results;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    public boolean hasNext() {
        if (totalCount == null) {
            return results.size() == pageSize;
        }
        return page * pageSize < totalCount;
    }

    public int getNextPage() {
        return page + 1;
    }

    public Integer getTotalPages() {
        if (totalCount == null) {
            return null;
        }
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
