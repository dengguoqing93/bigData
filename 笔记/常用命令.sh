##Linux

## 启动hdfs
start-dfs.sh

## 启动yarn
start-yarn.sh

##启动Hbase
satrt-hbase.sh


## kafka命令集

### 启动Kafka
kafka-server-start.sh $KAFKA_HOME/config/server.properties
#建主题
kafka-topics.sh --zookeeper master:2181,slave0:2181,slave2:2181/kafka --create --replication-factor 3 --partitions 1 --topic rtdw
#查主题
kafka-topics.sh --zookeeper master:2181,slave0:2181,slave2:2181/kafka --list
#启动生产者
kafka-console-producer.sh --broker-list master:9092,slave0:9092,slave2:9092 --topic rtdw
#启动消费者
kafka-console-consumer.sh --bootstrap-server master:9092,slave0:9092,slave2:9092 --topic rtdw --from-beginning
#删除主题
kafka-topics.sh --zookeeper master:2181,slave0:2181,slave2:2181/kafka --delete --topic druid1



##hbase重用命令

##建表语句
create 'order_product','f1'
create 'payments','f1'
create 'product_category','f1'
create 'product_info','f1'
create 'shop_admin_org','f1'
create 'shops','f1'
create 'trade_orders','f1'
create 'area','f1'
create 'dim_area','f1'