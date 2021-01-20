package org.spiral.mynetty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

/**
 * 服务端Handler实现操作
 *
 * @author : spiral
 * @since : 2021/1/17 - 下午1:54
 */
//该注解表示一个ChannelHandler可以被多个Channel安全地共享
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 该方法对于每个传入的消息都要调用
     *
     * @param ctx 上下文信息
     * @param msg 传入的消息
     * @throws Exception 异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println(
                "server received: " + in.toString(Charset.forName("UTF-8")));
        ctx.write(in);
    }

    /**
     * 将未决的消息冲刷到远程节点，并且关闭该Channel
     *
     * @param ctx 上下文
     * @throws Exception 异常
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
           .addListener(ChannelFutureListener.CLOSE);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
