package com.chny.dynamic.datasource.service;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.dao.JdbcOperatorDAO;
import com.chny.dynamic.datasource.entity.DBParamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JdbcOperatorService {

    @Autowired
    private JdbcOperatorDAO jdbcOperatorDAO;

    @Autowired
    private ApplicationContext context;

    public ApiResponse queryDB(DBParamDTO paramDTO) {
        JdbcTemplate template = null;
        try {
            template = (JdbcTemplate) context.getBean(paramDTO.getServer());
        } catch (Exception e) {
            return new ApiResponse(ApiResponse.FAIL, "error operator: server can not support operator.", null);
        }
        return new ApiResponse(jdbcOperatorDAO.query(template, paramDTO.getParam()));
    }

}
