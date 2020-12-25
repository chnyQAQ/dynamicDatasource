package com.chny.dynamic.datasource.service;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.entity.RedisParamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisOperatorService {

    @Autowired
    private ApplicationContext context;

    public ApiResponse queryRedis(RedisParamDTO paramDTO) {
        RedisTemplate template = null;
        try {
            template = (RedisTemplate) context.getBean(paramDTO.getServer());
            template.afterPropertiesSet();
        } catch (Exception e) {
            return new ApiResponse(ApiResponse.FAIL, "error operator: server can not support operator.", null);
        }
        return new ApiResponse(template.opsForValue().get(paramDTO.getKey()));
    }

    public ApiResponse setRedis(RedisParamDTO paramDTO) {
        RedisTemplate template = null;
        try {
            template = (RedisTemplate) context.getBean(paramDTO.getServer());
            template.afterPropertiesSet();
        } catch (Exception e) {
            return new ApiResponse(ApiResponse.FAIL, "error operator: server can not support operator.", null);
        }
        template.opsForValue().set(paramDTO.getKey(), paramDTO.getValue());
        return new ApiResponse();
    }

}
