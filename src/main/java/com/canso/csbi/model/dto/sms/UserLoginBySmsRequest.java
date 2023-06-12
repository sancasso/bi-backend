package com.canso.csbi.model.dto.sms;

import lombok.Data;

import java.io.Serializable;

/**
 * 通过手机号登录
 */
@Data
public class UserLoginBySmsRequest implements Serializable {
    /**
     * 手机号
     */
    private String phoneNum;

    /**
     * 验证码
     */
    private String code;
}