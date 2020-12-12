package com.chny.dynamic.datasource.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperatorDAO {

    public Object query(JdbcTemplate template, String param) {
        String sql = "select * from user where username = ?";
        return template.queryForList(sql, param);
    }

}
