package com.antonzhao;

import java.util.concurrent.TimeUnit;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public interface EventExecutorGroup {
    EventExecutor next();
    void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);
}
