package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.dto.CreateUserRequest;
import com.sportbuddy.sportbuddy.dto.CurrentUserResponse;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal Jwt principal) {
        return userService.getCurrentUser(Long.valueOf(principal.getSubject()));
    }
}
