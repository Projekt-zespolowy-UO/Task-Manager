package com.project.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.UserDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.UserRepository;
import com.project.backend.Security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordResetCodeService passwordResetCodeService;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticateNormalizesEmailAndReturnsJwt() {
        UserDto request = userDto(" User@Example.COM ", "password");
        UserModel user = user(1L, "user@example.com", "encoded-password");
        JwtAuthDto tokens = tokens("access-token", "refresh-token");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("user@example.com")).thenReturn(tokens);

        JwtAuthDto result = authService.authenticate(request);

        assertThat(result).isSameAs(tokens);
        verify(userRepository).findByEmail("user@example.com");
        verify(jwtService).generateToken("user@example.com");
    }

    @Test
    void authenticateRejectsWrongPassword() {
        UserDto request = userDto("user@example.com", "wrong-password");
        UserModel user = user(1L, "user@example.com", "encoded-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(jwtService, never()).generateToken("user@example.com");
    }

    @Test
    void refreshTokenRejectsBlankToken() {
        assertThatThrownBy(() -> authService.refreshToken(" "))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(jwtService, never()).getEmailFromValidToken(" ");
    }

    private UserDto userDto(String email, String password) {
        UserDto dto = new UserDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private UserModel user(Long id, String email, String password) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setUserName("user");
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }

    private JwtAuthDto tokens(String token, String refreshToken) {
        JwtAuthDto dto = new JwtAuthDto();
        dto.setToken(token);
        dto.setRefreshToken(refreshToken);
        return dto;
    }
}
