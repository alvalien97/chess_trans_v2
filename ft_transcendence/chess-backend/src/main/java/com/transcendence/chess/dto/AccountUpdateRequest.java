package com.transcendence.chess.dto;

import jakarta.validation.constraints.Size;

public class AccountUpdateRequest {
    private String currentPassword;

    @Size(min = 3, max = 32)
    private String username;

    private String newPassword;
    private String avatarUrl;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}