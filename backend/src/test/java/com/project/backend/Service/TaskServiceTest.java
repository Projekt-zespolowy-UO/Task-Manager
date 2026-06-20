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
import java.util.List;

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
