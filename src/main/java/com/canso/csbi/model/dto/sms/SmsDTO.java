package com.canso.csbi.model.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 短信服务传输对象
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsDTO implements Serializable {

    private static final long serialVersionUID = 8504215015474691352L;

    String phoneNum;

    String code;
}