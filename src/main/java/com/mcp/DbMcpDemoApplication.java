package com.mcp;

import com.mcp.service.MysqlDBService;
import com.mcp.service.RedisDBService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DbMcpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMcpDemoApplication.class, args);
    }


    @Bean
    public ToolCallbackProvider dbTools(MysqlDBService mysqlDBService, RedisDBService redisDBService) {
        return MethodToolCallbackProvider.builder().toolObjects(mysqlDBService, redisDBService).build();
    }
}
