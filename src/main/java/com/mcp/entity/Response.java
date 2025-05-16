package com.mcp.entity;
import com.mcp.contant.Code;

import java.io.Serializable;

/**
 * @author
 * @Version 1.0
 * @Description 统一返回值
 * @date 2024/3/23 21:03
 */
public class Response<T> implements Serializable {
    private int code;
    private String message;
    private T  data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Response() {
    }

    public static <T> Response<T> ok(T data) {
        return generateResponse(Code.SUCCESS.getCode(), Code.SUCCESS.getMessage(), data);
    }

    private static <T> Response<T> generateResponse(int code, String message, T data) {
        Response<T> objectResponse = new Response<>();
        objectResponse.setCode(code);
        objectResponse.setMessage(message);
        objectResponse.setData(data);
        return objectResponse;
    }

    public static <T> Response<T> error(Code code) {
        return generateResponse(code.getCode(), code.getMessage(), null);
    }
}
