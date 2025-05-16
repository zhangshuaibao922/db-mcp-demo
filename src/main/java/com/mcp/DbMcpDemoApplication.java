package com.mcp;

import com.mcp.service.MysqlDBService;
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
    public ToolCallbackProvider weatherTools(MysqlDBService mysqlDBService) {
        return  MethodToolCallbackProvider.builder().toolObjects(mysqlDBService).build();
    }
}
