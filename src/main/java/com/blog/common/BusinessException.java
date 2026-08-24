package com.blog.common;

import lombok.Getter;

/**
 * 业务异常：用于在 Service 层主动抛出，由全局异常处理器统一转为响应
 *
 * @author Liangkunrui
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
