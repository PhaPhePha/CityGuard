package com.cityguard.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu vote cho báo cáo sự cố (CONFIRMED hoặc NO_LONGER).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

    @NotBlank(message = "Loại vote không được để trống")
    @JsonAlias({ "type", "vote_type" }) // Cho phép nhận cả "type", "voteType" lẫn "vote_type"
    private String voteType;

    public String getVoteType() {
        return voteType != null ? voteType.trim().toUpperCase() : "";
    }
}
