package com.antonzhao;

/**
 * @Author: zhaoxin227
 * @Date: 2023/8/9
 */
public class NioEventLoopGroup extends MultiThreadEventLoopGroup{
    public NioEventLoopGroup(int threads) {
        super(threads);
    }

    @Override
    protected EventLoop newChild() {
        return new NioEventLoop();
    }
}
