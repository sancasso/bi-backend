package com.canso.csbi.model.entity;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Resource;


public class MessageReceiver {
    
    @Resource
    private SimpMessagingTemplate messagingTemplate;
    
    @RabbitHandler
    public void receiveMessage(String message) throws InterruptedException {
        // 处理任务
        Thread.sleep(10000);
        // 发送通知
        messagingTemplate.convertAndSend("/topic/task", "Task processing is complete!");
    }
}