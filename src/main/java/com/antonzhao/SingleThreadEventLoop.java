package com.antonzhao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {
    private final static Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

    public SingleThreadEventLoop() {
    }

    @Override
    public EventLoop next() {
        return this;
    }

    public void register(SocketChannel socketChannel, NioEventLoop nioEventLoop) {
        // 如果执行该方法的线程是执行器中的线程，直接执行方法
        if (inEventLoop(Thread.currentThread())) {
            register0(socketChannel, nioEventLoop);
        } else {
            // 第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            this.execute(() -> {
                register0(socketChannel, nioEventLoop);
                logger.info("客户端的channel已注册到新线程的多路复用器上了！");
            });
        }
    }

    private void register0(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.selector(), SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


}
