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

    /**
     * Create a success response with data and custom message
     * 
     * @param result The data to return
     * @param message Custom success message
     * @return ApiResponse with success code (1000)
     */
    public static <T> ApiResponse<T> success(T result, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        response.setResult(result);
        return response;
    }

    /**
     * Create a success response with data and default message
     * 
     * @param result The data to return
     * @return ApiResponse with success code (1000) and default message
     */
    public static <T> ApiResponse<T> success(T result) {
        return success(result, "Success");
    }

    /**
     * Create a success response without data
     * 
     * @param message Success message
     * @return ApiResponse with success code (1000) and no data
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        return response;
    }
}
