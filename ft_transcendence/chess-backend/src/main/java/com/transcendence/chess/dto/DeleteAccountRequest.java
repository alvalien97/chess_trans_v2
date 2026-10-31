package com.transcendence.chess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeleteAccountRequest {
    @NotBlank
    @Size(min = 8, max = 72)
    private String currentPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}