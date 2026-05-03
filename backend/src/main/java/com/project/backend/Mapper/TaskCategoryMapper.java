package com.project.backend.Mapper;

import org.springframework.stereotype.Component;

import com.project.backend.Dto.TaskCategoryDto;
import com.project.backend.Model.TaskCategoryModel;

@Component
public class TaskCategoryMapper {

    public TaskCategoryDto toDto(TaskCategoryModel model) {
        TaskCategoryDto dto = new TaskCategoryDto();
        dto.setId(model.getId());
        dto.setName(model.getName());
        return dto;
    }

    public TaskCategoryModel toModel(TaskCategoryDto dto) {
        TaskCategoryModel model = new TaskCategoryModel();
        model.setId(dto.getId());
        model.setName(dto.getName());
        return model;
    }
}