# Netty

## Netty特性

+ 设计
  + 统一的API支持多种传输类型，阻塞和非阻塞的
  + 简单而强大的线程模型
  + 真正的无连接数据报套接字支持
  + 连接逻辑组件以支持复用
+ 易于使用
+ 高性能
  + 拥有比Java的核心API更高的吞吐量以及更低的延迟
  + 池化和复用，拥有更低的资源消耗
  + 最少的内存复制
+ 健壮性
  + 不会因慢速、快速或者超载的连接而导致OutOfMemoryerror
  + 消除在高速网络中NIO应用程序常见的不公平读写/写比率
+ 安全性
  + 完整的SSL和TLS以及StartTLS支持
  + 可用于受限环境



## Netty的核心组件

+ Channel
  + 代表一个到实体的开放连接，如度操纵和写操作
+ 回调
  + 一个指向已经被提供给另外一个方法的方法的信用
+ Future
  + 提供了另一种在造作完成时通知应用程序的方式。这个对象可以看做是一个异步操作的结果的占位符；它将在未来的某个时刻完成，并提供对其结果的访问
+ 事件和ChannelHandler



### ChannelHandler

+ 针对不同类型的事件来调用ChannelHandler
+ 应用程序通过实现或者拓展ChannelHandler来挂钩到事件的声明周期，并且提供自定义的应用程序逻辑
+ 在架构上，ChannelHanler有助于保持业务逻辑与网络处理代码的分离。简化了开发过程

### 引导服务器（ServerBootStrap)



引导服务器：

+ 绑定到服务器将在其上监听并接受传入连接请求的端口
+ 配置Channel，将有关的入站消息通知给EchoServerHandler实例





## Netty的组件和设计



Netty网络抽象的代表：

+ Channel——Socket
+ EventLoop——控制流、多线程处理、并发
+ ChannelFuture——异步通知



### Channel接口

​		基本的I/O操作（bind()、connect()、read()和write() ）依赖于底层网络传输提供的原语。Netty提供的Channel接口提供的API大大降低了直接使用Socket类的复杂性。Channel拥有许多预定义的专门化实现的广泛类层次结构的根。

![Channel](./Channel.png)



### EventLoop接口

EventLoop用于处理连接的生命周期中所发生的事件。

![关系图](./Channel-EventLoop-EventLoopGroup.png)

关系：

+ 一个EventLoopGroup包含一个或者多个EventLoop
+ 一个EventLoop在它的生命周期内只和一个Thread绑定
+ 所有由EventLoop处理的I/O事件都将在它专有的Thread上被处理
+ 一个Channel在它的声明周期内只注册于一个EventLoop
+ 一个EventLoop可能会被分配给一个或多个Channel



### ChannelHandler接口

该接口充当了多有处理入站和出站数据的应用逻辑的容器。因为ChannelHandler的方法是由网络时间触发



### ChannelPipeline接口

​		ChannelPipeline为ChannelHandler链提供了容器，并定义了用于在该链上传播入站和出站事件流的API。当channel被创建时，它会被自动地分配到它专属的ChannelPipline。

​		ChannelHandler安装到ChannelPipline中的过程：

+ 一个ChannelInitializer的实现被注册到了ServerBootstrap中
+ 当ChannelInitializer.initChannel()方法被调用时，ChannelInitializer将在ChannelPipeline中安装一组自定义的ChannelHandler
+ ChannelInitializer将它自己从ChannelPipline中移除



![pipeline](./包含入站和出站ChannelHandler的ChannelPipeline.png)

### BootStrap

​		Netty的BootStrap类为应用程序的网络层配置提供了容器，这设计将一个进程绑定到某个指定的端口，或者将一个进程连接到另一个运行在某个指定主机和指定端口的进程。有两种类型的BootStrap: 一种用于客户端（简称为BootStrap)，一种用于服务器(ServerBootStrap)

|           类别           |      BootStrap       | ServerBootStrap  |
| :------------------: | :------------------: | :--------------: |
| 网络编程中的作用     | 连接到远程主机和端口 | 绑定一个本地端口 |
| EventLoopGroup的数目 |          1           |        2         |





## ByteBuf

ByteBuf是Netty的数据容器

ByteBuf API的优点：

+ 可以被用户自定义的缓冲区类型拓展
+ 通过内置的复合缓冲区类型实现了透明的零拷贝
+ 容量可以按需增长
+ 在读和写这两种模式之间切换不需要调用ByteBuffer的flip()方法
+ 读和写使用了不同的索引
+ 支持方法链的调用
+ 支持引用计数
+ 支持池化



### 工作原理

​		ByteBuf维护了两个不同的索引：一个用于读取，一个用于写入。名称以read或则write开头的ByteBuf方法，将会推进其对应的索引，而名称以set或者get开头的操作不会



### 使用方式

#### 	堆缓冲区

​		支撑数组：将数据存储在JVM的对空间中。可以在没有使用池化的情况下提供快速的分配和释放，适用于有遗留的数据需要处理的情况。代码如下:

```java
				ByteBuf heapBuf = ...;
        //检查ByteBuf是否有个支撑数组
        if (heapBuf.hasArray()) {
            //获取数组的引用
            byte[] array = heapBuf.array();
            //计算第一个字节的偏移量
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            //获得可读字节数
            int length = heapBuf.readableBytes();
            //使用数组、偏移量和长度作为参数调用自定义的方法
            handleArray(array,offset,length);
        }
```

#### 直接缓冲区

#### 复合缓冲区



