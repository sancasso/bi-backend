package com.canso.csbi.manager;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetryingManager {

    public Retryer<Boolean> createRetryer(int maxAttempt,
                                          int interval,
                                          int maxInterval,
                                          TimeUnit timeUnit) {
        return RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                .retryIfResult(result -> false)
                .withStopStrategy(StopStrategies.stopAfterAttempt(maxAttempt))
                .withWaitStrategy(WaitStrategies
                        .incrementingWait(interval, timeUnit, maxInterval, timeUnit))
                .build();
    }
}
