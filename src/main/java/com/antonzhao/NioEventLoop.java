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
import java.util.Set;
import java.util.concurrent.TimeUnit;

// 负责nio
public class NioEventLoop extends SingleThreadEventLoop {
    private final static Logger logger = LoggerFactory.getLogger(NioEventLoop.class);
    private final SelectorProvider provider;
    private Selector selector;


    public NioEventLoop() {
        // 通过provider不仅可以得到selector，还可以得到ServerSocketChannel和SocketChannel
        this.provider = SelectorProvider.provider();
        this.selector = openSelector();
    }

    private Selector openSelector() {
        try {
            selector = provider.openSelector();
            return selector;
        } catch (IOException e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }

    public Selector selector() {
        return selector;
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

    @Override
    public void run() {
        while (true) {
            logger.info("新线程阻塞在这里吧。。。。。。。");
            try {
                // 没事件就阻塞
                select();
                // 走到这里，说明selector没有阻塞
                processSelectedKeys(selector.selectedKeys());
            } catch (IOException e) {
                logger.error("nio handle error.", e);
//                logger.error(e.getMessage());
            } finally {
                // 执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }

    @Override
    public void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {

    }
}
