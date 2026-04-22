package com.project.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.UserDto;
import com.project.backend.Dto.UserSettingsResponseDto;
import com.project.backend.Dto.UserSettingsUpdateDto;
import com.project.backend.Dto.UserSettingsUpdateResponseDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/userdata")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUserMe(userDetails));
    }

    @GetMapping("/settings")
    public ResponseEntity<UserSettingsResponseDto> getSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUserSettings(userDetails));
    }

    @PutMapping("/settings")
    public ResponseEntity<UserSettingsUpdateResponseDto> updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserSettingsUpdateDto request) {
        return ResponseEntity.ok(userService.updateCurrentUserSettings(userDetails, request));
    }

    
}
