package com.cityguard.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoteRequest(@NotBlank String type) {}
