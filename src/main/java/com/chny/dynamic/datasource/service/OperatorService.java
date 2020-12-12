package com.chny.dynamic.datasource.service;

import com.chny.dynamic.datasource.common.ApiResponse;
import com.chny.dynamic.datasource.dao.OperatorDAO;
import com.chny.dynamic.datasource.entity.ParamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperatorService {

    @Autowired
    private OperatorDAO operatorDAO;

    @Autowired
    private ApplicationContext context;

    public ApiResponse query(ParamDTO paramDTO) {
        JdbcTemplate template = null;
        try {
            template = (JdbcTemplate) context.getBean(paramDTO.getServer());
        } catch (Exception e) {
            return new ApiResponse(ApiResponse.FAIL, "error operator: server can not support operator.", null);
        }
        return new ApiResponse(operatorDAO.query(template, paramDTO.getParam()));
    }

}
