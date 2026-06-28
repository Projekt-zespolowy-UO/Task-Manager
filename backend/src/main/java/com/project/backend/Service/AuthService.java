package com.project.backend.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.PasswordResetConfirmDto;
import com.project.backend.Dto.PasswordResetRequestDto;
import com.project.backend.Dto.UserDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.UserRepository;
import com.project.backend.Security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final PasswordResetCodeService passwordResetCodeService;

    @Transactional(readOnly = true)
    public JwtAuthDto authenticate(UserDto userDto) {
        validateCredentials(userDto);

        UserModel user = userRepository.findByEmail(userDto.getEmail().trim().toLowerCase())
                .orElseThrow(() -> ApiError.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            throw ApiError.unauthorized("Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail());
    }

    @Transactional(readOnly = true)
    public JwtAuthDto refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiError.badRequest("Refresh token is required");
        }

        String email = jwtService.getEmailFromValidToken(refreshToken)
                .orElseThrow(() -> ApiError.unauthorized("Refresh token is invalid or expired"));
        userRepository.findByEmail(email)
                .orElseThrow(() -> ApiError.unauthorized("User associated with token was not found"));

        return jwtService.refreshBaseToken(email, refreshToken);
    }

    public void sendPasswordResetCode(PasswordResetRequestDto request) {
        validateEmail(request.getEmail());

        UserModel user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> ApiError.notFound("User with this email not found"));

        String code = passwordResetCodeService.createResetCode(user);
        String emailText = "Your password reset code: " + code
                + "\nThis code is valid for 15 minutes.";
        mailService.sendEmail(user.getEmail(), "Password Reset", emailText);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmDto request) {
        validatePasswordResetRequest(request);

        UserModel user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> ApiError.notFound("User with this email not found"));

        if (!passwordResetCodeService.consumeResetCode(user, request.getCode())) {
            throw ApiError.badRequest("Reset code is invalid or expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    

    private void validatePasswordResetRequest(PasswordResetConfirmDto request) {
        if (request == null) {
            throw ApiError.badRequest("Request body is required");
        }

        validateEmail(request.getEmail());

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw ApiError.badRequest("Reset code is required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw ApiError.badRequest("New password is required");
        }

        if (request.getNewPassword().length() < 6) {
            throw ApiError.badRequest("Password must contain at least 6 characters");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw ApiError.badRequest("Valid email is required");
        }
    }

    private void validateCredentials(UserDto userDto) {
        if (userDto == null) {
            throw ApiError.badRequest("Request body is required");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw ApiError.badRequest("Email is required");
        }

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw ApiError.badRequest("Password is required");
        }
    }
}
