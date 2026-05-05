package com.project.backend.Controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.CategoryCreateDto;
import com.project.backend.Dto.CategoryResponseDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponseDto> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryService.getCategories(userDetails);
    }

    @PostMapping
    public CategoryResponseDto createCategory(
            @Valid @RequestBody CategoryCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryService.createCategory(dto, userDetails);
    }
}
