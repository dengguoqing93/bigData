### 流式构建



#### 准备数据源

​		从Kafka消费消息，每条消息都需要包含：维度信息、度量信息、业务时间戳。

​		每条消息的数据结构都应该相同，并且可以用同一个分析器将每条消息中的维度、度量和时间戳信息提取出来。目前默认的分析器为：org.apache.kylin.source.kafka.TimedJsonStreamParser

```shell
# 创建名为kylin_streaming_topic的topic，有三个分区
kafka-topics.sh --create --zookeeper master:2181/kafka --replication-factor 1 --partitions 3 --topic kylin_streaming_topic
# 使用工具，每秒会向以上topic每秒发送100条记录
kylin.sh org.apache.kylin.source.kafka.util.KafkaSampleProducer --topic kylin_streaming_topic --broker master:9092,slave0:9092
# 检查消息是否成功发送
kafka-console-consumer.sh --bootstrap-server master:9092,slave0:9092 --topic kylin_streaming_topic1 --from-beginning
```

