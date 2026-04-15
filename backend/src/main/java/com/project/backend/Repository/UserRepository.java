package com.project.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.UserModel;

public interface  UserRepository extends JpaRepository<UserModel,Long>{

    Optional<UserModel> findByEmail(String email);

    boolean existsByEmail(String email);

}
