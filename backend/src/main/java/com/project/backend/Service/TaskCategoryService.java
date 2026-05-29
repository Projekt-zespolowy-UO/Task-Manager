package com.project.backend.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.backend.Dto.TaskCategoryDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Mapper.TaskCategoryMapper;
import com.project.backend.Model.TaskCategoryModel;
import com.project.backend.Repository.TaskCategoryRepository;

@Service
public class TaskCategoryService {

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    @Autowired
    private TaskCategoryMapper taskCategoryMapper;

    public List<TaskCategoryDto> getAllCategories() {
        return taskCategoryRepository.findAll()
                .stream()
                .map(taskCategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public TaskCategoryDto getCategoryById(Long id) {
        return taskCategoryRepository.findById(id)
                .map(taskCategoryMapper::toDto)
                .orElseThrow(() -> ApiError.notFound("Category not found"));
    }

    public TaskCategoryDto createCategory(TaskCategoryDto categoryDto) {
        TaskCategoryModel categoryModel = taskCategoryMapper.toModel(categoryDto);
        TaskCategoryModel savedCategory = taskCategoryRepository.save(categoryModel);
        return taskCategoryMapper.toDto(savedCategory);
    }

    public void deleteCategory(Long id) {
        if (!taskCategoryRepository.existsById(id)) {
            throw ApiError.notFound("Category not found");
        }
        taskCategoryRepository.deleteById(id);
    }
}
