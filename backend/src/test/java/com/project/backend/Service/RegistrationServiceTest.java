package com.project.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerNormalizesUserAndReturnsJwt() {
        UserDto request = userDto("  Alice  ", " Alice@Example.COM ", "password123");
        JwtAuthDto tokens = tokens("access-token", "refresh-token");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateToken("alice@example.com")).thenReturn(tokens);

        JwtAuthDto result = registrationService.register(request);

        ArgumentCaptor<UserModel> savedUser = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(savedUser.capture());

        assertThat(result).isSameAs(tokens);
        assertThat(savedUser.getValue().getUserName()).isEqualTo("Alice");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        UserDto request = userDto("Alice", "alice@example.com", "password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any(UserModel.class));
        verify(jwtService, never()).generateToken("alice@example.com");
    }

    @Test
    void registerRejectsShortPassword() {
        UserDto request = userDto("Alice", "alice@example.com", "123");

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).existsByEmail("alice@example.com");
    }

    private UserDto userDto(String username, String email, String password) {
        UserDto dto = new UserDto();
        dto.setUserName(username);
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private JwtAuthDto tokens(String token, String refreshToken) {
        JwtAuthDto dto = new JwtAuthDto();
        dto.setToken(token);
        dto.setRefreshToken(refreshToken);
        return dto;
    }
}
