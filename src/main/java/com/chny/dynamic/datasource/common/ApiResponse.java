package com.chny.dynamic.datasource.common;

import java.io.Serializable;

public class ApiResponse<T> implements Serializable {

    public static final int SUCCESS = 0;
    public static final int FAIL = -1;
    public static final int BUSY = -100;
    public static final String SUCCESS_TEXT = "SUCCESS";
    public static final String BUSY_TEXT = "service is busy now";
    private long status;
    private String statusText;
    private T data;

    public ApiResponse() {
        this.status = SUCCESS;
        this.statusText = SUCCESS_TEXT;
    }

    public  ApiResponse(long status, String statusText, T data) {
        this.status = status;
        this.statusText = statusText;
        this.data = data;
    }

    public ApiResponse(T data) {
        if (data instanceof Exception) {
            this.status = FAIL;
            this.statusText = ((Exception) data).getLocalizedMessage();
        } else {
            this.status = status;
            this.statusText = statusText;
            this.data = data;
        }
    }

    public long getStatus() {
        return this.status;
    }

    public void setStatus(long status) {
        this.status = status;
        if (this.status == SUCCESS) {
            statusText = SUCCESS_TEXT;
        } else if(this.status == BUSY) {
            statusText = BUSY_TEXT;
        }
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
