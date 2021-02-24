package org.spiral.mynetty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @author : spiral
 * @since : 2021/1/18 - 下午2:28
 */
public class Strategy {
    public static void main(String[] args) {
        ByteBuf heapBuf = new EmptyByteBuf(new PooledByteBufAllocator());
        //检查ByteBuf是否有个支撑数组
        if (heapBuf.hasArray()) {
            //获取数组的引用
            byte[] array = heapBuf.array();
            //计算第一个字节的偏移量
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            //获得可读字节数
            int length = heapBuf.readableBytes();
            //使用数组、偏移量和长度作为参数调用自定义的方法
            //handleArray(array,offset,length);
        }
    }
}
