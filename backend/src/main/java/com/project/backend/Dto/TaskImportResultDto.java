package com.project.backend.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskImportResultDto {
    private int totalRows;
    private int imported;
    private int skipped;
    private List<String> errors;
}
