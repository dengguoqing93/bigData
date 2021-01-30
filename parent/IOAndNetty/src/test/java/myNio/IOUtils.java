package myNio;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author : spiral
 * @since : 2021/1/30 - 下午2:46
 */
public class IOUtils {

    public static void close(Closeable closeable) throws IOException {
        closeable.close();
    }
}
