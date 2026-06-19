package com.sprinklr.sprintplanning.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response envelope")
public record ApiResponse<T>(
        @Schema(description = "Whether the request succeeded") boolean success,
        @Schema(description = "Response payload on success") T data,
        @Schema(description = "Error details on failure") ErrorDetail error
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(false, null, error);
    }
}
