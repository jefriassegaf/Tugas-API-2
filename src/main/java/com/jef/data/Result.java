package com.bay.data;

public class Result {
    private Object data;
    private String message;
    private int statusCode;
    private boolean isSuccess;

    public Result(Object data, String message, int statusCode, boolean isSuccess) {
        this.data = data;
        if (data != null) {
            this.message = message;
            this.statusCode = statusCode;
            this.isSuccess = isSuccess;
        } else {
            this.message = message;
            this.statusCode = 400;
            this.isSuccess = false;
        }
    }

    public Result() {
        
    }
    public String getData() {
        if (data == null) return null;
        return data.toString();
    }

    public String getMessage() {
        return "\"" + message + "\"";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}
