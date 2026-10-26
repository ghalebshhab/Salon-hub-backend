package com.salon.backend.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private static final String DEFAULT_ERROR_MESSAGE =
            "The request could not be completed. Please review the submitted information and try again.";


    private boolean success;

    private String message;

    private T data;



    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        String clearMessage = message == null || message.isBlank()
                ? DEFAULT_ERROR_MESSAGE
                : message.trim();
        return new ApiResponse<>(false, clearMessage, null);
    }

}
