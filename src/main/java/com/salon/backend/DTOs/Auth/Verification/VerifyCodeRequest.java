package com.salon.backend.DTOs.Auth.Verification;

public record VerifyCodeRequest(String email, String code) {
}
