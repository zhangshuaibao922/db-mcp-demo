package com.mcp.service;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.entity.Response;
import com.mcp.contant.Code;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MysqlDBService {

    private MysqlDataSource mysqlDataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(description = "初始化数据库连接")
    public String initDatabaseConnection(
            @ToolParam(description = "数据库驱动类名") String driverClassName,
            @ToolParam(description = "数据库URL") String url,
            @ToolParam(description = "数据库用户名") String username,
            @ToolParam(description = "数据库密码") String password
    ) {
        try {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUrl(url);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            
            // 测试连接
            dataSource.getConnection().close();
            
            // 设置数据源
            this.mysqlDataSource = dataSource;
            
            return objectMapper.writeValueAsString(Response.ok("数据库连接初始化成功"));
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "查询数据库中所有的表名")
    public String getAllTableNames() {
        if (mysqlDataSource == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        List<String> tableNames = new ArrayList<>();
        Connection conn = null;
        ResultSet rs = null;
        
        try {
            conn = mysqlDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取当前数据库的所有表
            rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("tables", tableNames);
            resultData.put("count", tableNames.size());
            
            if (tableNames.isEmpty()) {
                return objectMapper.writeValueAsString(Response.error(Code.NO_TABLES_FOUND));
            }
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.TABLE_NAMES_QUERY_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭资源时的异常
            }
        }
    }
    
    @Tool(description = "查询指定表的表结构")
    public String getTableStructure(
            @ToolParam(description = "表名") String tableName
    ) {
        if (mysqlDataSource == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        Connection conn = null;
        ResultSet rs = null;
        
        try {
            conn = mysqlDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取表的列信息
            rs = metaData.getColumns(conn.getCatalog(), null, tableName, null);
            
            // 检查表是否存在
            if (!rs.next()) {
                try {
                    return objectMapper.writeValueAsString(Response.error(Code.TABLE_NOT_FOUND));
                } catch (Exception ex) {
                    return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
                }
            }
            
            // 回到结果集的开始
            rs.beforeFirst();
            
            List<Map<String, Object>> columns = new ArrayList<>();
            
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getInt("NULLABLE") == 1);
                columns.add(column);
            }
            
            // 获取主键信息
            ResultSet pkRs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            pkRs.close();
            
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("tableName", tableName);
            tableInfo.put("columns", columns);
            if (!primaryKeys.isEmpty()) {
                tableInfo.put("primaryKeys", primaryKeys);
            }
            
            return objectMapper.writeValueAsString(Response.ok(tableInfo));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.TABLE_QUERY_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭资源时的异常
            }
        }
    }
    
    @Tool(description = "执行SQL语句")
    public String executeSQL(
            @ToolParam(description = "SQL语句") String sql
    ) {
        if (mysqlDataSource == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = mysqlDataSource.getConnection();
            stmt = conn.createStatement();
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("sql", sql);
            
            // 判断SQL类型（查询或更新）
            sql = sql.trim();
            if (sql.toLowerCase().startsWith("select")) {
                // 执行查询
                rs = stmt.executeQuery(sql);
                
                // 获取结果集元数据
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // 获取列名
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }
                resultData.put("columns", columns);
                
                // 获取数据行
                List<Map<String, Object>> dataList = new ArrayList<>();
                int rowCount = 0;
                
                while (rs.next()) {
                    rowCount++;
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    dataList.add(row);
                }
                
                resultData.put("data", dataList);
                resultData.put("rowCount", rowCount);
                resultData.put("type", "query");
            } else {
                // 执行更新（INSERT, UPDATE, DELETE等）
                int affectedRows = stmt.executeUpdate(sql);
                resultData.put("affectedRows", affectedRows);
                resultData.put("type", "update");
            }
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭资源时的异常
            }
        }
    }
}