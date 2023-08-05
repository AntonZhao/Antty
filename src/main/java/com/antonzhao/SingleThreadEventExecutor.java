package com.antonzhao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);
    // 任务队列的容量，默认int.max
    private static final int DEFAULT_MAX_PENDING_TASK = Integer.MAX_VALUE;
    private final Queue<Runnable> taskQueue;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private volatile boolean start = false;
    private final SelectorProvider provider;
    private Selector selector;
    private Thread thread;

    public SingleThreadEventExecutor() throws IOException {
        // 通过provider不仅可以得到selector，还可以得到ServerSocketChannel和SocketChannel
        this.provider = SelectorProvider.provider();
        this.taskQueue = newTaskQueue(DEFAULT_MAX_PENDING_TASK);
        this.rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
        this.selector = openSelector();
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

    public void register(SocketChannel socketChannel) {
        // 如果执行该方法的线程是执行器中的线程，直接执行方法
        if (inEventLoop(Thread.currentThread())) {
            register0(socketChannel);
        }else {
            // 第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            this.execute(() -> {
                register0(socketChannel);
                logger.info("客户端的channel已注册到新线程的多路复用器上了！");
            });
        }
    }

    private void register0(SocketChannel channel) {
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private boolean inEventLoop(Thread currentThread) {
        return currentThread == this.thread;
    }

    private Selector openSelector() {
        try {
            selector = provider.openSelector();
            return selector;
        } catch (IOException e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }

    public void run() {
        while (true) {
            logger.info("新线程阻塞在这里吧。。。。。。。");
            try {
                // 没事件就阻塞
                select();
                // 走到这里，说明selector没有阻塞
                processSelectedKeys(selector.selectedKeys());
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                // 执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }

    private void runAllTasks() {
        runAllTasksFrom(taskQueue);
    }

    private void runAllTasksFrom(Queue<Runnable> taskQueue) {
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

    private void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        for (;;) {
            SelectionKey key = iterator.next();
            iterator.remove();
            processSelectedKey(key);
            if (!iterator.hasNext()) {
                break;
            }
        }
    }

    private void processSelectedKey(SelectionKey key) throws IOException {
        // 如果是读事件
        if (key.isReadable()) {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int len = channel.read(byteBuffer);
            if (len == -1) {
                logger.info("客户端通道要关闭！");
                channel.close();
                return;
            }
            byte[] bytes = new byte[len];
            byteBuffer.flip();
            byteBuffer.get(bytes);
            logger.info("新线程收到客户端发送的数据:{}", new String(bytes));
        }
    }

    private void select() throws IOException {
        Selector selector = this.selector;
        for (;;) {
            //如果没有就绪事件，就在这里阻塞3秒，有限时的阻塞
            logger.info("新线程阻塞在这里3秒吧。。。。。。。");
            int selectKeys = selector.select(3000);
            // 如果有io事件或者单线程执行器中有任务待执行，就退出循环
            if (selectKeys != 0 || hasTasks()) {
                break;
            }
        }
    }

    private boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

}
