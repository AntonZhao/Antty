package com.antonzhao;

import java.util.concurrent.TimeUnit;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public abstract class MultiThreadEventExecutorGroup implements EventExecutorGroup {
   private EventExecutor[] eventExecutors;
   private int index = 0;

    public MultiThreadEventExecutorGroup(int threads) {
        eventExecutors = new EventExecutor[threads];
        for (int i = 0; i < threads; i++) {
            eventExecutors[i] = newChild();
        }
    }
    protected abstract EventExecutor newChild();

    @Override
    public EventExecutor next() {
        int id = index % eventExecutors.length;
        index++;
        return eventExecutors[id];
    }

    @Override
    public void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        next().shutdownGracefully(quietPeriod, timeout, unit);
    }
}
