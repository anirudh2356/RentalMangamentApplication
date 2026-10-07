package com.realestate.search.dto;

import java.util.List;

public record SearchResponse(List<SearchResult> results) {
}
