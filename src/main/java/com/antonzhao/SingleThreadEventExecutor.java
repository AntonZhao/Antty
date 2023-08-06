package com.antonzhao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

// 主要负责线程池相关的功能
public abstract class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);
    // 任务队列的容量，默认int.max
    private static final int DEFAULT_MAX_PENDING_TASK = Integer.MAX_VALUE;
    private final Queue<Runnable> taskQueue;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private volatile boolean start = false;
    private Thread thread;

    public SingleThreadEventExecutor() {
        this.taskQueue = newTaskQueue(DEFAULT_MAX_PENDING_TASK);
        this.rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
    }

    private Queue<Runnable> newTaskQueue(int maxPendingTask) {
        return new LinkedBlockingQueue<>(maxPendingTask);
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        // 把任务提交到任务队列
        // 直接提交队列是考虑到[单线程执行器]既要处理IO事件，也需要执行用户提交的任务
        addTask(task);
        // 启动单线程执行器中的线程
        startThread();
    }

    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (!offerTask(task)) {
            reject(task);
        }
    }

    private void startThread() {
        if (start) {
            return;
        }
        start = true;
        new Thread(() -> {
            thread = Thread.currentThread();
            SingleThreadEventExecutor.this.run();
        }).start();
        logger.info("新线程创建了！");
    }

    private boolean offerTask(Runnable task) {
        return taskQueue.offer(task);
    }

    private void reject(Runnable task) {
//        rejectedExecutionHandler.rejectedExecution(task, this);
    }

    public boolean inEventLoop(Thread currentThread) {
        return currentThread == this.thread;
    }

    protected abstract void run();

    protected void runAllTasks() {
        runAllTasksFrom(taskQueue);
    }

    protected void runAllTasksFrom(Queue<Runnable> taskQueue) {
        // 从任务队列中拉取任务,如果第一次拉取就为null，说明任务队列中没有任务，直接返回即可
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }
        for (;;) {
            safeExecute(task);
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }

    private void safeExecute(Runnable task) {
        task.run();
    }

    private Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        return taskQueue.poll();
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

}
