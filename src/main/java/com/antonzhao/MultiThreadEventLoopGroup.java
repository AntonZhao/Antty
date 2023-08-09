package com.antonzhao;

import java.nio.channels.SocketChannel;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public abstract class MultiThreadEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup {
    public MultiThreadEventLoopGroup(int threads) {
        super(threads);
    }

    @Override
    protected abstract EventLoop newChild();

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        next();
    }
}
