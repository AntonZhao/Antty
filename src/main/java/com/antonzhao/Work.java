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

public class Work implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Work.class);
    private volatile boolean start;
    private final SelectorProvider provider;
    private Selector selector;
    private Thread thread;
    private SelectionKey selectionKey;
    private SocketChannel socketChannel;

    public Work() throws IOException {
        provider = SelectorProvider.provider();
        this.selector = openSelector();
        thread = new Thread(this);
    }

    public void register(SocketChannel socketChannel) {
        try {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Selector openSelector() {
        try {
            selector = provider.openSelector();
            return selector;
        } catch (IOException e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }
    public void start() {
        if (start) {
            return;
        }
        start = true;
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            logger.info("新线程阻塞在这里吧。。。。。。。");
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int len = channel.read(byteBuffer);
                        if (len == -1) {
                            logger.info("客户端通道要关闭！");
                            channel.close();
                            break;
                        }
                        byte[] bytes = new byte[len];
                        byteBuffer.flip();
                        byteBuffer.get(bytes);
                        logger.info("新线程收到客户端发送的数据:{}", new String(bytes));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
