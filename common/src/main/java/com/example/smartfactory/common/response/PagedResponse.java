package com.example.smartfactory.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * ページネーション付きレスポンスの共通ラッパー。{"data": [...], "pagination": {...}} 形式で返す。
 *
 * @param <T> 要素の型
 */
public record PagedResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(long total, int page, @JsonProperty("per_page") int perPage) {}

    public static <T> PagedResponse<T> of(List<T> data, long total, int page, int perPage) {
        return new PagedResponse<>(data, new Pagination(total, page, perPage));
    }
}
