package com.chny.dynamic.datasource.controller;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.entity.ParamDTO;
import com.chny.dynamic.datasource.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jdbcplat")
public class OperatorApi {

    @Autowired
    private OperatorService operatorService;

    @PostMapping("/queryInfo")
    public ApiResponse query(@RequestBody ParamDTO paramDTO) {
        return operatorService.query(paramDTO);
    }

}
