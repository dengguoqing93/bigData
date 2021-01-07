### Flume概述

#### 	

#### Flume定义

​	Flume是一个分布式、高可靠、高可用的海量日志采集、聚合、传输的系统。是实时采集日志的数据采集引擎。	![page110image8442960.png](/Users/dengguoqing/Library/Application Support/typora-user-images/page110image8442960.png) 

Flume有3个重要组件:Source、Channel、Sink

特点：

+ 分布式：flume分布式集群，拓展性好
+ 可靠性好：当节点出现故障时，日志能被传送到其他节点上，而不会丢失
+ 易用性：flume配置使用繁琐，对使用人员专业技术高
+ 实时采集：flume采集流模式进行数据实时采集

使用场景：使用于日志文件实时采集

#### Flume体系结构

![page112image8571744.png](/Users/dengguoqing/Library/Application Support/typora-user-images/page112image8571744.png) 

Flume的组件：

+ **Agent**本质上是一个JVM进程，该JVM进程控制Event数据流从外部日志生产者传输到目的地（或者下一agent)。一个完整的Agent包含三个组件：Source、Channel和Sink，Source是指数据的来源和方式，Channel是一个数据的缓冲池，Sink定义了数据输出的方式和目的地
+ **Source**是负责接受数据到Flume Agent组件。Source组件可以处理各种类型，各种格式的日志数据，包括Avro,exec,netcat等
+ **Channel**是位于Source和sink之间的缓冲区。Channel允许source和sink运行在不同的速率上。Channel是线程安全的，可以同时处理多个Source的写入和多个Sink的读取操作。常用的Channel包括：
  + Memory Channel：是内存中的队列。memory Channel在允许数据丢失的情况下使用。若数据不允许丢失，则应避免使用该Channel
  + File Channel:将所有事件写到磁盘。因此在程序关闭或机器宕机的情况下不会丢失数据
  + Kafka Channel:将 Event 存储到Kafka集群（必须单独安装）。Kafka提供了高可用性和复制机制，因此如果Flume实例或者 Kafka 的实例挂掉，能保证Event数据随时可用。 Kafka channel可以用于多种场景：
    1. 与source和sink一起：给所有Event提供一个可靠、高可用的channel。
    2. 与source、interceptor一起，但是没有sink：可以把所有Event写入到Kafka的topic中，来给其他的应用使用。
    3. 与sink一起，但是没有source：提供了一种低延迟、容错高的方式将Event发送的各种Sink上，比如：HDFS、HBase、Solr。
+ **Sink**不断轮询Channel中的事件，且批量的移除它们，并将这些事件批量写入到存储或索引系统、或者被发送到另一个flume Agent。Sink是完全事务性的。在Channel批量删除数据前，sink从Channel开启一个事务，批量事件一旦成功写出到存储系统或下一个agent，Sink向channel提交事务，事务提交后Channel将删除内部缓冲区的事件
+ **Event**是Flume定义的一个数据流传输的最小单位



#### Flume的拓扑结构

**串行模式**

​	将多个Flume Agent顺序连接起来，从最初的Source开始到最终的Sink传送的目的存储体统。该模式中任意agent出现问题，会影响整体的传输系统

![两个Agent通过Avro RPC组成了一个多agent流](/Users/dengguoqing/大数据/个人笔记/flume-串行模式.png) 



**复制模式（单source多channel、sink模式）**

​	将事件流向一个或多个目的地。这种模式将数据源复制到多个channel中，每个channel的数据都相同，sink可以选择发送不同的目的地。

​			![A fan-out flow using a (/Users/dengguoqing/大数据/个人笔记/flume-复制模式.png) channel selector](https://flume.liyifeng.org/_images/UserGuide_image01.png) 

**负载均衡模式（单source、channel多sink)**

将多个sink逻辑上分到一个sink组，flume将数据发送到不同的sink，主要解决负载 均衡和故障转移问题。

![page114image8510992.png](/Users/dengguoqing/Library/Application Support/typora-user-images/page114image8510992.png) 

**聚合模式**

这种模式最常见的，也非常实用，日常web应用通常分布在上百个服务器，大者甚 至上千个、上万个服务器。产生的日志，处理起来也非常麻烦。用这种组合方式能 很好的解决这一问题，每台服务器部署一个flume采集日志，传送到一个集中收集日 志的flume，再由此flume上传到hdfs、hive、hbase、消息队列中。

![使用Avro RPC来将所有Event合并到一起的一个扇入流例子](https://flume.liyifeng.org/_images/UserGuide_image02.png)

#### Flume内部原理

```
总体数据流向:Souce => Channel => Sink 
Channel: 处理器、拦截器、选择器
```

![page115image8434272.png](/Users/dengguoqing/Library/Application Support/typora-user-images/page115image8434272.png) 



### 基础应用

fFume提供了多种Source组件来采集数据源。常见的Source:

1. Avro Source：监听Avro端口来接受外部avro客户端的事件流。avro source的源数据必须是经过avro序列化后的数据。利用Avro source可以实现多级流动、扇出流、扇入流等效果。
2. exec source：可以将命令产生的输出作为source。
3. netcat source：一个NetCat Source用来监听一个指定端口，并接受监听到的数据
4. spooling directory source：将指定的文件加入到“自动搜集”目录中，Flume会持续监听该目录，把文件当做source来处理。文件不能修改，且不能重名
5. Taildir Source（1.7）：监控指定的多个文件，一旦文件内有新写入的数据，就会将其写入到指定的sink内，该source可靠性高，不会丢失数据。不会对于跟踪的文件有任何处理，不会重命名也不会删除，不做任何修改，不支持二进制文件，支持一行一行的读取文本文件



使用channel组件来缓存日志数据。常见的channel组件：

1. memory channel：缓存到内存中（最常用）
2. file channel：缓存到文件中
3. JDBC channel:通过JDBC缓存到关系型数据库中
4. kafka channel：缓存到Kafka中

使用Sink组件来保存数据。常见的Sink组件：

1. logger sink：将信息显示在标准输出上，主要用于测试
2. Avro Sink ：Flume Events发送到sink,转化为Avro events,并发送配置好的hostname/port。从配置好的channel按照配置好的大小批量获取events
3. null sink : 将接收到的events全部丢弃
4. HDFS sink：将events写进到HDFS。支持创建文本和序列文件，支持两种文件类型压缩。文件可以基于数据的经过时间、大小、事件的数量周期性地滚动
5. Hive Sink: 该Sink stream将包含分割文本或者json数据的events直接传送到Hive表或分区中。使用Hive事务写Events。当一系列Events提交到Hive时，他们可以被Hive查询到
6. Hbase sink：保存到HBase中
7. Kafka sink:保存到Kafka中

