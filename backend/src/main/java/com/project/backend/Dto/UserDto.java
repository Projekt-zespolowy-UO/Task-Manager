package com.project.backend.Dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class UserDto {
    private Long id;
    private String userName;
    private String email;
    private String password;
}
