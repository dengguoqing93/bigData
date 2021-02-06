package org.spiral.reactor.model;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 * 单线程Reactor模式实现案例
 *
 * @author : spiral
 * @since : 2021/2/1 - 下午9:21
 */
public class Reactor implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverSocket;

    public Reactor() throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();

        serverSocket.configureBlocking(false);

        //注册ServerSocket的accept事件
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        sk.attach(new AcceptorHandler());
    }

    @Override
    public void run() {
        //选择器轮询
        while (!Thread.interrupted()) {
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> selected = selector.selectedKeys();
            for (SelectionKey selectionKey : selected) {
                dispatch(selectionKey);
            }
            selected.clear();
        }
    }

    private void dispatch(SelectionKey selectionKey) {
        Runnable handler = (Runnable) selectionKey.attachment();
        if (handler != null) {
            handler.run();

        }
    }

    class AcceptorHandler implements Runnable {
        @Override
        public void run() {
            //接受新连接，为新链接创建一个输入输出的handler处理器
        }
    }
}
