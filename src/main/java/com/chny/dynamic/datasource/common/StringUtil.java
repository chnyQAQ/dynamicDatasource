package com.chny.dynamic.datasource.common;

public class StringUtil {

    public static  String object2Str(Object obj) {
        return null == obj || "".equals(String.valueOf(obj)) ? "" : String.valueOf(obj);
    }

}
