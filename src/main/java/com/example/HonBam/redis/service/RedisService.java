package com.example.HonBam.redis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    @Cacheable(value = "message", key = "#name")
    public String getMessage(String name, String message) {
        System.out.println("Redis...");
        return "User: " + name;
    }
}
