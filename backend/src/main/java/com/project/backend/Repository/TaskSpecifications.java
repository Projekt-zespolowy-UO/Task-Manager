package com.project.backend.Repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.project.backend.Dto.TaskFilterDto;
import com.project.backend.Model.TaskModel;

import jakarta.persistence.criteria.Predicate;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<TaskModel> forUserAndFilter(Long userId, TaskFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (filter != null) {
                if (filter.getStatusId() != null) {
                    predicates.add(cb.equal(root.get("status").get("id"), filter.getStatusId()));
                }
                if (filter.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
                }
                if (filter.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
                }
                if (filter.getDashboardId() != null) {
                    predicates.add(cb.equal(root.get("dashboard").get("id"), filter.getDashboardId()));
                }
                if (filter.getCustomStatusId() != null) {
                    predicates.add(cb.equal(root.get("customStatus").get("id"), filter.getCustomStatusId()));
                }
                if (filter.getDeadlineFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), filter.getDeadlineFrom()));
                }
                if (filter.getDeadlineTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), filter.getDeadlineTo()));
                }
                if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                    String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
