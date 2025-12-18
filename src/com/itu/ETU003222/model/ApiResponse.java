package com.itu.ETU003222.model;

public class ApiResponse {
    private String status;  // "success" ou "error"
    private int code;       // 200, 400, 500, etc.
    private Object data;    // Les données à retourner
    private String message; // Message optionnel
    
    public ApiResponse() {}
    
    public ApiResponse(String status, int code, Object data) {
        this.status = status;
        this.code = code;
        this.data = data;
    }
    
    public ApiResponse(String status, int code, Object data, String message) {
        this.status = status;
        this.code = code;
        this.data = data;
        this.message = message;
    }
    
    // Méthodes statiques pour faciliter la création
    public static ApiResponse success(Object data) {
        return new ApiResponse("success", 200, data);
    }
    
    public static ApiResponse success(Object data, String message) {
        return new ApiResponse("success", 200, data, message);
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse("error", 400, null, message);
    }
    
    public static ApiResponse error(int code, String message) {
        return new ApiResponse("error", code, null, message);
    }
    
    public static ApiResponse serverError(String message) {
        return new ApiResponse("error", 500, null, message);
    }
    
    // Getters et setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}