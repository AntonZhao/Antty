package com.antonzhao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class SimpleServer {
    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(SimpleServer.class);

        // 创建服务端channel，并设置非阻塞
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        // 获得selector
        Selector selector = Selector.open();
        // 把channel注册到selector上，现在还未给key设置感兴趣的事件
        SelectionKey selectionKey = serverSocketChannel.register(selector, 0, serverSocketChannel);
        // 给key设置感兴趣的事件
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        // 绑定端口号
        serverSocketChannel.bind(new InetSocketAddress(8080));

        // 开始接收连接，处理事件，整个处理是一个死循环
        while (true) {
            // 没有事件的时候，这里是阻塞的，有事件才往下走
            selector.select();
            // 如果有事件到来，得到注册到该selector的所有key，每个key上都有一个channel
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isAcceptable()) {
                    // 有两种方式获得服务端的channel，一种是直接获得，第二种是通过attachment获得
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    // ServerSocketChannel channel = (ServerSocketChannel) key.attachment();
                    // 得到客户端的channel
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    // 客户端的channel和服务端channel做法类似，也被注册到selector上，通过轮询并处理channel上的相关事件
                    SelectionKey socketChannelKey = socketChannel.register(selector, 0, socketChannel);
                    socketChannelKey.interestOps(SelectionKey.OP_READ);
                    logger.info("客户端连接成功!");
                    socketChannel.write(ByteBuffer.wrap("我发送成功了".getBytes()));
                    logger.info("向客户端发送数据成功!");
                }
                // 如果是可读事件，说明要用客户端的channel来处理
                if (key.isReadable()) {
                    // 获得客户端的channel
                    SocketChannel channel = (SocketChannel) key.channel();
                    // 分配字符缓冲区来接受客户端传来的数据
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int len = channel.read(buffer);
                    logger.info("读到的字节数：" + len);
                    if (len == -1) {
                        channel.close();
                        break;
                    } else {
                        // 切换buffer的读模式
                        buffer.flip();
                        logger.info(Charset.defaultCharset().decode(buffer).toString());
                    }
                }
            }
        }

    }
}
