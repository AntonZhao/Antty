package com.antonzhao;

import java.nio.channels.SocketChannel;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public class Bootstrap {
    private EventLoopGroup eventLoopGroup;

    public Bootstrap() {
    }

    public Bootstrap group(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        eventLoopGroup.register(channel,nioEventLoop);
    }
}
