package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ApiResponse<T> {
    private int code = 1000;
    private String message;
    private T result;

    public static <T> ApiResponse<T> success(T result, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        response.setResult(result);
        return response;
    }

    public static <T> ApiResponse<T> success(T result) {
        return success(result, "Success");
    }

    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        return response;
    }
}
