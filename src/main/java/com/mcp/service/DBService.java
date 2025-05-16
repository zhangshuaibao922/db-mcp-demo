package com.mcp.service;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class DBService {

    private MysqlDataSource mysqlDataSource;

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
            
            return "数据库连接初始化成功";
        } catch (Exception e) {
            return "数据库连接初始化失败: " + e.getMessage();
        }
    }
    
    @Tool(description = "查询数据库中所有的表名")
    public String getAllTableNames() {
        if (mysqlDataSource == null) {
            return "错误：数据库连接尚未初始化，请先调用初始化数据库连接方法";
        }
        
        List<String> tableNames = new ArrayList<>();
        Connection conn = null;
        ResultSet rs = null;
        
        try {
            conn = mysqlDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取当前数据库的所有表
            // 参数说明：catalog(可为null), schemaPattern(可为null), tableNamePattern, types
            rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            
            if (tableNames.isEmpty()) {
                return "数据库中没有找到任何表";
            } else {
                return "数据库中的表: " + String.join(", ", tableNames);
            }
            
        } catch (Exception e) {
            return "查询表名失败: " + e.getMessage();
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
            return "错误：数据库连接尚未初始化，请先调用初始化数据库连接方法";
        }
        
        Connection conn = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();
        
        try {
            conn = mysqlDataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取表的列信息
            rs = metaData.getColumns(conn.getCatalog(), null, tableName, null);
            
            // 检查表是否存在
            if (!rs.next()) {
                return "表 '" + tableName + "' 不存在或没有列信息";
            }
            
            // 回到结果集的开始
            rs.beforeFirst();
            
            // 构建表结构信息
            result.append("表 '").append(tableName).append("' 的结构:\n");
            result.append("---------------------------------------\n");
            result.append(String.format("%-20s %-15s %-10s %-10s\n", "列名", "数据类型", "大小", "是否可为空"));
            result.append("---------------------------------------\n");
            
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                String nullable = rs.getInt("NULLABLE") == 1 ? "是" : "否";
                
                result.append(String.format("%-20s %-15s %-10d %-10s\n", 
                        columnName, typeName, columnSize, nullable));
            }
            
            // 获取主键信息
            ResultSet pkRs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            pkRs.close();
            
            if (!primaryKeys.isEmpty()) {
                result.append("\n主键: ").append(String.join(", ", primaryKeys));
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "查询表结构失败: " + e.getMessage();
        } finally {
            try {
                if (rs != null) rs.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭资源时的异常
            }
        }
    }
}