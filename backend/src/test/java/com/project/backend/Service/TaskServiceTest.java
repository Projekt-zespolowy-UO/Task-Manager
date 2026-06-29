package com.project.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;

import com.project.backend.Dto.TaskImportResultDto;
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

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private TaskStatusRepository taskStatusRepository;

    @Mock
    private DashboardAuthorizationService dashboardAuthorizationService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getTasksCsvAsBytesExportsAccessibleTasksAndEscapesValues() {
        UserModel user = user(7L);
        TaskModel task = task(100L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        when(taskRepository.findAccessibleTasksForCsvExport(
                eq(7L),
                eq(10L),
                eq(20L),
                eq(30L),
                eq("report"),
                pageable.capture()))
                .thenReturn(new SliceImpl<>(List.of(task), PageRequest.of(0, 500), false));

        byte[] bytes = taskService.getTasksCsvAsBytes(
                new CustomUserDetails(user),
                10L,
                20L,
                30L,
                "  report  ");

        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFF\"ID\",\"Title\",\"Description\"");
        assertThat(csv).contains("\"Write \"\"tests\"\"\"");
        assertThat(csv).contains("\"Backlog \"\"A\"\"\"");
        assertThat(csv).contains("\"100\",\"Write \"\"tests\"\"\",\"Line one");
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(500);
        verify(dashboardAuthorizationService).validateDashboardAccess(7L, 10L);
    }

    @Test
    void getTasksCsvAsBytesRejectsUnauthenticatedUser() {
        assertThatThrownBy(() -> taskService.getTasksCsvAsBytes(null, null, null, null, null))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(taskRepository, never()).findAccessibleTasksForCsvExport(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void getTasksCsvAsBytesRejectsTooLongSearch() {
        UserModel user = user(7L);
        String longSearch = "a".repeat(256);

        assertThatThrownBy(() -> taskService.getTasksCsvAsBytes(
                new CustomUserDetails(user),
                null,
                null,
                null,
                longSearch))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(taskRepository, never()).findAccessibleTasksForCsvExport(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void importTasksFromCsvCreatesTasksAndCollectsRowErrors() {
        UserModel user = user(7L);

        Dashboard dashboard = new Dashboard();
        dashboard.setId(10L);

        CategoryModel category = new CategoryModel();
        category.setId(20L);
        category.setName("Backlog");
        category.setDashboard(dashboard);

        TaskStatusModel toDo = status(30L, "To Do", 0, category);
        TaskStatusModel inProgress = status(31L, "In Progress", 1, category);

        when(dashboardAuthorizationService.getDashboardOrThrow(10L)).thenReturn(dashboard);
        when(categoryRepository.findByDashboard_IdOrderByIdAsc(10L)).thenReturn(List.of(category));
        when(taskStatusRepository.findByCategory_IdOrderByPositionAsc(20L))
                .thenReturn(List.of(toDo, inProgress));

        byte[] csv = csv(
                q("ID", "Title", "Description", "Status", "Priority",
                        "Start Date", "Deadline", "Category ID", "Category Name", "Dashboard ID"),
                q("1", "Task A", "Desc A", "In Progress", "HIGH",
                        "2026-06-01", "2026-06-20", "20", "Backlog", "10"),
                q("2", "Task B", "", "Unknown Status", "",
                        "", "", "", "Backlog", "10"),
                q("3", "Task C", "", "To Do", "LOW",
                        "", "", "", "Missing", "10"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskModel>> saved = ArgumentCaptor.forClass(List.class);

        TaskImportResultDto result = taskService.importTasksFromCsv(new CustomUserDetails(user), 10L, csv);

        assertThat(result.getTotalRows()).isEqualTo(3);
        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Row 4").contains("Missing");

        verify(dashboardAuthorizationService).validateDashboardAccess(7L, 10L);
        verify(taskRepository).saveAll(saved.capture());

        List<TaskModel> tasks = saved.getValue();
        assertThat(tasks).hasSize(2);

        TaskModel taskA = tasks.get(0);
        assertThat(taskA.getTitle()).isEqualTo("Task A");
        assertThat(taskA.getDescription()).isEqualTo("Desc A");
        assertThat(taskA.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(taskA.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(taskA.getDeadline()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(taskA.getCategory()).isSameAs(category);
        assertThat(taskA.getStatus()).isSameAs(inProgress);
        assertThat(taskA.getUser()).isSameAs(user);
        assertThat(taskA.getDashboard()).isSameAs(dashboard);

        TaskModel taskB = tasks.get(1);
        assertThat(taskB.getTitle()).isEqualTo("Task B");
        assertThat(taskB.getPriority()).isEqualTo(Priority.MEDIUM);
        // Unmatched status name falls back to the first status by position.
        assertThat(taskB.getStatus()).isSameAs(toDo);
    }

    @Test
    void importTasksFromCsvRejectsUnauthenticatedUser() {
        assertThatThrownBy(() -> taskService.importTasksFromCsv(null, 10L, new byte[] {1}))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(taskRepository, never()).saveAll(any());
    }

    @Test
    void importTasksFromCsvRejectsEmptyFile() {
        UserModel user = user(7L);
        Dashboard dashboard = new Dashboard();
        dashboard.setId(10L);
        when(dashboardAuthorizationService.getDashboardOrThrow(10L)).thenReturn(dashboard);

        assertThatThrownBy(() -> taskService.importTasksFromCsv(new CustomUserDetails(user), 10L, new byte[0]))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(taskRepository, never()).saveAll(any());
    }

    @Test
    void importTasksFromCsvRejectsHeaderWithoutTitle() {
        UserModel user = user(7L);
        Dashboard dashboard = new Dashboard();
        dashboard.setId(10L);
        when(dashboardAuthorizationService.getDashboardOrThrow(10L)).thenReturn(dashboard);

        byte[] csv = csv(q("ID", "Description", "Status"));

        assertThatThrownBy(() -> taskService.importTasksFromCsv(new CustomUserDetails(user), 10L, csv))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(taskRepository, never()).saveAll(any());
    }

    private TaskStatusModel status(Long id, String name, int position, CategoryModel category) {
        TaskStatusModel status = new TaskStatusModel();
        status.setId(id);
        status.setName(name);
        status.setPosition(position);
        status.setCategory(category);
        return status;
    }

    private byte[] csv(String... lines) {
        String content = "\uFEFF" + String.join("\r\n", lines) + "\r\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private String q(String... fields) {
        return Arrays.stream(fields)
                .map(field -> "\"" + field.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setUserName("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setPassword("password");
        return user;
    }

    private TaskModel task(Long id) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(10L);

        CategoryModel category = new CategoryModel();
        category.setId(20L);
        category.setName("Backlog \"A\"");
        category.setDashboard(dashboard);

        TaskStatusModel status = new TaskStatusModel();
        status.setId(30L);
        status.setName("In Progress");
        status.setCategory(category);

        TaskModel task = new TaskModel();
        task.setId(id);
        task.setTitle("Write \"tests\"");
        task.setDescription("Line one\nLine two");
        task.setStatus(status);
        task.setPriority(Priority.HIGH);
        task.setStartDate(LocalDate.of(2026, 6, 1));
        task.setDeadline(LocalDate.of(2026, 6, 20));
        task.setCategory(category);
        task.setDashboard(dashboard);
        return task;
    }
}
