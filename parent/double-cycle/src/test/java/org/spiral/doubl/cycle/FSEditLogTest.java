package org.spiral.doubl.cycle;

import org.junit.jupiter.api.Test;

/**
 * @author : spiral
 * @since : 2021/1/14 - 上午9:48
 */
class FSEditLogTest {

    @Test
    void logEdit() {
        final FSEditLog fsEditLog = new FSEditLog();

        for (int i = 0; i < 50; i++) {
            new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        fsEditLog.logEdit("日志信息");
                    }

                }
            }).start();

        }
    }
}