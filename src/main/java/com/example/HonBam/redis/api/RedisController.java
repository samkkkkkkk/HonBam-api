package com.example.HonBam.redis.api;

import com.example.HonBam.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;
    @GetMapping("/redis/test")
    public String redisTest(@RequestParam(value = "name") String name,
                            @RequestParam(value = "message") String message) {

        return redisService.getMessage(name, message);
    }


}
