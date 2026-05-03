package com.project.backend.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Model.PasswordResetCodeModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.PasswordResetCodeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_CODES_PER_WINDOW = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetCodeRepository passwordResetCodeRepository;

    @Value("${password-reset.secret}")
    private String passwordResetSecret;

    @Transactional
    public String createResetCode(UserModel user) {
        Instant now = Instant.now();
        long recentCodes = passwordResetCodeRepository.countByUserAndCreatedAtAfter(
                user,
                now.minus(RATE_LIMIT_WINDOW));

        if (recentCodes >= MAX_CODES_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset requests");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        passwordResetCodeRepository.deleteByUserAndUsedAtIsNull(user);

        PasswordResetCodeModel resetCode = new PasswordResetCodeModel();
        resetCode.setUser(user);
        resetCode.setCodeHash(hashCode(user, code));
        resetCode.setExpiresAt(now.plus(CODE_TTL));
        resetCode.setCreatedAt(now);
        passwordResetCodeRepository.save(resetCode);

        return code;
    }

    @Transactional
    public boolean consumeResetCode(UserModel user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        return passwordResetCodeRepository
                .findTopByUserAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now())
                .filter(resetCode -> resetCode.getCodeHash().equals(hashCode(user, code.trim())))
                .map(resetCode -> {
                    resetCode.setUsedAt(Instant.now());
                    return true;
                })
                .orElse(false);
    }

    private String hashCode(UserModel user, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    passwordResetSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal((user.getId() + ":" + user.getEmail() + ":" + code)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash reset code", e);
        }
    }
}
