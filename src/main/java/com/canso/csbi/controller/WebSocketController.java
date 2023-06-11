package com.canso.csbi.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    
    @MessageMapping("/task")
    public void handleTask() throws InterruptedException {
        // 处理客户端发送的任务
        Thread.sleep(1000);
    }
    
    @SubscribeMapping("/topic/task")
    public String handleTaskResult() {
        return "Task processing is complete!";
    }
}