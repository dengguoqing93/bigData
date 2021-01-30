package myNio;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

/**
 * Buffer常用类
 *
 * @author : spiral
 * @since : 2021/1/29 - 下午9:28
 */
public class UserBuffer {

    private static Logger log = LoggerFactory.getLogger("UserBuffer");


    private IntBuffer intBuffer = null;

    @Test
    public void allocateTest() {
        intBuffer = IntBuffer.allocate(20);
        log.info("-----after allocate-----");
        log.info("Position = " + intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());


    }

    @Test
    public void putTest() {

        allocateTest();

        for (int i = 0; i < 10; i++) {
            intBuffer.put(i);
        }

        log.info("----after put -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }

    /**
     * 测试flip方法
     */
    @Test
    public void flipTest() {
        putTest();
        intBuffer.flip();
        log.info("----after flip -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }

    /**
     * 测试get方法
     */
    @Test
    public void getTest() {
        flipTest();
        for (int i = 0; i < 2; i++) {
            int out = intBuffer.get();
            log.info("get的数据为：{}", out);
        }

        log.info("----after get 2 int -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());

        for (int i = 0; i < 3; i++) {
            int out = intBuffer.get();
            log.info("get的数据为：{}", out);
        }
        log.info("----after get 3 int -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }

}
