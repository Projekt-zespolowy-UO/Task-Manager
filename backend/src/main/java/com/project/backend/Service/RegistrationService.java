package com.project.backend.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.UserDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.UserRepository;
import com.project.backend.Security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public JwtAuthDto register(UserDto userDto) {
        validateRegistrationData(userDto);

        String normalizedEmail = userDto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ApiError.conflict("User with this email already exists");
        }

        UserModel user = new UserModel();
        user.setUserName(userDto.getUserName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);
        return jwtService.generateToken(user.getEmail());
    }

    private void validateRegistrationData(UserDto userDto) {
        if (userDto == null) {
            throw ApiError.badRequest("Request body is required");
        }

        if (userDto.getUserName() == null || userDto.getUserName().isBlank()) {
            throw ApiError.badRequest("Username is required");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw ApiError.badRequest("Email is required");
        }

        if (!userDto.getEmail().contains("@")) {
            throw ApiError.badRequest("Email format is invalid");
        }

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw ApiError.badRequest("Password is required");
        }

        if (userDto.getPassword().length() < 6) {
            throw ApiError.badRequest("Password must contain at least 6 characters");
        }
    }
}
