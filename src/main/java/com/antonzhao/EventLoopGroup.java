package com.antonzhao;

import java.nio.channels.SocketChannel;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public interface EventLoopGroup extends EventExecutorGroup{
    @Override
    EventLoop next();

    void register(SocketChannel channel, NioEventLoop nioEventLoop);
}
