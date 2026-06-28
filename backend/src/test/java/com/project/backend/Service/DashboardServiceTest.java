package com.project.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.project.backend.Security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DashboardMemberRepository dashboardMemberRepository;

    @Mock
    private DashboardAuthorizationService authorizationService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void transferOwnershipDemotesCurrentOwnerAndPromotesTargetMember() {
        Long dashboardId = 10L;
        UserModel currentOwner = user(1L);
        UserModel targetUser = user(2L);
        Dashboard dashboard = dashboard(dashboardId, currentOwner);
        DashboardMember currentOwnerMember = new DashboardMember(dashboard, currentOwner, DashboardRole.OWNER);
        DashboardMember targetMember = new DashboardMember(dashboard, targetUser, DashboardRole.MEMBER);

        when(authorizationService.getDashboardOrThrow(dashboardId)).thenReturn(dashboard);
        when(authorizationService.getMemberOrThrow(dashboardId, targetUser.getId())).thenReturn(targetMember);
        when(authorizationService.getOwnerMember(dashboardId)).thenReturn(currentOwnerMember);

        dashboardService.transferOwnership(
                dashboardId,
                targetUser.getId(),
                new CustomUserDetails(currentOwner)
        );

        assertThat(currentOwnerMember.getRole()).isEqualTo(DashboardRole.MEMBER);
        assertThat(targetMember.getRole()).isEqualTo(DashboardRole.OWNER);
        assertThat(dashboard.getUser()).isSameAs(targetUser);
        verify(authorizationService).validateDashboardOwnerAccess(currentOwner.getId(), dashboardId);
        verify(dashboardMemberRepository).save(currentOwnerMember);
        verify(dashboardMemberRepository).save(targetMember);
        verify(dashboardRepository).save(dashboard);
    }

    @Test
    void transferOwnershipRejectsTargetUserWhoIsAlreadyOwner() {
        Long dashboardId = 10L;
        UserModel currentOwner = user(1L);
        Dashboard dashboard = dashboard(dashboardId, currentOwner);
        DashboardMember targetMember = new DashboardMember(dashboard, currentOwner, DashboardRole.OWNER);

        when(authorizationService.getDashboardOrThrow(dashboardId)).thenReturn(dashboard);

        assertThatThrownBy(() -> dashboardService.transferOwnership(
                dashboardId,
                currentOwner.getId(),
                new CustomUserDetails(currentOwner)
        ))
                .isInstanceOf(ApiError.class)
                .satisfies(error -> assertThat(((ApiError) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(authorizationService).validateDashboardOwnerAccess(currentOwner.getId(), dashboardId);
        verify(authorizationService, never()).getOwnerMember(dashboardId);
        verify(dashboardMemberRepository, never()).save(targetMember);
        verify(dashboardRepository, never()).save(dashboard);
    }

    private UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setUserName("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setPassword("password");
        return user;
    }

    private Dashboard dashboard(Long id, UserModel owner) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(id);
        dashboard.setName("Dashboard");
        dashboard.setUser(owner);
        return dashboard;
    }
}
