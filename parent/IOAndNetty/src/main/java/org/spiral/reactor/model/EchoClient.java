package org.spiral.reactor.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spiral.myio.config.NioConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;

/**
 * 单线程Reactor的客户端实现
 *
 * @author : spiral
 * @since : 2021/2/1 - 下午10:18
 */
public class EchoClient {

    private static Logger log = LoggerFactory.getLogger(EchoClient.class);

    public static void main(String[] args) throws IOException {
        new EchoClient().start();
    }

    public void start() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(NioConfig.IP, NioConfig.port));
        socketChannel.configureBlocking(false);
        while (!socketChannel.finishConnect()) {
        }

        log.info("客户端启动成功");
        Processor processor = new Processor(socketChannel);
        new Thread(processor).start();
    }

    private class Processor implements Runnable {
        final Selector selector;
        final SocketChannel channel;

        public Processor(SocketChannel socketChannel) throws IOException {
            selector = Selector.open();
            channel = socketChannel;
            channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    Set<SelectionKey> selected = selector.selectedKeys();
                    for (SelectionKey sk : selected) {
                        if (sk.isWritable()) {
                            //创建缓冲区
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            Scanner scanner = new Scanner(System.in);
                            log.info("请输入发送内容");
                            if (scanner.hasNext()) {
                                SocketChannel channel = (SocketChannel) sk.channel();
                                String input = scanner.next();
                                //将数据写入缓冲区
                                buffer.put((LocalDateTime.now().toString() + ">>" + input).getBytes());
                                //将缓冲区的切换为读模式
                                buffer.flip();
                                //通过socket发送数据
                                channel.write(buffer);
                                //将缓冲区切换为写模式
                                buffer.clear();
                            }
                        }
                        if (sk.isReadable()) {
                            //若选择键的IO事件是"可读事件"，读取数据
                            SocketChannel channel = (SocketChannel) sk.channel();
                            //读取数据
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int length = 0;
                            while ((length = channel.read(buffer)) > 0) {
                                buffer.flip();
                                log.info("server echo:{}", new String(buffer.array(), 0, length));
                                buffer.clear();
                            }
                        }

                    }
                    selected.clear();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
