package org.spiral.mynetty.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * 客户端ChannelHandler
 *
 * @author : spiral
 * @since : 2021/1/17 - 下午2:38
 */
@ChannelHandler.Sharable
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //当被通知channel是活跃的时候，发送一条消息
        ctx.writeAndFlush(
                Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
            throws Exception {
        //记录已接收的转储
        System.out
                .println("Client received: " + msg.toString(CharsetUtil.UTF_8));
    }

    //在发生异常时，记录错误并关闭channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
