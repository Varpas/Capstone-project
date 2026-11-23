package com.bookworm.service;

import com.bookworm.config.JwtService;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.AuthResponse;
import com.bookworm.dto.Dto.LoginRequest;
import com.bookworm.dto.Dto.RegisterRequest;
import com.bookworm.dto.Dto.UserDto;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this e-mail already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(req.phone());
        users.save(user);
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid e-mail or password"));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(Long userId) {
        return toDto(users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Unknown user")));
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getGiftPoints());
    }

    private AuthResponse tokenFor(User user) {
        return new AuthResponse(jwt.issue(user.getId(), user.getEmail()), "Bearer", jwt.expirySeconds(),
                toDto(user));
    }
}
