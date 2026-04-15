package com.project.backend.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.PasswordResetConfirmDto;
import com.project.backend.Dto.PasswordResetRequestDto;
import com.project.backend.Dto.UserDto;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.UserRepository;
import com.project.backend.Security.JwtService;
import com.project.backend.Service.MailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    @Value("${jwt.secret}")
    private String passwordResetSecret;

    @Transactional(readOnly = true)
    public JwtAuthDto authenticate(UserDto userDto) {
        validateCredentials(userDto);

        UserModel user = userRepository.findByEmail(userDto.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        if (!passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail());
    }

    @Transactional(readOnly = true)
    public JwtAuthDto refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        if (!jwtService.validateToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }

        String email = jwtService.getEmailFromToken(refreshToken);
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User associated with token was not found"));

        return jwtService.refreshBaseToken(email, refreshToken);
    }

    @Transactional
    public void sendPasswordResetCode(PasswordResetRequestDto request) {
        validateEmail(request.getEmail());

        UserModel user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with this email not found"));

        String code = generateResetCodeForEmail(user.getEmail(), Instant.now());
        String emailText = "Ваш код для скидання пароля: " + code
                + "\nТермін дії коду: 15 хвилин.";
        mailService.sendEmail(user.getEmail(), "Скидання пароля", emailText);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmDto request) {
        validatePasswordResetRequest(request);

        UserModel user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with this email not found"));

        if (!isResetCodeValid(user.getEmail(), request.getCode(), Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code is invalid or expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String generateResetCodeForEmail(String email, Instant now) {
        long window = now.getEpochSecond() / 300;
        return formatCode(buildCode(email, window));
    }

    private boolean isResetCodeValid(String email, String code, Instant now) {
        for (int i = 0; i < 3; i++) {
            long window = now.minus(i * 5, ChronoUnit.MINUTES).getEpochSecond() / 300;
            if (formatCode(buildCode(email, window)).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private byte[] buildCode(String email, long window) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(passwordResetSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal((email + ":" + window).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate reset code", e);
        }
    }

    private String formatCode(byte[] hash) {
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int code = Math.abs(binary % 1_000_000);
        return String.format("%06d", code);
    }

    private void validatePasswordResetRequest(PasswordResetConfirmDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        validateEmail(request.getEmail());

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code is required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }

        if (request.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 6 characters");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
        }
    }

    private void validateCredentials(UserDto userDto) {
        if (userDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
    }
}
