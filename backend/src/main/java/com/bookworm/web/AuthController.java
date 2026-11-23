package com.bookworm.web;

import com.bookworm.dto.Dto.AuthResponse;
import com.bookworm.dto.Dto.LoginRequest;
import com.bookworm.dto.Dto.RegisterRequest;
import com.bookworm.dto.Dto.UserDto;
import com.bookworm.service.AuthService;
import com.bookworm.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/users/me")
    public UserDto me() {
        return auth.currentUser(CurrentUser.requireId());
    }
}
