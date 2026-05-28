package com.project.backend.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.CustomStatusCreateDto;
import com.project.backend.Dto.CustomStatusResponseDto;
import com.project.backend.Dto.CustomStatusUpdateDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.CustomStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/statuses")
@RequiredArgsConstructor
@Tag(name = "Custom statuses", description = "User-defined task status panels")
@SecurityRequirement(name = "bearerAuth")
public class CustomStatusController {

    private final CustomStatusService customStatusService;

    @GetMapping
    @Operation(summary = "List custom statuses for the authenticated user")
    public List<CustomStatusResponseDto> getStatuses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return customStatusService.getStatuses(userDetails);
    }

    @PostMapping
    @Operation(summary = "Create a new custom status")
    public CustomStatusResponseDto createStatus(
            @Valid @RequestBody CustomStatusCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return customStatusService.createStatus(dto, userDetails);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a custom status (rename and/or reorder)")
    public CustomStatusResponseDto updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomStatusUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return customStatusService.updateStatus(id, dto, userDetails);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a custom status (tasks pointing to it become uncategorized)")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        customStatusService.deleteStatus(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
