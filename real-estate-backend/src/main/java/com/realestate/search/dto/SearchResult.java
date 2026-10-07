package com.realestate.search.dto;

public record SearchResult(
        String type,
        Long id,
        String title,
        String subtitle,
        String navigationTarget
) {
}
