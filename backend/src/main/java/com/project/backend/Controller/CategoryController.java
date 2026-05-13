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

import com.project.backend.Dto.CategoryCreateDto;
import com.project.backend.Dto.CategoryResponseDto;
import com.project.backend.Dto.CategoryUpdateDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "CRUD operations for user categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories for the authenticated user")
    public List<CategoryResponseDto> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryService.getCategories(userDetails);
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public CategoryResponseDto createCategory(
            @Valid @RequestBody CategoryCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryService.createCategory(dto, userDetails);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category name")
    public CategoryResponseDto updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryService.updateCategory(id, dto, userDetails);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category and all of its tasks")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        categoryService.deleteCategory(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
