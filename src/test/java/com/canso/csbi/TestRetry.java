package com.canso.csbi;

import com.canso.csbi.manager.RetryingManager;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class TestRetry {
//    private static final Logger LOGGER = LoggerFactory.getLogger(TestRetry.class);
//    @Resource
//    private RetryingManager Retrying;
//
//    @Test
//    void text() {
//        Retryer<Boolean> retryer = Retrying.createRetryer(3, 1, 5, TimeUnit.SECONDS);
//        try {
//            boolean result = retryer.call(TestRetry::foo);
//            LOGGER.info("result: {}", result);
//            System.out.println("result:" + result);
//        } catch (Exception e) {
//            LOGGER.error("error: {}", e);
//            System.out.println("error:" + e);
//        }
//    }

//    private static boolean foo() throws Exception {
//        LOGGER.info("execute foo");
//        if (Math.random() < 0.7) {
//            TimeUnit.SECONDS.sleep(3);
//            if (Math.random() < 0.8) {
//                LOGGER.info("foo success");
//                System.out.println("success");
//                return true;
//            }
//        }
//        LOGGER.info("foo fail");
//        System.out.println("fail");
//        throw new RuntimeException("foo error");
//    }

    private int invokeCount = 0;

    public int realAction(int num) {
        invokeCount++;
        System.out.println(String.format("当前执行第 %d 次,num:%d", invokeCount, num));
        if (num <= 0) {
            throw new IllegalArgumentException();
        }
        return num;
    }

    @Test
    public void guavaRetryTest001() {
        Retryer<Integer> retryer = RetryerBuilder.<Integer>newBuilder()
                // 非正数进行重试
                .retryIfRuntimeException()
                // 偶数则进行重试
                .retryIfResult(result -> result % 2 == 0)
                // 设置最大执行次数3次
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();

        try {
            invokeCount=0;
            retryer.call(() -> realAction(0));
        } catch (Exception e) {
            System.out.println("执行0，异常：" + e.getMessage());
        }

        try {
            invokeCount=0;
            retryer.call(() -> realAction(1));
        } catch (Exception e) {
            System.out.println("执行1，异常：" + e.getMessage());
        }

        try {
            invokeCount=0;
            retryer.call(() -> realAction(2));
        } catch (Exception e) {
            System.out.println("执行2，异常：" + e.getMessage());
        }
    }
}