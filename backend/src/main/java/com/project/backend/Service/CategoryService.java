package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.CategoryCreateDto;
import com.project.backend.Dto.CategoryResponseDto;
import com.project.backend.Dto.CategoryUpdateDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.Dashboard;
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
    private final DashboardAuthorizationService dashboardAuthorizationService;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategories(Long dashboardId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        dashboardAuthorizationService.validateDashboardAccess(user.getId(), dashboardId);

        return categoryRepository.findByDashboard_IdOrderByIdAsc(dashboardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponseDto createCategory(CategoryCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = dashboardAuthorizationService.getDashboardOrThrow(dto.getDashboardId());
        dashboardAuthorizationService.validateDashboardAccess(user.getId(), dashboard.getId());

        CategoryModel category = new CategoryModel();
        category.setName(dto.getName().trim());
        category.setUser(user);
        category.setDashboard(dashboard);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireAccessibleCategory(categoryId, user.getId());

        category.setName(dto.getName().trim());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireAccessibleCategory(categoryId, user.getId());

        taskRepository.deleteByCategoryIdAndDashboardId(
                category.getId(),
                category.getDashboard().getId());
        categoryRepository.delete(category);
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw ApiError.unauthorized("Authenticated user is required");
        }

        return userDetails.user();
    }

    private CategoryModel requireAccessibleCategory(Long categoryId, Long userId) {
        CategoryModel category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiError.notFound("Category not found"));
        dashboardAuthorizationService.validateDashboardAccess(userId, category.getDashboard().getId());
        return category;
    }

    private CategoryResponseDto toResponse(CategoryModel category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDashboard().getId());
    }
}
