package com.project.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.backend.Model.TaskCategoryModel;
import com.project.backend.Repository.TaskCategoryRepository;

@Service
public class TaskCategoryService {

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    public List<TaskCategoryModel> getAllCategories() {
        return taskCategoryRepository.findAll();
    }

    public Optional<TaskCategoryModel> getCategoryById(Long id) {
        return taskCategoryRepository.findById(id);
    }

    public TaskCategoryModel createCategory(TaskCategoryModel category) {
        return taskCategoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        taskCategoryRepository.deleteById(id);
    }
}