package com.project.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.project.backend.Enum.DashboardRole;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.DashboardMember;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.DashboardMemberRepository;
import com.project.backend.Repository.DashboardRepository;

@ExtendWith(MockitoExtension.class)
class DashboardAuthorizationServiceTest {

    @Mock
    private DashboardMemberRepository dashboardMemberRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardAuthorizationService dashboardAuthorizationService;

    @Test
    void validateDashboardAccessRejectsNonMember() {
        when(dashboardMemberRepository.existsByDashboardIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> dashboardAuthorizationService.validateDashboardAccess(1L, 10L))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void validateDashboardOwnerAccessAllowsOwner() {
        DashboardMember owner = member(DashboardRole.OWNER);

        when(dashboardMemberRepository.findByDashboard_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(owner));

        dashboardAuthorizationService.validateDashboardOwnerAccess(1L, 10L);
    }

    @Test
    void getOwnerMemberRejectsDashboardWithoutOwner() {
        when(dashboardMemberRepository.findByDashboardIdAndRole(10L, DashboardRole.OWNER)).thenReturn(List.of());

        assertThatThrownBy(() -> dashboardAuthorizationService.getOwnerMember(10L))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getDashboardOrThrowRejectsMissingDashboard() {
        when(dashboardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardAuthorizationService.getDashboardOrThrow(10L))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private DashboardMember member(DashboardRole role) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(10L);

        UserModel user = new UserModel();
        user.setId(1L);

        return new DashboardMember(dashboard, user, role);
    }
}
