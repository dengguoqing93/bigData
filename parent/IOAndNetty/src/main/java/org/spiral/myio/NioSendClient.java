package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spiral.myio.config.NioConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * NIO 传输文件客户端
 *
 * @author : spiral
 * @since : 2021/1/31 - 下午5:23
 */
public class NioSendClient {

    private static Logger log = LoggerFactory.getLogger(NioSendClient.class);

    public static void main(String[] args) throws IOException {
        NioSendClient client = new NioSendClient();
        client.sendFile();
    }

    /**
     * 向服务器传输文件
     */
    public void sendFile() throws IOException {
        String srcPath = "/Users/dengguoqing/IdeaProjects/bigData/parent/IOAndNetty/src/main/resources/input/";
        String destFile = "NioInputFile.txt";
        File file = new File(srcPath + destFile);
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.socket().connect(new InetSocketAddress(NioConfig.IP, NioConfig.port));
        socketChannel.configureBlocking(false);
        while (!socketChannel.finishConnect()) {
        }
        log.info("client成功连接服务器");
        //发送文件名称
        ByteBuffer fileNameByteBuffer = NioConfig.charset.encode(destFile);
        socketChannel.write(fileNameByteBuffer);
        //发送文件长度
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putLong(file.length());
        buffer.flip();
        socketChannel.write(buffer);
        buffer.clear();
        log.info("开始传输文件");
        int length = 0;
        long progress = 0;
        while ((length = fileChannel.read(buffer)) > 0) {
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
            progress += length;
            log.info("| {}%|", 100 * progress / file.length());
        }
        if (length == -1) {
            fileChannel.close();
            socketChannel.shutdownOutput();
            socketChannel.close();
        }
        log.info("======文件传输成功=======");
    }
}
