package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawalRequest(@NotBlank String password) {
}
