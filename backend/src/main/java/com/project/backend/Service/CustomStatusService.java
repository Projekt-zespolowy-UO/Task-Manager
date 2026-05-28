package com.project.backend.Service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.CustomStatusCreateDto;
import com.project.backend.Dto.CustomStatusResponseDto;
import com.project.backend.Dto.CustomStatusUpdateDto;
import com.project.backend.Model.CustomStatusModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CustomStatusRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomStatusService {

    private final CustomStatusRepository customStatusRepository;

    @Transactional(readOnly = true)
    public List<CustomStatusResponseDto> getStatuses(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        return customStatusRepository.findByUser_IdOrderByPositionAscIdAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomStatusResponseDto createStatus(CustomStatusCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        String name = normalizeName(dto.getName());

        if (customStatusRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status with this name already exists");
        }

        CustomStatusModel status = new CustomStatusModel();
        status.setName(name);
        status.setPosition(dto.getPosition() != null ? dto.getPosition() : nextPosition(user.getId()));
        status.setUser(user);

        try {
            return toResponse(customStatusRepository.save(status));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status with this name already exists");
        }
    }

    @Transactional
    public CustomStatusResponseDto updateStatus(Long statusId, CustomStatusUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CustomStatusModel status = requireOwned(statusId, user.getId());

        if (dto.getName() != null) {
            String name = normalizeName(dto.getName());
            if (customStatusRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(user.getId(), name, statusId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Status with this name already exists");
            }
            status.setName(name);
        }
        if (dto.getPosition() != null) {
            status.setPosition(dto.getPosition());
        }

        try {
            return toResponse(customStatusRepository.save(status));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status with this name already exists");
        }
    }

    @Transactional
    public void deleteStatus(Long statusId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CustomStatusModel status = requireOwned(statusId, user.getId());
        customStatusRepository.delete(status);
    }

    public CustomStatusModel requireOwned(Long statusId, Long userId) {
        return customStatusRepository.findByIdAndUser_Id(statusId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom status not found"));
    }

    private int nextPosition(Long userId) {
        return customStatusRepository.findByUser_IdOrderByPositionAscIdAsc(userId)
                .stream()
                .map(CustomStatusModel::getPosition)
                .filter(p -> p != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }
        return trimmed;
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        return userDetails.user();
    }

    private CustomStatusResponseDto toResponse(CustomStatusModel status) {
        return new CustomStatusResponseDto(status.getId(), status.getName(), status.getPosition());
    }
}
