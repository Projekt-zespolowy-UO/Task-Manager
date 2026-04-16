package com.project.backend.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.PasswordResetConfirmDto;
import com.project.backend.Dto.PasswordResetRequestDto;
import com.project.backend.Dto.UserDto;
import com.project.backend.Service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDto> login(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.authenticate(userDto));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        authService.sendPasswordResetCode(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthDto> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) Map<String, String> requestBody) {

        String refreshToken = extractRefreshToken(authorizationHeader, requestBody);
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    private String extractRefreshToken(String authorizationHeader, Map<String, String> requestBody) {
        if (requestBody != null) {
            String refreshToken = requestBody.get("refreshToken");
            if (refreshToken != null && !refreshToken.isBlank()) {
                return refreshToken;
            }

            String token = requestBody.get("token");
            if (token != null && !token.isBlank()) {
                return token;
            }
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }
}
