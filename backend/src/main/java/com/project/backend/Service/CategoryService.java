package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.CategoryCreateDto;
import com.project.backend.Dto.CategoryResponseDto;
import com.project.backend.Dto.CategoryUpdateDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;

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

    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireOwnedCategory(categoryId, user.getId());

        category.setName(dto.getName().trim());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireOwnedCategory(categoryId, user.getId());

        taskRepository.deleteByCategoryIdAndUserId(category.getId(), user.getId());
        categoryRepository.delete(category);
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw ApiError.unauthorized("Authenticated user is required");
        }

        return userDetails.user();
    }

    private CategoryModel requireOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> ApiError.notFound("Category not found"));
    }

    private CategoryResponseDto toResponse(CategoryModel category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }
}
