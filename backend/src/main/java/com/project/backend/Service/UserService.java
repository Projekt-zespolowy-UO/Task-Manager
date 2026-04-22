package com.project.backend.Service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.UserSettingsResponseDto;
import com.project.backend.Dto.UserSettingsUpdateDto;
import com.project.backend.Dto.UserSettingsUpdateResponseDto;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.UserRepository;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public UserSettingsResponseDto getCurrentUserSettings(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        return new UserSettingsResponseDto(user.getUserName(), user.getEmail());
    }

    public UserSettingsUpdateResponseDto updateCurrentUserSettings(
            CustomUserDetails userDetails,
            UserSettingsUpdateDto request) {

        UserModel user = requireAuthenticatedUser(userDetails);

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        }

        user.setUserName(request.getUserName().trim());
        user.setEmail(request.getEmail().trim());
        userRepository.save(user);

        JwtAuthDto tokens = jwtService.generateToken(user.getEmail());
        return new UserSettingsUpdateResponseDto(
                user.getUserName(),
                user.getEmail(),
                tokens.getToken(),
                tokens.getRefreshToken());
    }

    
    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userRepository.findById(userDetails.user().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    
}
