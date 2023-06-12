package com.canso.csbi.constant;

/**
 * rabbitmq 常量
 */
public interface RabbitMqConstant {
    String sms_exchange= "sms-exchange";

    String sms_routingKey= "sms-send";

    String MQ_PRODUCER="api:mq:producer:fail";

     String SMS_HASH_PREFIX = "api:sms_hash_";

     String order_exchange = "order.exchange";

     String order_delay_queue = "order.delay.queue";

     String order_queue = "api-order-queue";

     String order_pay_success = "order-pay-success";

}