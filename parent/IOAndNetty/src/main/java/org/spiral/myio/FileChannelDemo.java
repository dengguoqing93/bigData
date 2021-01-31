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
import java.util.Set;

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

        //轮询，选择需要处理的IO就绪事件
        while (selector.select() > 0) {
            //或许就绪的IO事件集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            //遍历就绪事件
            for (SelectionKey selectionKey : selectionKeys) {
                //根据具体的IO事件类型，执行对应的业务操作
                if (selectionKey.isAcceptable()) {
                    //ServerSocketChannel服务器监听通道有新连接
                    log.info("ServerSocketChannel服务器监听通道有新连接");
                } else if (selectionKey.isConnectable()) {
                    log.info("传输通道建立完成");
                } else if (selectionKey.isReadable()) {
                    log.info("传输通道可读");
                } else if (selectionKey.isWritable()) {
                    log.info("传输通道可写");
                }
                //处理完成后，移除选择键
                selectionKeys.remove(selectionKey);
            }
        }


        try (FileInputStream fis = new FileInputStream(srcFile); FileOutputStream fos = new FileOutputStream(destFile);
             //获取FileChannel通道
             FileChannel inChannel = fis.getChannel(); FileChannel outChannel = fos.getChannel()) {
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
