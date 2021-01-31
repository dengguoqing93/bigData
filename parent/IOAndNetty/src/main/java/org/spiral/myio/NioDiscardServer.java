package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;


/**
 * @author : spiral
 * @since : 2021/1/31 - 上午10:52
 */
public class NioDiscardServer {

    private static Logger log = LoggerFactory.getLogger(NioDiscardServer.class);

    private static void startServer() throws IOException {
        //1.获取选择器
        Selector selector = Selector.open();
        //2. 获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //3. 设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //4. 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(8080));
        log.info("服务器启动成功");
        //5. 将通道注册的"接受新连接"IO时间，注册到选择器上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //6. 轮询要处理的IO就绪事件
        while (selector.select() > 0) {
            //7. 获取单个的选择键，并进行处理
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey key : selectionKeys) {
                // 8. 判断Key的具体事件类型
                if (key.isAcceptable()) {
                    //9. 当选择键的IO事件是"连接就绪"时间，读取客户端连接
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //10. 切换socketChannel为非阻塞模式
                    socketChannel.configureBlocking(false);
                    //11. 将新连接的通道注册到选择器上，并注册为可读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    //选择键的IO事件是"可读"事件，读取数据
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int length = 0;
                    while ((length = socketChannel.read(byteBuffer)) > 0) {
                        byteBuffer.flip();
                        log.info(new String(byteBuffer.array(), 0, length));
                        byteBuffer.clear();
                    }
                    socketChannel.close();
                }
                selectionKeys.remove(key);
            }
        }
        serverSocketChannel.close();
    }

    public static void main(String[] args) throws IOException {
        startServer();
    }
}
