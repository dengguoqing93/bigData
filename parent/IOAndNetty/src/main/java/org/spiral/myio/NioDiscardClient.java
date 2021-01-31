package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author : spiral
 * @since : 2021/1/31 - 上午11:13
 */
public class NioDiscardClient {
    private static Logger log = LoggerFactory.getLogger(NioDiscardClient.class);

    private static void startClient() throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        //1.获取通道
        SocketChannel socketChannel = SocketChannel.open(address);

        //2. 切换为非阻塞模式
        socketChannel.configureBlocking(false);
        //不断自选、等待连接完成，在期间可以做些其他事情
        while (!socketChannel.finishConnect()) {
            log.info("进行服务端连接");
        }
        log.info("连接服务端成功");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello world".getBytes());
        byteBuffer.flip();
        //发送数据到服务器
        socketChannel.write(byteBuffer);
        socketChannel.shutdownOutput();
        socketChannel.close();
    }

    public static void main(String[] args) throws IOException {
        startClient();

    }

}
