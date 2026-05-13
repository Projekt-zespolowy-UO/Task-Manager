package com.project.backend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCategoryUpdateDto {

    @NotNull
    private Long categoryId;
}
