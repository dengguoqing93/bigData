package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spiral.myio.config.NioConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author : spiral
 * @since : 2021/1/31 - 下午4:18
 */
public class NioReceiveServer {

    private static Logger log = LoggerFactory.getLogger(NioReceiveServer.class);

    //使用Map保存每个文件传输，当OP_READ可读时，根据通道找到对应的对象
    private Map<SelectableChannel, Client> clientMap = new HashMap<>();
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public static void main(String[] args) throws IOException {
        NioReceiveServer server = new NioReceiveServer();
        server.startServer();
    }

    public void startServer() throws IOException {
        //1. 获取选择器
        Selector selector = Selector.open();
        //2. 获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //3. 设置通道为非阻塞
        serverSocketChannel.configureBlocking(false);
        //4. 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(9000));
        //5. 将通道注册到选择器上，注册的IO时间为："接受新连接"
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("serverChannel is listening...");
        //6. 选择感兴趣的IO就绪事件
        while (selector.select() > 0) {
            //7. 处理就绪事件
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (key.isAcceptable()) {
                    //8. 若就绪事件为"新连接事件"，获取客户端新连接,并建立连注册到选择器上
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    if (Objects.isNull(socketChannel)) { continue; }
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    //为每一条传输通道，建立一个Client客户端对象
                    Client client = new Client();
                    client.remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                    clientMap.put(socketChannel, client);
                    log.info("{}连接成功", socketChannel.getRemoteAddress());
                } else if (key.isReadable()) {
                    processData(key);
                }
                //NIO只会累加，已选择的键的集合不会删除
                keys.remove(key);
            }
        }


    }

    //处理客户端传输的数据
    private void processData(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Client client = clientMap.get(channel);
        int num = 0;
        buffer.clear();
        try {
            while ((num = channel.read(buffer)) > 0) {
                buffer.flip();
                if (Objects.isNull(client.fileName)) {
                    //客户端首先发送过来的是文件名
                    //根据文件名，创建服务器端的文件，将文件通道保存到客户端
                    String fileName = NioConfig.charset.decode(buffer).toString();
                    String destPath = "/Users/dengguoqing/IdeaProjects/bigData/parent/IOAndNetty/src/main/resources/server";
                    File directory = new File(destPath);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    client.fileName = fileName;
                    String fullName = directory.getAbsolutePath() + File.separator + fileName;
                    log.info("NIO 传输目标文件：{}", fullName);
                    File file = new File(fullName);
                    client.outChannel = new FileOutputStream(file).getChannel();
                } else if (0 == client.fileLength) {
                    //客户端发送过来的是文件长度
                    client.fileLength = buffer.getLong();
                    client.startTime = System.currentTimeMillis();
                    log.info("NIO 传输开始...");
                } else {
                    client.outChannel.write(buffer);
                }
                buffer.clear();
            }
            key.channel();
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return;
        }

        if (num == -1) {
            client.outChannel.close();
            log.info("文件上传结束");
            key.cancel();
            log.info("文件接受成功, fileName:{}" + client.fileName);
            log.info("size :{}", client.fileLength);
            long endTime = System.currentTimeMillis();
            log.info("NIO IO传输毫秒数：{}", (endTime - client.startTime));
        }
    }

    /**
     * 服务器端保存的客户对象，对应一个客户端文件
     */
    static class Client {
        /**
         * 文件名称
         */
        String fileName;
        /**
         * 长度
         */
        long fileLength;
        /**
         * 开始传输的时间
         */
        long startTime;
        /**
         * 客户端的地址
         */
        InetSocketAddress remoteAddress;
        /**
         * 输出的文件通道
         */
        FileChannel outChannel;
    }


}
