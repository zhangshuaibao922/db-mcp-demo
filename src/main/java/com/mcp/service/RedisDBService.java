package com.mcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.entity.Response;
import com.mcp.contant.Code;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisDBService {

    private JedisPool jedisPool;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 2000; // 默认连接超时时间2000毫秒

    @Tool(description = "初始化Redis连接")
    public String initRedisConnection(
            @ToolParam(description = "Redis主机地址") String host,
            @ToolParam(description = "Redis端口") int port,
            @ToolParam(description = "Redis密码") String password
    ) {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            
            // 创建Jedis连接池
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, DEFAULT_TIMEOUT, password);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port);
            }
            
            // 测试连接
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            
            return objectMapper.writeValueAsString(Response.ok("Redis连接初始化成功"));
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "获取所有Redis键")
    public String getAllKeys() {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keySet = jedis.keys("*");
            List<String> keys = new ArrayList<>(keySet);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("keys", keys);
            resultData.put("count", keys.size());
            
            if (keys.isEmpty()) {
                return objectMapper.writeValueAsString(Response.error(Code.NO_TABLES_FOUND));
            }
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.TABLE_NAMES_QUERY_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "获取Redis键的信息")
    public String getKeyInfo(
            @ToolParam(description = "键名") String key
    ) {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 检查键是否存在
            if (!jedis.exists(key)) {
                try {
                    return objectMapper.writeValueAsString(Response.error(Code.TABLE_NOT_FOUND));
                } catch (Exception ex) {
                    return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
                }
            }
            
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("key", key);
            keyInfo.put("type", jedis.type(key));
            keyInfo.put("ttl", jedis.ttl(key));
            
            // 根据类型获取额外信息
            String type = jedis.type(key);
            switch (type) {
                case "string":
                    keyInfo.put("value", jedis.get(key));
                    break;
                case "list":
                    keyInfo.put("length", jedis.llen(key));
                    keyInfo.put("values", jedis.lrange(key, 0, 9)); // 获取前10个元素
                    break;
                case "set":
                    keyInfo.put("size", jedis.scard(key));
                    keyInfo.put("members", jedis.smembers(key));
                    break;
                case "zset":
                    keyInfo.put("size", jedis.zcard(key));
                    keyInfo.put("members", jedis.zrange(key, 0, 9)); // 获取前10个元素
                    break;
                case "hash":
                    keyInfo.put("size", jedis.hlen(key));
                    keyInfo.put("fields", jedis.hgetAll(key));
                    break;
            }
            
            return objectMapper.writeValueAsString(Response.ok(keyInfo));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.TABLE_QUERY_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "执行Redis命令")
    public String executeCommand(
            @ToolParam(description = "Redis命令") String command
    ) {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("command", command);
            
            // 解析命令
            String[] parts = command.trim().split("\\s+");
            String cmd = parts[0].toUpperCase();
            
            Object result;
            switch (cmd) {
                case "GET":
                    result = jedis.get(parts[1]);
                    break;
                case "SET":
                    if (parts.length > 3) {
                        // 使用SetParams处理选项
                        redis.clients.jedis.params.SetParams setParams = new redis.clients.jedis.params.SetParams();
                        
                        // 处理可能的选项
                        for (int i = 3; i < parts.length; i++) {
                            String option = parts[i].toUpperCase();
                            if ("EX".equals(option) && i + 1 < parts.length) {
                                // 设置过期时间（秒）
                                setParams.ex(Integer.parseInt(parts[++i]));
                            } else if ("PX".equals(option) && i + 1 < parts.length) {
                                // 设置过期时间（毫秒）
                                setParams.px(Long.parseLong(parts[++i]));
                            } else if ("NX".equals(option)) {
                                // 键不存在时设置
                                setParams.nx();
                            } else if ("XX".equals(option)) {
                                // 键存在时设置
                                setParams.xx();
                            }
                        }
                        
                        result = jedis.set(parts[1], parts[2], setParams);
                    } else {
                        result = jedis.set(parts[1], parts[2]);
                    }
                    break;
                case "DEL":
                    String[] keys = new String[parts.length - 1];
                    System.arraycopy(parts, 1, keys, 0, parts.length - 1);
                    result = jedis.del(keys);
                    break;
                case "KEYS":
                    result = jedis.keys(parts[1]);
                    break;
                case "TTL":
                    result = jedis.ttl(parts[1]);
                    break;
                case "EXPIRE":
                    result = jedis.expire(parts[1], Integer.parseInt(parts[2]));
                    break;
                case "LPUSH":
                    String[] values = new String[parts.length - 2];
                    System.arraycopy(parts, 2, values, 0, parts.length - 2);
                    result = jedis.lpush(parts[1], values);
                    break;
                case "RPUSH":
                    values = new String[parts.length - 2];
                    System.arraycopy(parts, 2, values, 0, parts.length - 2);
                    result = jedis.rpush(parts[1], values);
                    break;
                case "LRANGE":
                    result = jedis.lrange(parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
                    break;
                case "HSET":
                    if (parts.length == 4) {
                        result = jedis.hset(parts[1], parts[2], parts[3]);
                    } else {
                        Map<String, String> hash = new HashMap<>();
                        for (int i = 2; i < parts.length; i += 2) {
                            if (i + 1 < parts.length) {
                                hash.put(parts[i], parts[i + 1]);
                            }
                        }
                        result = jedis.hset(parts[1], hash);
                    }
                    break;
                case "HGET":
                    result = jedis.hget(parts[1], parts[2]);
                    break;
                case "HGETALL":
                    result = jedis.hgetAll(parts[1]);
                    break;
                case "SADD":
                    values = new String[parts.length - 2];
                    System.arraycopy(parts, 2, values, 0, parts.length - 2);
                    result = jedis.sadd(parts[1], values);
                    break;
                case "SMEMBERS":
                    result = jedis.smembers(parts[1]);
                    break;
                case "ZADD":
                    Map<String, Double> scoreMembers = new HashMap<>();
                    for (int i = 2; i < parts.length; i += 2) {
                        if (i + 1 < parts.length) {
                            scoreMembers.put(parts[i + 1], Double.parseDouble(parts[i]));
                        }
                    }
                    result = jedis.zadd(parts[1], scoreMembers);
                    break;
                case "ZRANGE":
                    result = jedis.zrange(parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
                    break;
                default:
                    // 对于其他不支持的命令，返回错误信息
                    return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            }
            
            resultData.put("result", result);
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "设置字符串值")
    public String setStringValue(
            @ToolParam(description = "键名") String key,
            @ToolParam(description = "值") String value,
            @ToolParam(description = "过期时间（秒）") Integer expireSeconds
    ) {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, value);
            
            if (expireSeconds != null && expireSeconds > 0) {
                jedis.expire(key, expireSeconds);
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("key", key);
            resultData.put("value", value);
            resultData.put("result", result);
            if (expireSeconds != null && expireSeconds > 0) {
                resultData.put("expire", expireSeconds);
            }
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "获取字符串值")
    public String getStringValue(
            @ToolParam(description = "键名") String key
    ) {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 检查键是否存在
            if (!jedis.exists(key)) {
                try {
                    return objectMapper.writeValueAsString(Response.error(Code.TABLE_NOT_FOUND));
                } catch (Exception ex) {
                    return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
                }
            }
            
            String value = jedis.get(key);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("key", key);
            resultData.put("value", value);
            resultData.put("ttl", jedis.ttl(key));
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
    
    @Tool(description = "删除键")
    public String deleteKey(
            @ToolParam(description = "键名") String key
    ) {
        if (jedisPool == null) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.DB_CONNECTION_ERROR));
            } catch (Exception e) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 检查键是否存在
            if (!jedis.exists(key)) {
                try {
                    return objectMapper.writeValueAsString(Response.error(Code.TABLE_NOT_FOUND));
                } catch (Exception ex) {
                    return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
                }
            }
            
            Long result = jedis.del(key);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("key", key);
            resultData.put("deleted", result);
            
            return objectMapper.writeValueAsString(Response.ok(resultData));
            
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Response.error(Code.SQL_EXECUTION_ERROR));
            } catch (Exception ex) {
                return "{\"code\":" + Code.JSON_SERIALIZATION_ERROR.getCode() + ",\"message\":\"" + Code.JSON_SERIALIZATION_ERROR.getMessage() + "\"}";
            }
        }
    }
} 