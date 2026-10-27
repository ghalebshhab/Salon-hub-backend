package com.salon.backend.DTOs.Auth.Rigester;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RigesterResponse {

    private  long id;
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private String phoneNumber;
    private boolean emailVerificationRequired;
    private long verificationCodeExpiresInSeconds;
    private long resendAvailableInSeconds;

}
