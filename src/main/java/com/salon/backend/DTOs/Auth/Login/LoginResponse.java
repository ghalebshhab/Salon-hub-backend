package com.salon.backend.DTOs.Auth.Login;

import com.salon.backend.Entities.users.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private long id;
    private String username;
    private UserRole userRole;
    private String email;


}
