# Kafka架构设计

## Kafak核心概念

+ 生产者(Producer) ： 生产数据，并存放在指定的主题中
+ 主题(topic) ：类似于数据库中的表，按主题存储数据
+ 分区(partition) ：一个主题可以按分区存储数据，默认数据只有一个分区，一般为创建topic时指定分区数。在磁盘上体现为目录不同
+ 消费者(consumer)  ： 消费存储在主题中的数据,数据是通过pull的方式，实现消费者消费数据，即消费者自己从topic中拉取
+ 消费者组(consumer group) ： 一组消费者，消费同一的主题
+ Broker： 一个kafka节点称为一个Broker，集群内的BrokerID唯一
+ 副本（replica）：一份数据有多个副本
+ 消息（message）：存放的数据
+ 偏移量（offset）：消息处理的位置

![Kafka核心概念](./Kafka核心概念.png)



## Kafka高性能

+ 通过顺序写的方式实现kafka高性能的写
+ 通过零拷贝的方式实现Kafka高性能的读



num.network.threads = 9

num.io.threads =  32

+ kafka服务端的设计
  + 服务端如何处理请求
    + 使用NIO
    + 顺序读写
    + 跳表设计
    + 零拷贝
+  Producer设计
  + 批处理
  + 内存池设计
  + 封装同一服务器请求
+ ConsumerGroup设计
  + 同一个消费组是P2P方式，一个消息只能被同一个组的一个消费者消费
  + 不同组时订阅模式，一个消息可以被不同的消费组消费
  + 一个分区同一时间只会被同组一个消费者消费
+ Consumer设计-偏移量存储
  + 0.8以前存储在Zookeeper中，0.8以后存储在Kafka的_consumer_offset的主题中





## Kafka的网络设计

​	 基于NIO的网络设计

​	 消息写入到磁盘之前首先放在一个MessageQueue中，再用多线程处理MessageQueue中

 	有多个Selector设计 	

​	

​	一个Acceptor,负责启动Kafka并监听应用

   三个Processor负责接收请求

​	一个RequestChanel封装RequestQueue负责接收请求

​	一个KafkaRequestHandlerPool监听RequestChannel，当RequestChannle中有数据流入时，该进程负责处理数据，并最中放入磁盘。默认有8个线程进行数据处理

​	RequestChannle还封装三个ReposeChannle负责接收响应



![网络设计](服务端设计.png)





### Kafka架构思考

​	kafka是一个高性能，高并发，高可用的消息系统，分别通过以下设计实现上述特性

+ 高可用：通过副本机制保证了Kafka的高可用
+ 高并发：利用Java的NIO实现网络连接以及数据读写，并设计多Selector接受数据，多线程处理的设计方式保证高可用，可以通过修改制定参数定制化Selector的个数，以及处理数据的线程数。通过配置这两个参数可以提升吞吐量
+ 高性能：
  + 生产者
    + 顺序写的方式保证写高性能
    + 批量处理设计
  + 消费者
    + 零拷贝的方式读取数据
    + 索引，以及跳表的设计