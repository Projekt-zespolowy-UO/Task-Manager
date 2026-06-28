package com.project.backend.Model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.backend.Enum.DashboardRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dashboard_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"dashboard_id", "user_id"}, name = "uk_dashboard_user")
})
@Getter
@Setter
public class DashboardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    @JsonIgnore
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserModel user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DashboardRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DashboardMember() {
    }

    public DashboardMember(Dashboard dashboard, UserModel user, DashboardRole role) {
        this.dashboard = dashboard;
        this.user = user;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }
}
