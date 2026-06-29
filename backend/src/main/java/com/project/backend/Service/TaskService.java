package com.project.backend.Service;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskImportResultDto;
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
import com.project.backend.Util.CsvReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int CSV_EXPORT_PAGE_SIZE = 500;
    private static final int CSV_IMPORT_MAX_ROWS = 10_000;
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final DashboardRepository dashboardRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final DashboardAuthorizationService dashboardAuthorizationService;

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasks(Long dashboardId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        dashboardAuthorizationService.validateDashboardAccess(user.getId(), dashboardId);

        return taskRepository.findByDashboard_IdOrderByIdAsc(dashboardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] getTasksCsvAsBytes(
            CustomUserDetails userDetails,
            Long dashboardId,
            Long categoryId,
            Long statusId,
            String search) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            writeTasksCsv(byteArrayOutputStream, userDetails, dashboardId, categoryId, statusId, search);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Transactional
    public TaskImportResultDto importTasksFromCsv(
            CustomUserDetails userDetails,
            Long dashboardId,
            byte[] fileContent) {
        UserModel user = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = requireAccessibleDashboard(dashboardId, user.getId());

        if (fileContent == null || fileContent.length == 0) {
            throw ApiError.badRequest("CSV file must not be empty");
        }

        List<List<String>> rows = CsvReader.parse(new String(fileContent, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw ApiError.badRequest("CSV file must not be empty");
        }

        Map<String, Integer> columns = mapColumns(rows.get(0));
        if (!columns.containsKey("title")) {
            throw ApiError.badRequest("CSV header must contain a \"Title\" column");
        }
        if (rows.size() - 1 > CSV_IMPORT_MAX_ROWS) {
            throw ApiError.badRequest("CSV file contains too many rows (max " + CSV_IMPORT_MAX_ROWS + ")");
        }

        Map<String, CategoryModel> categoriesByName = new HashMap<>();
        Map<Long, CategoryModel> categoriesById = new HashMap<>();
        for (CategoryModel category : categoryRepository.findByDashboard_IdOrderByIdAsc(dashboardId)) {
            categoriesById.put(category.getId(), category);
            if (category.getName() != null) {
                categoriesByName.putIfAbsent(category.getName().trim().toLowerCase(Locale.ROOT), category);
            }
        }
        Map<Long, List<TaskStatusModel>> statusesByCategory = new HashMap<>();

        List<TaskModel> toCreate = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            totalRows += 1;

            try {
                toCreate.add(buildTaskFromRow(
                        row, columns, dashboard, user,
                        categoriesById, categoriesByName, statusesByCategory));
            } catch (ApiError e) {
                // Header occupies line 1, so the data row on index i is line i + 1.
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        taskRepository.saveAll(toCreate);

        return new TaskImportResultDto(totalRows, toCreate.size(), errors.size(), errors);
    }

    @Transactional(readOnly = true)
    private void writeTasksCsv(
            OutputStream outputStream,
            CustomUserDetails userDetails,
            Long dashboardId,
            Long categoryId,
            Long statusId,
            String search) throws IOException {
        UserModel user = requireAuthenticatedUser(userDetails);
        if (dashboardId != null) {
            dashboardAuthorizationService.validateDashboardAccess(user.getId(), dashboardId);
        }
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
                    "Start Date",
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
                        dashboardId,
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
        Dashboard dashboard = dto.getDashboardId() != null
                ? requireAccessibleDashboard(dto.getDashboardId(), user.getId())
                : null;

        if (dashboard == null) {
            throw ApiError.badRequest("Dashboard is required for task status management");
        }

        CategoryModel category = requireCategoryForDashboard(
                dto.getCategoryId(),
                dashboard.getId(),
                user.getId());
        TaskStatusModel status = requireStatusForDashboard(
                dto.getStatusId(),
                dashboard.getId(),
                user.getId());

        TaskModel task = new TaskModel();
        validateSchedule(dto.getStartDate(), dto.getDeadline());
        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(status);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        task.setStartDate(dto.getStartDate());
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

        validateSchedule(dto.getStartDate(), dto.getDeadline());
        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(requireStatusForDashboard(dto.getStatusId(), dashboard.getId(), user.getId()));
        task.setPriority(dto.getPriority());
        task.setStartDate(dto.getStartDate());
        task.setDeadline(dto.getDeadline());
        task.setCategory(requireCategoryForDashboard(dto.getCategoryId(), dashboard.getId(), user.getId()));

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
            task.setStatus(requireStatusForDashboard(dto.getStatusId(), dashboard.getId(), user.getId()));
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getStartDate() != null) {
            task.setStartDate(dto.getStartDate());
        }
        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }
        if (dto.getCategoryId() != null) {
            Dashboard dashboard = requireTaskDashboard(task);
            task.setCategory(requireCategoryForDashboard(
                    dto.getCategoryId(),
                    dashboard.getId(),
                    user.getId()));
        }

        validateSchedule(task.getStartDate(), task.getDeadline());
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

        task.setStatus(requireStatusForDashboard(dto.getStatusId(), dashboard.getId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto changeCategory(Long taskId, TaskCategoryUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        Dashboard dashboard = requireTaskDashboard(task);
        task.setCategory(requireCategoryForDashboard(
                dto.getCategoryId(),
                dashboard.getId(),
                user.getId()));

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

    private Dashboard requireAccessibleDashboard(Long dashboardId, Long userId) {
        Dashboard dashboard = dashboardAuthorizationService.getDashboardOrThrow(dashboardId);
        dashboardAuthorizationService.validateDashboardAccess(userId, dashboardId);
        return dashboard;
    }

    private CategoryModel requireCategoryForDashboard(Long categoryId, Long dashboardId, Long userId) {
        dashboardAuthorizationService.validateDashboardAccess(userId, dashboardId);
        return categoryRepository.findByIdAndDashboard_Id(categoryId, dashboardId)
                .orElseThrow(() -> ApiError.notFound("Category not found"));
    }

    private TaskStatusModel requireStatusForDashboard(Long statusId, Long dashboardId, Long userId) {
        dashboardAuthorizationService.validateDashboardAccess(userId, dashboardId);
        TaskStatusModel status = taskStatusRepository.findById(statusId)
                .orElseThrow(() -> ApiError.notFound("Status not found"));

        if (status.getCategory() == null
                || status.getCategory().getDashboard() == null
                || !status.getCategory().getDashboard().getId().equals(dashboardId)) {
            throw ApiError.notFound("Status not found");
        }

        return status;
    }

    private Dashboard requireTaskDashboard(TaskModel task) {
        if (task.getDashboard() == null) {
            throw ApiError.badRequest("Task does not belong to a dashboard");
        }
        return task.getDashboard();
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
                csvValue(task.getStartDate()),
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

    private void validateSchedule(LocalDate startDate, LocalDate deadline) {
        if (startDate != null && deadline != null && startDate.isAfter(deadline)) {
            throw ApiError.badRequest("Task start date must not be after the deadline");
        }
    }

    private Map<String, Integer> mapColumns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i);
            if (name != null) {
                columns.putIfAbsent(name.trim().toLowerCase(Locale.ROOT), i);
            }
        }
        return columns;
    }

    private String cell(List<String> row, Map<String, Integer> columns, String columnName) {
        Integer index = columns.get(columnName);
        if (index == null || index >= row.size()) {
            return null;
        }
        String value = row.get(index);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }

    private TaskModel buildTaskFromRow(
            List<String> row,
            Map<String, Integer> columns,
            Dashboard dashboard,
            UserModel user,
            Map<Long, CategoryModel> categoriesById,
            Map<String, CategoryModel> categoriesByName,
            Map<Long, List<TaskStatusModel>> statusesByCategory) {

        String title = normalizeRequiredTitle(cell(row, columns, "title"));
        if (title.length() > 255) {
            throw ApiError.badRequest("Title must not exceed 255 characters");
        }

        String description = cell(row, columns, "description");
        if (description != null && description.length() > 5000) {
            throw ApiError.badRequest("Description must not exceed 5000 characters");
        }

        LocalDate startDate = parseDate(cell(row, columns, "start date"), "Start Date");
        LocalDate deadline = parseDate(cell(row, columns, "deadline"), "Deadline");
        validateSchedule(startDate, deadline);
        Priority priority = parsePriority(cell(row, columns, "priority"));

        CategoryModel category = resolveCategory(row, columns, categoriesById, categoriesByName);
        TaskStatusModel status = resolveStatus(cell(row, columns, "status"), category, statusesByCategory);

        TaskModel task = new TaskModel();
        task.setTitle(title);
        task.setDescription(description);
        task.setStartDate(startDate);
        task.setDeadline(deadline);
        task.setPriority(priority);
        task.setCategory(category);
        task.setStatus(status);
        task.setUser(user);
        task.setDashboard(dashboard);
        return task;
    }

    private CategoryModel resolveCategory(
            List<String> row,
            Map<String, Integer> columns,
            Map<Long, CategoryModel> categoriesById,
            Map<String, CategoryModel> categoriesByName) {
        String categoryName = cell(row, columns, "category name");
        if (categoryName != null) {
            CategoryModel category = categoriesByName.get(categoryName.toLowerCase(Locale.ROOT));
            if (category == null) {
                throw ApiError.badRequest("Category \"" + categoryName + "\" was not found in this dashboard");
            }
            return category;
        }

        String categoryId = cell(row, columns, "category id");
        if (categoryId != null) {
            Long id = parseLong(categoryId, "Category ID");
            CategoryModel category = categoriesById.get(id);
            if (category == null) {
                throw ApiError.badRequest("Category with id " + id + " was not found in this dashboard");
            }
            return category;
        }

        throw ApiError.badRequest("Category Name or Category ID is required");
    }

    private TaskStatusModel resolveStatus(
            String statusName,
            CategoryModel category,
            Map<Long, List<TaskStatusModel>> statusesByCategory) {
        List<TaskStatusModel> statuses = statusesByCategory.computeIfAbsent(
                category.getId(),
                taskStatusRepository::findByCategory_IdOrderByPositionAsc);

        if (statuses.isEmpty()) {
            throw ApiError.badRequest("Category \"" + category.getName() + "\" has no statuses");
        }

        if (statusName != null) {
            for (TaskStatusModel status : statuses) {
                if (statusName.equalsIgnoreCase(status.getName())) {
                    return status;
                }
            }
        }

        // Fall back to the first status (by position) when none is given or it does not match.
        return statuses.get(0);
    }

    private LocalDate parseDate(String value, String fieldLabel) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiError.badRequest(fieldLabel + " has an invalid date (expected yyyy-MM-dd)");
        }
    }

    private Priority parsePriority(String value) {
        if (value == null) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiError.badRequest("Priority must be one of LOW, MEDIUM, HIGH");
        }
    }

    private Long parseLong(String value, String fieldLabel) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw ApiError.badRequest(fieldLabel + " must be a number");
        }
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
                task.getStartDate(),
                task.getDeadline(),
                category != null ? category.getId() : null,
                task.getDashboard() != null ? task.getDashboard().getId() : null,
                task.getCustomStatus() != null ? task.getCustomStatus().getId() : null);
    }
}
