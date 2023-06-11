package com.canso.csbi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/text")
@Slf4j
public class textController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void processTask() throws InterruptedException {
        // 处理任务
        Thread.sleep(1000);
        // 发送异步消息
        rabbitTemplate.convertAndSend("task-queue", "Task processing is complete!");
    }

}
