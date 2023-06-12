package com.canso.csbi.constant;

/**
 * cookie 过期时间
 */
public interface CookieConstant {

     String headAuthorization = "authorization";

     String autoLoginAuthCheck = "_Wu2ia_remember";

     int expireTime = 2592000 ;//30天过期

     String orderToken = "api-order-token";

     int orderTokenExpireTime = 1800; // 30分钟过期
     byte[] autoLoginKey = "Wzy-ApiAutoLogin".getBytes();
}
