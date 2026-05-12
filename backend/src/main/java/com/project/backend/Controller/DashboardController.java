package com.project.backend.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.DashboardCreateDto;
import com.project.backend.Model.Dashboard;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.DashboardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<Dashboard>> getDashboards(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Dashboard> dashboards = dashboardService.getDashboards(userDetails);
        return ResponseEntity.ok(dashboards);
    }

    @PostMapping
    public ResponseEntity<Dashboard> createDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DashboardCreateDto dto) {
        Dashboard dashboard = dashboardService.createDashboard(userDetails, dto.getName());
        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardService.deleteDashboard(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
