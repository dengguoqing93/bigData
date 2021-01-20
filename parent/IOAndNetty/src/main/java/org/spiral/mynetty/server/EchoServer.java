package org.spiral.mynetty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Echo服务端代码实现
 *
 * @author : spiral
 * @since : 2021/1/17 - 下午2:14
 */
public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + EchoServer.class.getSimpleName() + "<port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();

    }

    private void start() throws InterruptedException {
        final EchoServerHandler serverHandler = new EchoServerHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             //指定使用的NIO传输channel
             .channel(NioServerSocketChannel.class)
             //使用之地当的端口设置套接字地址
             .localAddress(new InetSocketAddress(port))
             //添加一个EchoServerHandler到子Channel的channelPipeLine
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 protected void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(serverHandler);
                 }
             });
            //异步地绑定服务器；调用sync()方法阻塞等待直到绑定完成
            ChannelFuture f = b.bind().sync();
            //获取Channel的CloseFuture,并且阻塞当前线程直到它完成
            f.channel().closeFuture().sync();
        } finally {
            //关闭EventLoopGroup，释放所有的资源
            group.shutdownGracefully().sync();
        }
    }
}
