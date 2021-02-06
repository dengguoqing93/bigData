package org.spiral.reactor.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author : spiral
 * @since : 2021/2/1 - 下午9:54
 */
public class EchoHandler implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(EchoHandler.class);

    private static final int RECEIVING = 0, SENDING = 1;
    private final SocketChannel channel;
    private final SelectionKey sk;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private int state = RECEIVING;

    public EchoHandler(Selector selector, SocketChannel channel) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);
        sk = channel.register(selector, 0);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if (state == SENDING) {
                //写入通道
                channel.write(buffer);
                //写完后，准备开始从通道读，buffer切换成写入模式
                buffer.clear();
                //写完后，注册read就绪状态事件
                sk.interestOps(SelectionKey.OP_READ);
                //进入接受的状态
                state = RECEIVING;
            } else if (state == RECEIVING) {
                //从通道读
                int length = 0;
                while ((length = channel.read(buffer)) > 0) {
                    logger.info(new String(buffer.array(), 0, length));
                }
                //读完后，准备开始写入通道，buffer切换成读取模式
                buffer.flip();
                //注册write就绪事件
                sk.interestOps(SelectionKey.OP_WRITE);
                //进入发送的状态
                state = SENDING;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
