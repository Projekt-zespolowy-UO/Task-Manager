package com.project.backend.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Model.TaskCategoryModel;
import com.project.backend.Service.TaskCategoryService;

@RestController
@RequestMapping("/api/categories")
public class TaskCategoryController {

    @Autowired
    private TaskCategoryService taskCategoryService;

    @GetMapping
    public List<TaskCategoryModel> getAllCategories() {
        return taskCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskCategoryModel> getCategoryById(@PathVariable Long id) {
        Optional<TaskCategoryModel> category = taskCategoryService.getCategoryById(id);
        return category.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TaskCategoryModel createCategory(@RequestBody TaskCategoryModel category) {
        return taskCategoryService.createCategory(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        taskCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}