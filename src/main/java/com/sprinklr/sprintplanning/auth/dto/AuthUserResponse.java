package com.sprinklr.sprintplanning.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated user profile from Microsoft Entra ID JWT")
public record AuthUserResponse(
    @Schema(example = "user@sprinklr.com") String email,
    @Schema(example = "Jane Doe") String name,
    @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String oid
) {
}
