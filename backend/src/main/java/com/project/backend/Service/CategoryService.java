package com.project.backend.Service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.CategoryCreateDto;
import com.project.backend.Dto.CategoryResponseDto;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategories(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        return categoryRepository.findByUser_IdOrderByIdAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponseDto createCategory(CategoryCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        CategoryModel category = new CategoryModel();
        category.setName(dto.getName().trim());
        category.setUser(user);

        return toResponse(categoryRepository.save(category));
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userDetails.user();
    }

    private CategoryResponseDto toResponse(CategoryModel category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }
}
