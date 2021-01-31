### NIO简介

​	核心组件：

+ Channel(通道)
+ Buffer(缓冲区)
+ Selector(选择器)





### 缓冲区（Buffer)



​		通道的读取，就是将数据从通道读取到缓冲区中；通道的写入就是将数据从缓冲区写入到通道中。

​		缓冲区本质上是一个内存块，既可以写入数据，也可以从中读取数据。



#### Buffer类

​		Buffer类是一个非线程安全的类。Buffer类是一个抽象类，对应于Java的主要数据类型，在NIO中主要有8中缓冲区类：ByteBuffer、CharBuffer、DoubleBuffer、FloatBuffer、IntBuffer、LongBuffer、ShortBuffer、MappedByteBuffer。

​		MappedByteBuffer是用于内存映射的一种ByteBuffer类型

​		用的最多的是ByteBuffer二进制字节缓冲区类型。



#### Buffer类的重要属性

​		Buffer类用有一个byte[]数组作为内存缓冲区。有三个重要的成员属性：capacity(容量)、position(读写位置)、limit(读写的限制)。以及一个标记属性：mark

| 属性     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| capacity | 容量，即可以容纳的最大数据量，在缓冲区创建时设置并且不能改变 |
| limit    | 上限，缓冲区当前的数据量                                     |
| position | 位置，缓冲区中下一个要被读或写的元素的索引                   |
| mark     | 调用mark（）方法来设置mark=position，再调用reset()方法可以让position恢复到mark标记的位置即position=mark |



#### Buffer类的重要方法

#####  allocate() 创建缓冲区

​		获取Buffer示例对象需要使用子类的allocate()方法创建。代码如下：

```java
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

		/*
		 * allocate方法测试 
		 */
    @Test
    public void allocateTest() {
        IntBuffer intBuffer = IntBuffer.allocate(20);
        log.info("-----after allocate-----");
        log.info("Position = " + intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }
}

```

输出结果为：

```
11:57:14.185 [main] INFO UserBuffer - -----after allocate-----
11:57:14.191 [main] INFO UserBuffer - Position = 0
11:57:14.191 [main] INFO UserBuffer - limit = 20
11:57:14.192 [main] INFO UserBuffer - capacity = 20
```



##### put()写入到缓冲区

​		用allocate()方法分配内存后、返回了实例对象后，缓冲区实例对象处于写入模式。此时可用put方法将需要写入缓冲区的对象，写入到缓冲区中。put方法只有一个参数，即需要写入缓冲区的对象，需要与缓冲区的类型保持一致。示例如下：

```java
 @Test
    public void allocateTest() {
        IntBuffer intBuffer = IntBuffer.allocate(20);
        log.info("-----after allocate-----");
        log.info("Position = " + intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());

      	//put方法使用
        for (int i = 0; i < 10; i++) {
            intBuffer.put(i);
        }

        log.info("----after put -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }
```



​	输出结果为：

```tex
UserBuffer - ----after put -----
UserBuffer - position = 10 
UserBuffer - limit = 20
INFO UserBuffer - capacity = 20
```



##### flip()翻转

​		该方法是将缓冲区从写入模式转为读取模式。转换规则：

1. 设置可读的长度上限limit。将写入模式下的缓冲区内容的最后写入位置position值，作为读取模式下的limit上限值。
2. 把读的其实位置position的值设为0，表示从头开始读。
3. 清除mark标记，

​		示例代码如下：

```java
/**
     * 测试flip方法
     */
    @Test
    public void flipTest(){
        putTest();
        intBuffer.flip();
        log.info("----after flip -----");
        log.info("position = {} ", intBuffer.position());
        log.info("limit = {}", intBuffer.limit());
        log.info("capacity = {}", intBuffer.capacity());
    }
```

运行结果如下：

```
12:05:39.684 [main] INFO UserBuffer - -----after allocate-----
12:05:39.690 [main] INFO UserBuffer - Position = 0
12:05:39.690 [main] INFO UserBuffer - limit = 20
12:05:39.692 [main] INFO UserBuffer - capacity = 20
12:05:39.693 [main] INFO UserBuffer - ----after put -----
12:05:39.693 [main] INFO UserBuffer - position = 10 
12:05:39.693 [main] INFO UserBuffer - limit = 20
12:05:39.693 [main] INFO UserBuffer - capacity = 20
12:05:39.693 [main] INFO UserBuffer - ----after flip -----
12:05:39.693 [main] INFO UserBuffer - position = 0 
12:05:39.693 [main] INFO UserBuffer - limit = 10
12:05:39.693 [main] INFO UserBuffer - capacity = 20
```



##### get() 从缓冲区读取

get方法，从position的位置读取一个数据，并且进行相应的缓冲区属性调整。

示例代码如下:

```java
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
```

输出结果为：

```
12:22:28.752 [main] INFO UserBuffer - -----after allocate-----
12:22:28.757 [main] INFO UserBuffer - Position = 0
12:22:28.758 [main] INFO UserBuffer - limit = 20
12:22:28.760 [main] INFO UserBuffer - capacity = 20
12:22:28.760 [main] INFO UserBuffer - ----after put -----
12:22:28.760 [main] INFO UserBuffer - position = 10 
12:22:28.761 [main] INFO UserBuffer - limit = 20
12:22:28.761 [main] INFO UserBuffer - capacity = 20
12:22:28.762 [main] INFO UserBuffer - ----after flip -----
12:22:28.762 [main] INFO UserBuffer - position = 0 
12:22:28.762 [main] INFO UserBuffer - limit = 10
12:22:28.762 [main] INFO UserBuffer - capacity = 20
12:22:28.762 [main] INFO UserBuffer - get的数据为：0
12:22:28.763 [main] INFO UserBuffer - get的数据为：1
12:22:28.763 [main] INFO UserBuffer - ----after get 2 int -----
12:22:28.763 [main] INFO UserBuffer - position = 2 
12:22:28.763 [main] INFO UserBuffer - limit = 10
12:22:28.763 [main] INFO UserBuffer - capacity = 20
12:22:28.763 [main] INFO UserBuffer - get的数据为：2
12:22:28.763 [main] INFO UserBuffer - get的数据为：3
12:22:28.763 [main] INFO UserBuffer - get的数据为：4
12:22:28.763 [main] INFO UserBuffer - ----after get 3 int -----
12:22:28.763 [main] INFO UserBuffer - position = 5 
12:22:28.763 [main] INFO UserBuffer - limit = 10
12:22:28.763 [main] INFO UserBuffer - capacity = 20
```



get方法只改变读取位置position的值，不会影响limit的值，当position的值和limit的值相等时，表示数据读取完成。



##### clear() 清空缓冲区



在读取模式下，调用clear()方法将缓冲区切换为写入模式。该方法会将position清零，limit设置为capacity值，可以一直写入直到缓冲区写满。



#### Buffer类的基本步骤

+ 使用创建子类实例的对象的allocate()方法，创建一个Buffer类的实例对象
+ 使用put方法，将数据写入到缓冲区中
+ 写入完成后，在开始读取数据前，调用Buffer.flip()方法，将缓冲区模式转为读模式
+ 调用get方法，从缓冲区读取数据
+ 读取完成后，调用Buffer.clear()或Buffer.compact()方法，将缓冲区转为写入模式



### 通道（Channel)

一个网络连接使用一个通道表示，所有NIO的IO操作都是从通道开始的。通道既可以进行数据读取，也可以进行数据的写入。



#### channle的主要类型

Channle中最重要的是如下四种通道实现：FileChannel、SocketChannel、ServerSoketChannel、DatagramChannel

+ FileChannel：文件通道，用于文件的读写
+ SocketChannel：套接字通道，用于Socket套接字TCP连接的数据读写
+ ServerSocketChannel：服务器套接字通道（或服务器监听通道）允许监听TCP连接请求，为每个监听到的请求，创建一个SocketChannel套接字通道
+ DatagramChannel：数据报通道，用于UDP协议的数据读写



#### FileChannel文件通道

​		FileChannel既可以从一个文件中读取数据，也可以将数据写入到文件中。FileChannel为阻塞模式，不能设置为非阻塞模式

 	FileChannel完成文件复制的实践案例

```java
package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author : spiral
 * @since : 2021/1/30 - 下午2:37
 */
public class FileChannelDemo {

    private static Logger log = LoggerFactory
            .getLogger(FileChannelDemo.class.getSimpleName());

    public static void main(String[] args) throws IOException {
        File srcFile = new File("parent/IOAndNetty/src/main/resources/NioInputFile.txt");
        File destFile = new File("/Users/dengguoqing/IdeaProjects/bigData/parent/IOAndNetty/src/test/resources/NioOutputFile.txt");

        if (!destFile.exists()){
            destFile.createNewFile();
        }

        try (FileInputStream fis = new FileInputStream(
                srcFile); FileOutputStream fos = new FileOutputStream(destFile);
             //获取FileChannel通道
             FileChannel inChannel = fis
                     .getChannel(); FileChannel outChannel = fos.getChannel()) {
            int length = -1;
            //获取一个字节缓冲区
            ByteBuffer buf = ByteBuffer.allocate(1024);
            //读取FileChannel通道，读取数据并写入缓冲区中
            while ((length = inChannel.read(buf)) != -1) {
                log.info("读取的字节数:{}", length);
                //翻转buf,变成读取模式
                buf.flip();
                int outLength = 0;
                //将buf写入到输出的通道
                while ((outLength = outChannel.write(buf)) != 0) {
                    log.info("写入的字节数：{}", outLength);
                }
                buf.clear();
            }
            outChannel.force(true);
        }

    }

}

```



### Selector选择器



​		IO多路复用是指一个进程/线程可以同时监视多个文件描述符（一个网络连接在操作系统底层使用一个文件描述符来表示），一旦其中的一个或者多个文件描述符可读或者可写，系统内核就通知该进程/线程

​		Selector选择器是一个IO事件的查询器。通过选择器一个线程可以查询多个通道的IO事件的就绪状态。

​		选择器和通道的关系，是监控和被监控的关系。一个单线程的选择器可以监控,处理数万的通道（channel)。

通道和选择器之间的关系，通过register的方式完成。调用通道的Channel.register方法，可以将通道注册到一个选择其中。register方法有两个参数：参数一：指定通道注册的选择器实例；参数二：指定选择器要监控的IO时间类型。

​		选择器监控的通道IO时间类型，包括四种：

+ 可读：SelectionKey.OP_READ

+ 可写：SelectionKey.OP_WRITE

+ 连接：SelectionKey.OP_CONNECT

+ 接受：SelectionKey.OP_ACCEPT

  ​	

  ​	如果选择器要监控通道的多种事件，用“按位或“运算符实现。示例如下：

  ```java
  //监控可读可写IO时间
   int key = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
  ```

  IO事件：指通道的某个IO操作的一种就绪状态，表示通道具备完成某个IO操作的条件。如：某个SocketChannel通道，完成了和对端的握手连接，则处于”连接就绪“（OP_CONNECTION)状态。

  ​		一个通道需要被选择器选择和选择，需要继承SelectableChannel类。



##### 选择器的使用流程



+ 获取选择器实例

  选择器实例通过调用静态工厂方法Open()来获取

  + ```java
    Selector selector = Selector.open();
    ```

    

+ 将通道注册到选择器实例

  将管道注册到相应的选择器

  ```java
  				//获取Selector实例
          Selector selector = Selector.open();
          //获取通道
          ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
          //将通道设置为非阻塞
          serverSocketChannel.configureBlocking(false);
          //绑定连接
          serverSocketChannel.bind(new InetSocketAddress(9900));
          //将通道注册到选择器上，并制定监听事件为："接受连接"事件
          serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
  ```

  

+ 选出感兴趣的IO就绪事件

  ​		通过Selector选择器的select()方法，选出已经注册的、已经就绪的IO事件，保存到SelectionKey选择键集合中。SelectionKey集合在选择器实例中，调用selectedKeys()方法，可以获得选择键集合。获得集合后，可以根据具体的IO事件类型，执行对应的业务操作。示例代码如下：

  ```java
  			//轮询，选择需要处理的IO就绪事件
          while (selector.select() > 0) {
              //或许就绪的IO事件集合
              Set<SelectionKey> selectionKeys = selector.selectedKeys();
              //Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
              //遍历就绪事件
              for (SelectionKey selectionKey : selectionKeys) {
                  //根据具体的IO事件类型，执行对应的业务操作
                  if (selectionKey.isAcceptable()) {
                      //ServerSocketChannel服务器监听通道有新连接
                      log.info("ServerSocketChannel服务器监听通道有新连接");
                  } else if (selectionKey.isConnectable()) {
                      log.info("传输通道建立完成");
                  } else if (selectionKey.isReadable()) {
                      log.info("传输通道可读");
                  } else if (selectionKey.isWritable()) {
                      log.info("传输通道可写");
                  }
                  //处理完成后，移除选择键
                  selectionKeys.remove(selectionKey);
              }
          }
  ```

### 使用NIO实现Discard服务器

服务端代码：

```java
package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
 * @author : spiral
 * @since : 2021/1/31 - 上午10:52
 */
public class NioDiscardServer {

    private static Logger log = LoggerFactory.getLogger(NioDiscardServer.class);

    private static void startServer() throws IOException {
        //1.获取选择器
        Selector selector = Selector.open();
        //2. 获取通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //3. 设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //4. 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(8080));
        log.info("服务器启动成功");
        //5. 将通道注册的"接受新连接"IO时间，注册到选择器上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //6. 轮询要处理的IO就绪事件
        while (selector.select() > 0) {
            //7. 获取单个的选择键，并进行处理
            for (SelectionKey key : selector.selectedKeys()) {
                // 8. 判断Key的具体事件类型
                if (key.isAcceptable()) {
                    //9. 当选择键的IO事件是"连接就绪"时间，读取客户端连接
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //10. 切换socketChannel为非阻塞模式
                    socketChannel.configureBlocking(false);
                    //11. 将新连接的通道注册到选择器上，并注册为可读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    //选择键的IO事件是"可读"事件，读取数据
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int length = 0;
                    while ((length = socketChannel.read(byteBuffer)) > 0) {
                        byteBuffer.flip();
                        log.info(new String(byteBuffer.array(), 0, length));
                        byteBuffer.clear();
                    }
                    socketChannel.close();
                }
            }
        }
        serverSocketChannel.close();
    }

    public static void main(String[] args) throws IOException {
        startServer();
    }
}

```

客户端代码：

```java
package org.spiral.myio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author : spiral
 * @since : 2021/1/31 - 上午11:13
 */
public class NioDiscardClient {
    private static Logger log = LoggerFactory.getLogger(NioDiscardClient.class);

    private static void startClient() throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        //1.获取通道
        SocketChannel socketChannel = SocketChannel.open(address);

        //2. 切换为非阻塞模式
        socketChannel.configureBlocking(false);
        //不断自选、等待连接完成，在期间可以做些其他事情
        while (!socketChannel.finishConnect()) {
            log.info("进行服务端连接");
        }
        log.info("连接服务端成功");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello world".getBytes());
        byteBuffer.flip();
        //发送数据到服务器
        socketChannel.write(byteBuffer);
        socketChannel.shutdownOutput();
        socketChannel.close();
    }

    public static void main(String[] args) throws IOException {
        startClient();

    }

}
```



### 使用SocketChannel在服务器端接受文件

服务器端代码：

```

```

