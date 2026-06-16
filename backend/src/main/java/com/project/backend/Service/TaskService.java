package com.project.backend.Service;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskPatchDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Dto.TaskStatusUpdateDto;
import com.project.backend.Dto.TaskUpdateDto;
import com.project.backend.Enum.Priority;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.TaskStatusModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Repository.TaskStatusRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int CSV_EXPORT_PAGE_SIZE = 500;
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final DashboardRepository dashboardRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final DashboardAuthorizationService dashboardAuthorizationService;

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasks(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        // Get all tasks from dashboards user is a member of
        return taskRepository.findAccessibleTasks(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] getTasksCsvAsBytes(
            CustomUserDetails userDetails,
            Long categoryId,
            Long statusId,
            String search) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            writeTasksCsv(byteArrayOutputStream, userDetails, categoryId, statusId, search);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Transactional(readOnly = true)
    private void writeTasksCsv(
            OutputStream outputStream,
            CustomUserDetails userDetails,
            Long categoryId,
            Long statusId,
            String search) throws IOException {
        UserModel user = requireAuthenticatedUser(userDetails);
        String normalizedSearch = normalizeSearch(search);

        outputStream.write(UTF8_BOM);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writeCsvRow(writer, List.of(
                    "ID",
                    "Title",
                    "Description",
                    "Status",
                    "Priority",
                    "Deadline",
                    "Category ID",
                    "Category Name",
                    "Dashboard ID"));

            int pageNumber = 0;
            Slice<TaskModel> taskPage;

            do {
                Pageable pageable = PageRequest.of(pageNumber, CSV_EXPORT_PAGE_SIZE);
                taskPage = taskRepository.findAccessibleTasksForCsvExport(
                        user.getId(),
                        categoryId,
                        statusId,
                        normalizedSearch,
                        pageable);

                for (TaskModel task : taskPage.getContent()) {
                    writeCsvRow(writer, toCsvRow(task));
                }

                writer.flush();
                pageNumber += 1;
            } while (taskPage.hasNext());
        }
    }

    @Transactional
    public TaskResponseDto createTask(TaskCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireOwnedCategory(dto.getCategoryId(), user.getId());

        Dashboard dashboard = dto.getDashboardId() != null
                ? requireOwnedDashboard(dto.getDashboardId(), user.getId())
                : null;

        if (dashboard == null) {
            throw ApiError.badRequest("Dashboard is required for task status management");
        }

        TaskStatusModel status = requireOwnedStatusForDashboard(dto.getStatusId(), user.getId());

        TaskModel task = new TaskModel();
        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(status);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        task.setDeadline(dto.getDeadline());
        task.setCategory(category);
        task.setUser(user);
        task.setDashboard(dashboard);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto updateTask(Long taskId, TaskUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        Dashboard dashboard = task.getDashboard();
        if (dashboard == null) {
            throw ApiError.badRequest("Task does not belong to a dashboard");
        }

        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(requireOwnedStatusForDashboard(dto.getStatusId(), user.getId()));
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto patchTask(Long taskId, TaskPatchDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        if (dto.getTitle() != null) {
            task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatusId() != null) {
            Dashboard dashboard = task.getDashboard();
            if (dashboard == null) {
                throw ApiError.badRequest("Task does not belong to a dashboard");
            }
            task.setStatus(requireOwnedStatusForDashboard(dto.getStatusId(), user.getId()));
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }
        if (dto.getCategoryId() != null) {
            task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto changeStatus(Long taskId, TaskStatusUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        Dashboard dashboard = task.getDashboard();
        if (dashboard == null) {
            throw ApiError.badRequest("Task does not belong to a dashboard");
        }

        task.setStatus(requireOwnedStatusForDashboard(dto.getStatusId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto changeCategory(Long taskId, TaskCategoryUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());
        taskRepository.delete(task);
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw ApiError.unauthorized("Authenticated user is required");
        }

        return userDetails.user();
    }

    private TaskModel requireOwnedTask(Long taskId, Long userId) {
        TaskModel task = taskRepository.findById(taskId)
                .orElseThrow(() -> ApiError.notFound("Task not found"));
        
        // Verify user is a member of the task's dashboard
        if (task.getDashboard() == null) {
            throw ApiError.badRequest("Task does not belong to a dashboard");
        }
        
        dashboardAuthorizationService.validateDashboardAccess(userId, task.getDashboard().getId());
        return task;
    }

    private CategoryModel requireOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> ApiError.notFound("Category not found"));
    }

    private Dashboard requireOwnedDashboard(Long dashboardId, Long userId) {
        Dashboard dashboard = dashboardAuthorizationService.getDashboardOrThrow(dashboardId);
        // Verify user is a member of the dashboard (can be MEMBER or OWNER)
        dashboardAuthorizationService.validateDashboardAccess(userId, dashboardId);
        return dashboard;
    }

    private TaskStatusModel requireOwnedStatusForDashboard(Long statusId, Long userId) {
        TaskStatusModel status = taskStatusRepository.findById(statusId)
                .orElseThrow(() -> ApiError.notFound("Status not found"));

        if (status.getCategory() == null
                || status.getCategory().getUser() == null
                || !status.getCategory().getUser().getId().equals(userId)) {
            throw ApiError.notFound("Status not found");
        }

        return status;
    }

    private String normalizeRequiredTitle(String title) {
        String trimmed = title == null ? null : title.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw ApiError.badRequest("Title must not be blank");
        }
        return trimmed;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }

        String trimmed = search.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > 255) {
            throw ApiError.badRequest("Search query is too long");
        }
        return trimmed;
    }

    private List<String> toCsvRow(TaskModel task) {
        CategoryModel category = task.getCategory();
        TaskStatusModel status = task.getStatus();
        Dashboard dashboard = task.getDashboard();

        return List.of(
                csvValue(task.getId()),
                csvValue(task.getTitle()),
                csvValue(task.getDescription()),
                csvValue(status != null ? status.getName() : null),
                csvValue(task.getPriority()),
                csvValue(task.getDeadline()),
                csvValue(category != null ? category.getId() : null),
                csvValue(category != null ? category.getName() : null),
                csvValue(dashboard != null ? dashboard.getId() : null));
    }

    private String csvValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i += 1) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsvValue(values.get(i)));
        }
        writer.write("\r\n");
    }

    private String escapeCsvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private TaskResponseDto toResponse(TaskModel task) {
        CategoryModel category = task.getCategory();

        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus() != null ? task.getStatus().getId() : null,
                task.getStatus() != null ? task.getStatus().getName() : null,
                task.getPriority(),
                task.getDeadline(),
                category != null ? category.getId() : null,
                task.getDashboard() != null ? task.getDashboard().getId() : null);
    }
}