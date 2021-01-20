package org.spiral.myhadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * @author : spiral
 * @since : 2021/1/20 - 上午10:03
 */
public class File {
    public static void main(String[] args) throws IOException {
        FileSystem fileSystem = FileSystem.get(new Configuration());

        FSDataOutputStream fsDataOutputStream = fileSystem
                .create(new Path("/"));
        fsDataOutputStream.write(2);

    }
}
