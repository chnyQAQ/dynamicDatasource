package com.chny.dynamic.datasource.controller;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.entity.RedisParamDTO;
import com.chny.dynamic.datasource.service.RedisOperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redisplat")
public class RedisOperatorApi {

    @Autowired
    private RedisOperatorService redisOperatorService;

    @PostMapping("/setRedis")
    public ApiResponse setRedisValue(@RequestBody RedisParamDTO paramDTO) {
        return redisOperatorService.setRedis(paramDTO);
    }

    @PostMapping("/queryRedis")
    public ApiResponse queryRedisValue(@RequestBody RedisParamDTO paramDTO) {
        return redisOperatorService.queryRedis(paramDTO);
    }



}
