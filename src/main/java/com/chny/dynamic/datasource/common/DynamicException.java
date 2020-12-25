package com.chny.dynamic.datasource.common;

import java.util.List;
import java.util.Locale;

public class DynamicException extends RuntimeException{
    private static final String DEFAULT_ERROR_CODE = "500";
    private String code;
    private Object[] params;
    private List<Locale> locales;

    public DynamicException(String code, Throwable e, List<Locale> locales, Object... params) {
        super(e);
        this.code = code;
        this.locales = locales;
        this.params = params;
    }

    public DynamicException(String code, Throwable e, Object... params) {
        this(code, e, null, params);
    }

    public DynamicException(String code, Object... params) {
        this(code, null, params);
    }

    public DynamicException(String msg) {
        this(DEFAULT_ERROR_CODE, null, msg);
    }

    public DynamicException(String msg, Throwable e) {
        this(DEFAULT_ERROR_CODE, e, null, msg);
    }

    public DynamicException(Throwable e) {
        this(null, e);
    }

    public DynamicException() {

    }

    public String getCode() {
        return this.code;
    }

    public String getParams() {
        String rst = "";
        if (null == this.params) {
            return rst;
        } else {
            int len = this.params.length;
            for (int i = 0; i < len; i++) {
                rst = i == len -1 ? rst.concat(StringUtil.object2Str(this.params[i])) : rst.concat(StringUtil.object2Str(this.params[i]) + ",");
            }
            return  rst;
        }
    }

    @Override
    public String toString() {
        String s = this.getClass().getName();
        String message = this.getMessage();
        return null == message ? s : s + ":" + message;
    }
}
