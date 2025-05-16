package com.mcp.contant;


public enum Code {

    SUCCESS(200, "success"),
    ERROR(500, "系统错误"),
    DB_CONNECTION_ERROR(5001, "数据库连接尚未初始化，请先调用初始化数据库连接方法"),
    JSON_SERIALIZATION_ERROR(5002, "序列化JSON失败"),
    SQL_EXECUTION_ERROR(5003, "SQL执行失败"),
    TABLE_NOT_FOUND(5004, "表不存在或没有列信息"), 
    TABLE_QUERY_ERROR(5005, "查询表结构失败"),
    TABLE_NAMES_QUERY_ERROR(5006, "查询表名失败"),
    NO_TABLES_FOUND(5007, "数据库中没有找到任何表")
    ;


    // 错误代码
    private final int code;

    // 错误消息
    private final String message;

    // 构造方法
    Code(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // 获取错误代码
    public int getCode() {
        return code;
    }

    // 获取错误消息
    public String getMessage() {
        return message;
    }

}
