package com.project.backend.Service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.JwtAuthDto;
import com.project.backend.Dto.UserDto;
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

    @Transactional(readOnly = true)
    public UserDto getCurrentUserMe(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUserName(user.getUserName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    @Transactional
    public UserSettingsUpdateResponseDto updateCurrentUserSettings(
            CustomUserDetails userDetails,
            UserSettingsUpdateDto request) {

        UserModel user = requirePersistedAuthenticatedUser(userDetails);
        validateUpdateRequest(request);
        String updatedUserName = resolveUpdatedUserName(user, request);
        String updatedEmail = resolveUpdatedEmail(user, request);

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            validateCurrentPassword(user, request.getCurrentPassword());
            user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        }

        user.setUserName(updatedUserName);
        user.setEmail(updatedEmail);
        userRepository.save(user);

        JwtAuthDto tokens = jwtService.generateToken(user.getEmail());
        return new UserSettingsUpdateResponseDto(
                user.getUserName(),
                user.getEmail(),
                tokens.getToken(),
                tokens.getRefreshToken());
    }

    private void validateUpdateRequest(UserSettingsUpdateDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        boolean wantsUserNameChange = request.getUserName() != null;
        boolean wantsEmailChange = request.getEmail() != null;
        boolean wantsPasswordChange = request.getNewPassword() != null && !request.getNewPassword().isBlank();

        if (!wantsUserNameChange && !wantsEmailChange && !wantsPasswordChange) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No settings changes were provided");
        }

        if (wantsUserNameChange && request.getUserName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be blank");
        }

        if (wantsEmailChange) {
            if (request.getEmail().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be blank");
            }

            if (!request.getEmail().contains("@")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email format is invalid");
            }
        }

        if (wantsPasswordChange) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Current password is required to change password");
            }

            if (request.getNewPassword().trim().length() < 6) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Password must contain at least 6 characters");
            }
        }
    }

    private String resolveUpdatedUserName(UserModel user, UserSettingsUpdateDto request) {
        if (request.getUserName() == null) {
            return user.getUserName();
        }

        return request.getUserName().trim();
    }

    private String resolveUpdatedEmail(UserModel user, UserSettingsUpdateDto request) {
        if (request.getEmail() == null) {
            return user.getEmail();
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        return normalizedEmail;
    }

    private void validateCurrentPassword(UserModel user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
    }

    
    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userDetails.user();
    }

    private UserModel requirePersistedAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userRepository.findById(userDetails.user().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    
}
