package com.example.smartfactory.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PagedResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(
            long total,
            int page,
            @JsonProperty("per_page") int perPage) {
    }
}
