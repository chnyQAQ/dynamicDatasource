package com.chny.dynamic.datasource.controller;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.entity.DBParamDTO;
import com.chny.dynamic.datasource.service.JdbcOperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jdbcplat")
public class JdbcOperatorApi {

    @Autowired
    private JdbcOperatorService jdbcOperatorService;

    @PostMapping("/queryDB")
    public ApiResponse queryDBInfo(@RequestBody DBParamDTO paramDTO) {
        return jdbcOperatorService.queryDB(paramDTO);
    }

}
