package org.spiral.doubl.cycle;

import java.util.LinkedList;

/**
 * 模拟Hdfs的双缓存编辑FSEdit
 *
 * @author : spiral
 * @since : 2021/1/14 - 上午9:21
 */
public class FSEditLog {
    private long txid = 0L;
    private DoubleBuffer editLogBuffer = new DoubleBuffer();
    //当前是否正在往磁盘里面刷写数据
    private volatile Boolean isSyncRunning = false;
    private volatile Boolean isWaitSync = false;

    private volatile Long syncMaxTxid = 0L;
    /**
     * 一个线程 就会有自己一个ThreadLocal的副本
     */
    private ThreadLocal<Long> localTxid = new ThreadLocal<Long>();

    /**
     * 写lodEdit
     *
     * @param content 写入数据内容
     */
    public void logEdit(String content) {
        //加锁
        synchronized (this) {
            txid++;
            localTxid.set(txid);
            EditLog editLog = new EditLog(txid, content);
            editLogBuffer.write(editLog);
        }

        logSync();
    }

    private void logSync() {
        synchronized (this) {
            if (isSyncRunning) {
                long txid = localTxid.get();
                if (txid <= syncMaxTxid) {
                    return;
                }
                if (isWaitSync) {
                    return;
                }
                isWaitSync = true;

                while (isSyncRunning) {
                    try {
                        wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                isWaitSync = false;
            }
            editLogBuffer.setReadyToSync();

            if (editLogBuffer.syncBuffer.size() > 0) {
                syncMaxTxid = editLogBuffer.getSyncMaxTxid();
            }
            isSyncRunning = true;
        }

        /*
         * 分段加锁
         * 线程一 刷写数据
         * 这个过程要稍微慢一些，因为往磁盘上面去写数据。
         * 线程一就会在这儿运行一会儿。
         */
        //50ms
        editLogBuffer.flush();

        //重新加锁
        synchronized (this) {
            //线程一 赋值为false
            isSyncRunning = false;
            //唤醒等待线程。
            notify();
        }
    }

    /**
     * 日志存储对象
     */
    static class EditLog {
        Long txId;
        String content;

        public EditLog(long txId, String content) {
            this.txId = txId;
            this.content = content;
        }

        @Override
        public String toString() {
            return "EditLog{" + "txId=" + txId + ", content='" + content + '\'' + '}';
        }
    }

    /**
     * 双缓存类
     */
    class DoubleBuffer {
        private LinkedList<EditLog> currentBuffer = new LinkedList<EditLog>();
        private LinkedList<EditLog> syncBuffer = new LinkedList<EditLog>();

        /**
         * 写数据
         *
         * @param editLog 写入的数据
         */
        public void write(EditLog editLog) {
            currentBuffer.add(editLog);
        }

        /**
         * 内存交换
         */
        public void setReadyToSync() {
            LinkedList<EditLog> tmp = currentBuffer;
            currentBuffer = syncBuffer;
            syncBuffer = tmp;
        }

        public Long getSyncMaxTxid() {
            return syncBuffer.getLast().txId;
        }

        public void flush() {
            for (EditLog editLog : syncBuffer) {
                System.out.println(editLog);
            }
            syncBuffer.clear();
        }
    }

}


