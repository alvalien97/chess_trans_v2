package com.transcendence.chess.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.dto.AccountUpdateRequest;
import com.transcendence.chess.dto.AddFriendRequest;
import com.transcendence.chess.dto.AuthRequest;
import com.transcendence.chess.dto.AuthResponse;
import com.transcendence.chess.dto.DeleteAccountRequest;
import com.transcendence.chess.dto.FriendView;
import com.transcendence.chess.dto.PublicProfileView;
import com.transcendence.chess.model.User;
import com.transcendence.chess.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) {
        authService.requireUserId(authorization);
        authService.logout(authorization);
    }

    @PutMapping("/account")
    public User updateAccount(@RequestHeader("Authorization") String authorization,
                              @Valid @RequestBody AccountUpdateRequest request) {
        return authService.updateAccount(authService.requireUserId(authorization), request);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@RequestHeader("Authorization") String authorization,
                              @Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(authService.requireUserId(authorization), request.getCurrentPassword());
    }

    @GetMapping("/friends")
    public List<FriendView> getFriends(@RequestHeader("Authorization") String authorization) {
        return authService.getFriends(authService.requireUserId(authorization));
    }

    @GetMapping("/friends/search")
    public List<FriendView> searchUsers(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String query) {
        return authService.searchUsers(authService.requireUserId(authorization), query);
    }

    @GetMapping("/users/{userId}")
    public PublicProfileView getPublicProfile(@RequestHeader("Authorization") String authorization,
                                              @PathVariable Long userId) {
        authService.requireUserId(authorization);
        return authService.getPublicProfile(userId);
    }

    @PostMapping("/friends")
    public FriendView addFriend(@RequestHeader("Authorization") String authorization,
                                @RequestBody AddFriendRequest request) {
        return authService.addFriend(authService.requireUserId(authorization), request.getUsername());
    }

    @DeleteMapping("/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long friendId) {
        authService.removeFriend(authService.requireUserId(authorization), friendId);
    }
}
