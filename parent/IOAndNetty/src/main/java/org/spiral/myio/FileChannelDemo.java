package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * @author : spiral
 * @since : 2021/1/30 - 下午2:37
 */
public class FileChannelDemo {

    private static Logger log = LoggerFactory
            .getLogger(FileChannelDemo.class.getSimpleName());

    public static void main(String[] args) throws IOException {
        File srcFile = new File(
                "parent/IOAndNetty/src/main/resources/NioInputFile.txt");
        File destFile = new File(
                "/Users/dengguoqing/IdeaProjects/bigData/parent/IOAndNetty/src/test/resources/NioOutputFile.txt");

        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        //获取Selector实例
        Selector selector = Selector.open();
        //获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //将通道设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //绑定连接
        serverSocketChannel.bind(new InetSocketAddress(9900));
        //将通道注册到选择器上，并制定监听事件为："接受连接"事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


        try (FileInputStream fis = new FileInputStream(
                srcFile); FileOutputStream fos = new FileOutputStream(destFile);
             //获取FileChannel通道
             FileChannel inChannel = fis
                     .getChannel(); FileChannel outChannel = fos.getChannel()) {
            int length = -1;
            //获取一个字节缓冲区
            ByteBuffer buf = ByteBuffer.allocate(1024);
            //读取FileChannel通道，读取数据并写入缓冲区中
            while ((length = inChannel.read(buf)) != -1) {
                log.info("读取的字节数:{}", length);
                //翻转buf,变成读取模式
                buf.flip();
                int outLength = 0;
                //将buf写入到输出的通道
                while ((outLength = outChannel.write(buf)) != 0) {
                    log.info("写入的字节数：{}", outLength);
                }
                buf.clear();
            }
            outChannel.force(true);
        }

    }

}
