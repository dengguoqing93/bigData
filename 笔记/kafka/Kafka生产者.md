# Kafka的源码解读（一）-- 生产者

​	该文档及之后的的kafka源码解读均以kafka2.4.0版本进行解读。kafka是用NIO作为通信基础的，这里不做赘述，如有需要连接NIO基础的课参考以下链接：

https://editor.csdn.net/md/?articleId=113486103

## 生产者发送数据流程解读

​	生产者发送消息的流程简图如下：

![发送消息简图](/Users/dengguoqing/IdeaProjects/bigData/笔记/kafka/kafka生产者消息概述.png)

发送消息的简要流程如下：

+ 生产者接受到消息
+ 将消息封装为ProducerRecord
+ 将消息序列化
+ 获取消息的分区信息：如果没有元数据，需要去获取元数据
+ 将消息放入缓存区
+ sender读取缓存区的消息，并进行batch封装
+ 满足batch发送的消息，将消息发送给Broker

发送消息核心流程剖析图如下：

![kafka生产者发送消息的核心流程图](/Users/dengguoqing/IdeaProjects/bigData/笔记/kafka/kafka发送消息核心流程剖析.png)

Kafpa源码注释翻译如下：

```
用于将消息发布到Kafka集群的Kafka生产者客户端。
生产者是线程安全的，因此可以多个线程共享一个生产者示例，且通常情况下多线程共享同一个实例要比每个线程一个实例性能要高。
下面是一个包含键/值对序列化器的使用案例：
 Properties props = new Properties();
 props.put("bootstrap.servers", "localhost:9092");
 props.put("acks", "all");
 props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
 props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
 Producer<String, String> producer = new KafkaProducer<>(props);
 for (int i = 0; i < 100; i++)
     producer.send(new ProducerRecord<String, String>("my-topic", Integer.toString(i), Integer.toString(i)));
 producer.close();
 
生产者由一个缓冲空间池(RecordAccumulator)和一个I/O后台线程（Sender）组成，缓存空间保存尚未传输到服务器的消息，Sender线程负责将这些消息转换为请求（batch）并将它们发送到集群。 当生产者close失败的时候，这些消息会丢失。
		send()方法是异步的。 调用时，它将消息添加到暂挂记录发送和立即返回的缓冲区中。 这样生产者可以将消息通过批处理来提高效率。
		acks参数控制用于确定请求完成的条件。 我们指定的“all”设置是指将消息同步到所有分区，这是最慢但最安全的设置。
		如果请求失败，生产者可以自动重试，虽然我们将retries指定为0，但仍然会进行重试。 启用重试也将打开重复的可能性（有关详细信息，请参阅有关邮件传递语义 http://kafka.apache.org/documentation.html#semantics的文档）。
			生产者为每个分区维护未发送消息的缓冲区（一个分区一个Dequeue）。 这些缓冲区的大小由batch.size配置指定。 增大它可以导致更多的批处理，但是需要更多的内存（因为我们通常会为每个活动分区使用一个缓冲区）。
默认情况下，即使缓冲区中还有其他未使用的空间，缓冲区也可以立即发送。 如果想要减少请求数，可以将linger.ms设置为大于0的值，该参数生产者在发送请求之前等待该毫秒数，以希望会有更多记录来填充。这类似于Nagle在TCP中的算法。 例如，在上面的代码段中，由于我们将延迟时间设置为1毫秒，因此很可能所有100条记录都将在一个请求中发送。 但是，如果我们没有填充缓冲区，此设置将为我们的请求增加1毫秒的延迟，以等待更多记录到达。 请注意，时间接近的记录通常即使linger.ms=0也作为一个批次处理，因此在高负载下将进行批处理，与linger配置无关。 但是，如果不将其设置为大于0的值，则在不处于最大负载的情况下，可能会导致更少的请求和更有效的请求，但以少量的等待时间为代价。
		buffer.memory控制生产者缓冲区的内存大小。如果消息的发送速度超过了将消息发送到服务器的速度，则该缓冲区空间将被耗尽。当缓冲区空间用尽时，其他发送调用将阻塞。阻塞时间的阈值由max.block.ms确定，此阈值之后将引发TimeoutException。
		key.serializer和value.serializer指示如何将用户通过其ProducerRecord提供的键和值对象转换为字节。 您可以将包含的org.apache.kafka.common.serialization.ByteArraySerializer或org.apache.kafka.common.serialization.StringSerializer用于简单的字符串或字节类型。
		从Kafka 0.11开始，KafkaProducer支持两种附加模式：幂等生产者和事务生产者。 幂等的生成器将Kafka的交付语义从至少一次交付增强到恰好一次交付。 特别是，生产者重试将不再引入重复项。 事务产生器允许应用程序原子地将消息发送到多个分区(partition)和主题（topic）
		要启用幂等，必须将enable.idempotence参数设置为true。如果启用幂等，则retries配置将默认为Integer.MAX_VALUE，而acks配置将默认为all。幂等生产者没有API更改，因此无需修改现有应用程序即可利用此功能。
为了利用幂等生成器，必须避免重新发送应用程序级别，因为这些应用程序无法重复删除。 因此，如果应用程序启用幂等性，建议将retries配置保留为未设置状态，因为它将默认为Integer.MAX_VALUE 。 此外，如果send(ProducerRecord)即使无限次重试也返回错误（例如，如果消息在发送之前已在缓冲区中过期），则建议关闭生产者并检查最后产生的消息的内容，以确保不能重复。 最后，生产者只能保证在单个会话中发送的消息具有幂等性。
		要使用事务性生产者和附带的API，必须设置transactional.id参数属性。如果设置了transactional.id ，则会自动启用幂等性以及幂等性所依赖的生产者配置。此外交易中包含的主题应配置为具有持久性。特别是， replication.factor应该至少为3 ，并且这些主题的min.insync.replicas应该设置为2。最后，为了从端到端实现事务保证，必须使使用者配置为仅读取已提交的消息。transactional.id的目的是启用单个生产者实例的多个会话之间的事务恢复。 它通常从分区的有状态应用程序中的分片标识符派生。 这样，它对于分区应用程序中运行的每个生产者实例都应该是唯一的。所有新的事务性API都将被阻止，并且将在失败时引发异常。 下面的示例说明了如何使用新的API。 除了所有100条消息都是单个事务的一部分外，它与上面的示例相似。

 Properties props = new Properties();
 props.put("bootstrap.servers", "localhost:9092");
 props.put("transactional.id", "my-transactional-id");
 Producer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
 producer.initTransactions();
 try {
     producer.beginTransaction();
     for (int i = 0; i < 100; i++)
         producer.send(new ProducerRecord<>("my-topic", Integer.toString(i), Integer.toString(i)));
     producer.commitTransaction();
 } catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
     // We can't recover from these exceptions, so our only option is to close the producer and exit.
     producer.close();
 } catch (KafkaException e) {
     // For all other exceptions, just abort the transaction and try again.
     producer.abortTransaction();
 }
 producer.close();
  

如示例所示，每个生产者只能有一个未完成的事务。 在beginTransaction()和commitTransaction()调用之间发送的所有消息都将成为单个事务的一部分。 指定transactional.id ，生产者发送的所有消息都必须是事务的一部分。
事务性生产者使用异常来传达错误状态。 特别是，不需要为producer.send()指定回调或在返回的Future上调用.get() ：如果在producer.send()任何producer.send()或事务调用遇到不可恢复的错误，都将抛出KafkaException 。交易。 有关从事务性发送中检测错误的更多详细信息，请参见send(ProducerRecord)文档。通过在接收到KafkaException调用producer.abortTransaction() ，我们可以确保将任何成功的写入标记为已中止，从而保留事务保证。
	

```





## 生产者初始化源码解析

kafka对外提供了如下四个构造器:

+ 参数封装为Map传入

+ ```java
   public KafkaProducer(final Map<String, Object> configs) {
          this(configs, null, null, null, null, null, Time.SYSTEM);
   }
  ```

+ 其他参数封装为map,序列化器单独传入

+ ```java
      public KafkaProducer(Map<String, Object> configs, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
          this(configs, keySerializer, valueSerializer, null, null, null, Time.SYSTEM);
      }
  ```

+ 参数封装为property传入

+ ```java
      public KafkaProducer(Properties properties) {
          this(propsToMap(properties), null, null, null, null, null, Time.SYSTEM);
      }
  ```

+ 序列化器单独传入，其他参数封装为property

+ ```java
      public KafkaProducer(Properties properties, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
          this(propsToMap(properties), keySerializer, valueSerializer, null, null, null,
                  Time.SYSTEM);
      }
  ```

所有上述构造器，都是通过调用如下构造器完成生产者初始化：

```java
KafkaProducer(Map<String, Object> configs,
                  Serializer<K> keySerializer,
                  Serializer<V> valueSerializer,
                  ProducerMetadata metadata,
                  KafkaClient kafkaClient,
                  ProducerInterceptors interceptors,
                  Time time) 
```

下面分析该方法内容：

```java
 KafkaProducer(Map<String, Object> configs,
                  Serializer<K> keySerializer,
                  Serializer<V> valueSerializer,
                  ProducerMetadata metadata,
                  KafkaClient kafkaClient,
                  ProducerInterceptors interceptors,
                  Time time) {
        /**
         * 如果参数中传入了键值的序列化器，这里进行序列化器配置
         */
        ProducerConfig config = new ProducerConfig(ProducerConfig.addSerializerToConfig(configs, keySerializer,
                valueSerializer));
        try {
            /**
             * 获取配置参数
             */
            Map<String, Object> userProvidedConfigs = config.originals();
            this.producerConfig = config;
            this.time = time;

            //获取事务ID
            String transactionalId = userProvidedConfigs.containsKey(ProducerConfig.TRANSACTIONAL_ID_CONFIG) ?
                    (String) userProvidedConfigs.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG) : null;

            //获取clientId
            this.clientId = buildClientId(config.getString(ProducerConfig.CLIENT_ID_CONFIG), transactionalId);

            //日志配置
            LogContext logContext;
            if (transactionalId == null)
                logContext = new LogContext(String.format("[Producer clientId=%s] ", clientId));
            else
                logContext = new LogContext(String.format("[Producer clientId=%s, transactionalId=%s] ", clientId, transactionalId));
            log = logContext.logger(KafkaProducer.class);
            log.trace("Starting the Kafka producer");
            /*
            测量工具
             */
            Map<String, String> metricTags = Collections.singletonMap("client-id", clientId);
            MetricConfig metricConfig = new MetricConfig().samples(config.getInt(ProducerConfig.METRICS_NUM_SAMPLES_CONFIG))
                    .timeWindow(config.getLong(ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG), TimeUnit.MILLISECONDS)
                    .recordLevel(Sensor.RecordingLevel.forName(config.getString(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG)))
                    .tags(metricTags);
            List<MetricsReporter> reporters = config.getConfiguredInstances(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG,
                    MetricsReporter.class,
                    Collections.singletonMap(ProducerConfig.CLIENT_ID_CONFIG, clientId));
            reporters.add(new JmxReporter(JMX_PREFIX));
            this.metrics = new Metrics(metricConfig, reporters, time);
            //分区器，若没有配置，使用DefaultPartitioner作为分区器
            this.partitioner = config.getConfiguredInstance(ProducerConfig.PARTITIONER_CLASS_CONFIG, Partitioner.class);
            //发送失败后，多久后重试
            long retryBackOffMs = config.getLong(ProducerConfig.RETRY_BACKOFF_MS_CONFIG);
            //获取键值序列化类
            if (keySerializer == null) {
                this.keySerializer = config.getConfiguredInstance(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                                                                         Serializer.class);
                this.keySerializer.configure(config.originals(), true);
            } else {
                config.ignore(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
                this.keySerializer = keySerializer;
            }
            if (valueSerializer == null) {
                this.valueSerializer = config.getConfiguredInstance(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                                                                           Serializer.class);
                this.valueSerializer.configure(config.originals(), false);
            } else {
                config.ignore(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
                this.valueSerializer = valueSerializer;
            }

            // load interceptors and make sure they get clientId，加载拦截器
            userProvidedConfigs.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
            ProducerConfig configWithClientId = new ProducerConfig(userProvidedConfigs, false);
            List<ProducerInterceptor<K, V>> interceptorList = (List) configWithClientId.getConfiguredInstances(
                    ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, ProducerInterceptor.class);
            if (interceptors != null)
                this.interceptors = interceptors;
            else
                this.interceptors = new ProducerInterceptors<>(interceptorList);
            ClusterResourceListeners clusterResourceListeners = configureClusterResourceListeners(keySerializer,
                    valueSerializer, interceptorList, reporters);
            //单个请求的最大大小，默认值的大小是1M.
            this.maxRequestSize = config.getInt(ProducerConfig.MAX_REQUEST_SIZE_CONFIG);
            //该值是缓存大小，默认是32M.一般够用，根据实际情况看是否需要修改
            this.totalMemorySize = config.getLong(ProducerConfig.BUFFER_MEMORY_CONFIG);
            //设置压缩格式
            this.compressionType = CompressionType.forName(config.getString(ProducerConfig.COMPRESSION_TYPE_CONFIG));

            this.maxBlockTimeMs = config.getLong(ProducerConfig.MAX_BLOCK_MS_CONFIG);
            /**
             * 事务管理器
             */
            this.transactionManager = configureTransactionState(config, logContext, log);
            int deliveryTimeoutMs = configureDeliveryTimeout(config, log);

            this.apiVersions = new ApiVersions();
            //缓存器初始化
            this.accumulator = new RecordAccumulator(logContext,
                    config.getInt(ProducerConfig.BATCH_SIZE_CONFIG),
                    this.compressionType,
                    lingerMs(config),
                    retryBackOffMs,
                    deliveryTimeoutMs,
                    metrics,
                    PRODUCER_METRIC_GROUP_NAME,
                    time,
                    apiVersions,
                    transactionManager,
                    //内存池
                    new BufferPool(this.totalMemorySize, config.getInt(ProducerConfig.BATCH_SIZE_CONFIG), metrics, time, PRODUCER_METRIC_GROUP_NAME));
            List<InetSocketAddress> addresses = ClientUtils.parseAndValidateAddresses(
                    config.getList(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG),
                    config.getString(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG));
            //初始化原数据信息，但是并不真正的加载元数据
            if (metadata != null) {
                this.metadata = metadata;
            } else {
                this.metadata = new ProducerMetadata(retryBackOffMs,
                        config.getLong(ProducerConfig.METADATA_MAX_AGE_CONFIG),
                        logContext,
                        clusterResourceListeners,
                        Time.SYSTEM);
                this.metadata.bootstrap(addresses, time.milliseconds());
            }
            this.errors = this.metrics.sensor("errors");
            //sender,真正发送数据的线程
            this.sender = newSender(logContext, kafkaClient, this.metadata);
            String ioThreadName = NETWORK_THREAD_PREFIX + " | " + clientId;
            this.ioThread = new KafkaThread(ioThreadName, this.sender, true);
            this.ioThread.start();
            config.logUnused();
            AppInfoParser.registerAppInfo(JMX_PREFIX, clientId, metrics, time.milliseconds());
            log.debug("Kafka producer started");
        } catch (Throwable t) {
            // call close methods if internal objects are already constructed this is to prevent resource leak. see KAFKA-2121
            close(Duration.ofMillis(0), true);
            // now propagate the exception
            throw new KafkaException("Failed to construct kafka producer", t);
        }
    }
```

​		生产者虽然有加载元数据的代码，但是这里并没有真正的建立和kafka集群的联系，真正加载元数据是在第一个消息发送时，获取分区信息时加载的。

​		以上代码中，最重要的部分是，缓冲区的初始化，以及sender线程的启动。

### 缓冲区初始化

缓冲区是用来存放已经生产但是还没有发送给kafka集群的信息。缓冲区初始化代码如下：

```java
this.accumulator = new RecordAccumulator(logContext,
                    config.getInt(ProducerConfig.BATCH_SIZE_CONFIG),
                    this.compressionType,
                    lingerMs(config),
                    retryBackOffMs,
                    deliveryTimeoutMs,
                    metrics,
                    PRODUCER_METRIC_GROUP_NAME,
                    time,
                    apiVersions,
                    transactionManager,
                    //内存池
                    new BufferPool(this.totalMemorySize, config.getInt(ProducerConfig.BATCH_SIZE_CONFIG), metrics, time,PRODUCER_METRIC_GROUP_NAME));
```

### sender初始化

​		sender线程是完成数据从生产者发送到kafka集群的线程。该线程会从上面的缓冲区拉取可以发送的消息，并按partition组成发送的batch，之后从生产者发送到kafka集群。代码如下：

```java
 						//sender,真正发送数据的线程
            this.sender = newSender(logContext, kafkaClient, this.metadata);
            String ioThreadName = NETWORK_THREAD_PREFIX + " | " + clientId;
            this.ioThread = new KafkaThread(ioThreadName, this.sender, true);
            this.ioThread.start();
```

sender初始化代码如下：

```java
// visible for testing
    Sender newSender(LogContext logContext, KafkaClient kafkaClient, ProducerMetadata metadata) {
        int maxInflightRequests = configureInflightRequests(producerConfig, transactionManager != null);
        int requestTimeoutMs = producerConfig.getInt(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG);
        ChannelBuilder channelBuilder = ClientUtils.createChannelBuilder(producerConfig, time);
        ProducerMetrics metricsRegistry = new ProducerMetrics(this.metrics);
        Sensor throttleTimeSensor = Sender.throttleTimeSensor(metricsRegistry.senderMetrics);
        //网络连接 初始化kafkaClient
        KafkaClient client = kafkaClient != null ? kafkaClient : new NetworkClient(
                new Selector(producerConfig.getLong(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG),
                        this.metrics, time, "producer", channelBuilder, logContext),
                metadata,
                clientId,
                maxInflightRequests,
                producerConfig.getLong(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG),
                producerConfig.getLong(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG),
                producerConfig.getInt(ProducerConfig.SEND_BUFFER_CONFIG),
                producerConfig.getInt(ProducerConfig.RECEIVE_BUFFER_CONFIG),
                requestTimeoutMs,
                ClientDnsLookup.forConfig(producerConfig.getString(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG)),
                time,
                true,
                apiVersions,
                throttleTimeSensor,
                logContext);
        int retries = configureRetries(producerConfig, transactionManager != null, log);
        short acks = configureAcks(producerConfig, transactionManager != null, log);
        /**
         * acks:
         *  0: 生产者发送完数据，不管Broker的响应
         *  1: 生产者发送数据后，接受到Leader Partition返回后成功
         *  -1: 生产者发送数据后，接收到所有partition成功后，才认为成功
         */
        return new Sender(logContext,
                client,
                metadata,
                this.accumulator,
                maxInflightRequests == 1,
                producerConfig.getInt(ProducerConfig.MAX_REQUEST_SIZE_CONFIG),
                acks,
                retries,
                metricsRegistry.senderMetrics,
                time,
                requestTimeoutMs,
                producerConfig.getLong(ProducerConfig.RETRY_BACKOFF_MS_CONFIG),
                this.transactionManager,
                apiVersions);
    }
```



## 生产者发送消息代码解析

生产者发送消息的方法有两个分别如下:

```java
		@Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }	
		@Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        // intercept the record, which can be potentially modified; this method does not throw exceptions
        ProducerRecord<K, V> interceptedRecord = this.interceptors.onSend(record);
        return doSend(interceptedRecord, callback);
    }
```

​		可以看出最后都是调用第二个方法。该方法会将消息异步发送到指定topic并在消息确认发送成功后调用提供的回调函数。该方法在消息放到生产者的缓冲区后就立刻返回。这样可以并行无阻塞的发送消息。

​		该方法的返回值是一个RecordMetadata,该类包含消息的分区信息、消息的offset、以及创建消息的时间。topic指定的TimestampType是CREATE_TIME时，消息的timestamp是用户提供的一个时间戳，如果用户没有指定时间戳，则使用消息发送的时间。如果topic指定的TimestampType是LOG_APPEND_TIME是LogAppendTime，消息的时间戳是消息添加到Kafka集群时的本地时间。

​	 上述方法最终会进入doSend()方法，下面各处doSend()方法的解析。



```java
//检查生产者是否已经关闭
            throwIfProducerClosed();
            // first make sure the metadata for the topic is available
            //确定已经获取到元数据，且topic是可用的。根据指定的topic和分区信息获取元数据
            ClusterAndWaitTime clusterAndWaitTime;
            /**
             * 步骤一：获取元数据，maxBlockTimeMs最大等待时间
             */
            try {
                clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), maxBlockTimeMs);
            } catch (KafkaException e) {
                if (metadata.isClosed())
                    throw new KafkaException("Producer closed while send in progress", e);
                throw e;
            }
            long remainingWaitMs = Math.max(0, maxBlockTimeMs - clusterAndWaitTime.waitedOnMetadataMs);
            Cluster cluster = clusterAndWaitTime.cluster;
            /**
             * 步骤二：
             * 对消息的key和value进行序列化
             */
            byte[] serializedKey;
            try {
                serializedKey = keySerializer.serialize(record.topic(), record.headers(), record.key());
            } catch (ClassCastException cce) {
                throw new SerializationException("Can't convert key of class " + record.key().getClass().getName() +
                        " to class " + producerConfig.getClass(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG).getName() +
                        " specified in key.serializer", cce);
            }
            byte[] serializedValue;
            try {
                serializedValue = valueSerializer.serialize(record.topic(), record.headers(), record.value());
            } catch (ClassCastException cce) {
                throw new SerializationException("Can't convert value of class " + record.value().getClass().getName() +
                        " to class " + producerConfig.getClass(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG).getName() +
                        " specified in value.serializer", cce);
            }
            /**
             * 步骤三：获取分区
             */
            int partition = partition(record, serializedKey, serializedValue, cluster);
            tp = new TopicPartition(record.topic(), partition);

            setReadOnly(record.headers());
            Header[] headers = record.headers().toArray();

            int serializedSize = AbstractRecords.estimateSizeInBytesUpperBound(apiVersions.maxUsableProduceMagic(),
                    compressionType, serializedKey, serializedValue, headers);
            /**
             * 步骤四：检查消息的大小，单条消息的大小不能大于设置的最大值，也不能大于缓冲区的大小
             */
            ensureValidRecordSize(serializedSize);
            long timestamp = record.timestamp() == null ? time.milliseconds() : record.timestamp();
            if (log.isTraceEnabled()) {
                log.trace("Attempting to append record {} with callback {} to topic {} partition {}", record, callback, record.topic(), partition);
            }
            /**
             * 步骤五：给每个消息绑定回调函数
             */
            // producer callback will make sure to call both 'callback' and interceptor callback
            Callback interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);

            if (transactionManager != null && transactionManager.isTransactional()) {
                transactionManager.failIfNotReadyForSend();
            }

            /**
             *  步骤六：
             *  将消息追加到缓冲区中，这是一个要终点关注的方法，里面有很多提升性能巧妙的设计
             */
            RecordAccumulator.RecordAppendResult result = accumulator.append(tp, timestamp, serializedKey,
                    serializedValue, headers, interceptCallback, remainingWaitMs, true);

            /**
             * 当batch已经满了，要在这里新开辟batch添加数据
             */
            if (result.abortForNewBatch) {
                int prevPartition = partition;
                partitioner.onNewBatch(record.topic(), cluster, prevPartition);
                partition = partition(record, serializedKey, serializedValue, cluster);
                tp = new TopicPartition(record.topic(), partition);
                if (log.isTraceEnabled()) {
                    log.trace("Retrying append due to new batch creation for topic {} partition {}. The old partition was {}", record.topic(), partition, prevPartition);
                }
                // producer callback will make sure to call both 'callback' and interceptor callback
                interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);

                result = accumulator.append(tp, timestamp, serializedKey,
                    serializedValue, headers, interceptCallback, remainingWaitMs, false);
            }

            if (transactionManager != null && transactionManager.isTransactional())
                transactionManager.maybeAddPartitionToTransaction(tp);

            if (result.batchIsFull || result.newBatchCreated) {
                log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);
                /**
                 * 步骤八
                 * 唤醒发送数据的线程
                 */
                this.sender.wakeup();
            }
            return result.future;
```

根据上述代码可以看出，发送数据的流程大概分为八个步骤，下面对每个步骤进行分析

#### 步骤一：获取元数据

获取元数据的代码

```java
clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), maxBlockTimeMs);
private ClusterAndWaitTime waitOnMetadata(String topic, Integer partition, long maxWaitMs) throws InterruptedException {
        // add topic to metadata topic list if it is not there already and reset expiry
        Cluster cluster = metadata.fetch();

        //检查topic是否失效
        if (cluster.invalidTopics().contains(topic))
            throw new InvalidTopicException(topic);

        //将topic添加到元数据
        metadata.add(topic);

        /**
         * 获取topic的分区信息，第一次发送数据的时候，该值为null
         */
        Integer partitionsCount = cluster.partitionCountForTopic(topic);
        // Return cached metadata if we have it, and if the record's partition is either undefined
        // or within the known partition range
        if (partitionsCount != null && (partition == null || partition < partitionsCount))
            return new ClusterAndWaitTime(cluster, 0);
        /**
         * 获取元数据的开始时间
         */
        long begin = time.milliseconds();
        /**
         * 获取元数据的剩余时间
         */
        long remainingWaitMs = maxWaitMs;
        /**
         * 获取元数据花费的时间
         */
        long elapsed;
        // Issue metadata requests until we have metadata for the topic and the requested partition,
        // or until maxWaitTimeMs is exceeded. This is necessary in case the metadata
        // is stale and the number of partitions for this topic has increased in the meantime.
        do {
            if (partition != null) {
                log.trace("Requesting metadata update for partition {} of topic {}.", partition, topic);
            } else {
                log.trace("Requesting metadata update for topic {}.", topic);
            }
            metadata.add(topic);
            int version = metadata.requestUpdate();
            /**
             * 唤醒sender线程，这里是获取元数据真正位置
             */
            sender.wakeup();
            try {
                /**
                 * 挂起该线程，直到sender线程获取元数据成功，或者超时
                 */
                metadata.awaitUpdate(version, remainingWaitMs);
            } catch (TimeoutException ex) {
                // Rethrow with original maxWaitMs to prevent logging exception with remainingWaitMs
                throw new TimeoutException(
                        String.format("Topic %s not present in metadata after %d ms.",
                                topic, maxWaitMs));
            }
            /**
             * 以下都是获取的元数据赋值及检查
             */
            cluster = metadata.fetch();
            elapsed = time.milliseconds() - begin;
            if (elapsed >= maxWaitMs) {
                throw new TimeoutException(partitionsCount == null ?
                        String.format("Topic %s not present in metadata after %d ms.",
                                topic, maxWaitMs) :
                        String.format("Partition %d of topic %s with partition count %d is not present in metadata after %d ms.",
                                partition, topic, partitionsCount, maxWaitMs));
            }
            metadata.maybeThrowExceptionForTopic(topic);
            remainingWaitMs = maxWaitMs - elapsed;
            partitionsCount = cluster.partitionCountForTopic(topic);
        } while (partitionsCount == null || (partition != null && partition >= partitionsCount));

        return new ClusterAndWaitTime(cluster, elapsed);
    }

```

​		根据这段代码可以看出，获取元数据需要sender线程获取，下面分析下sender线程获取元数据的代码。run()方法中关键代码如下

```java
				log.debug("Starting Kafka producer I/O thread.");

        /**
         * 该方法在Producer关闭前会一直调用
         */
        while (running) {
            try {
                runOnce();
            } catch (Exception e) {
                log.error("Uncaught error in kafka producer I/O thread: ", e);
            }
        }
```

run()方法中关键的执行方法是runOnce()。关键代码如下：

```java
				/**
         * 发送数据的代码在下面。
         */
        long currentTimeMs = time.milliseconds();
        long pollTimeout = sendProducerData(currentTimeMs);
        client.poll(pollTimeout, currentTimeMs);
```

其中关键代码是poll()方法，关键获取请求的代码如下：

```java
 				// process completed actions
        long updatedNow = this.time.milliseconds();
        List<ClientResponse> responses = new ArrayList<>();
        handleCompletedSends(responses, updatedNow);
				//处理返回的请求
        handleCompletedReceives(responses, updatedNow);
        handleDisconnections(responses, updatedNow);
        handleConnections();
        handleInitiateApiVersionRequests(updatedNow);
        handleTimedOutRequests(responses, updatedNow);
        completeResponses(responses);
```



#### 步骤二：对消息的key和value进行序列化

这段代码没有特殊的逻辑，不再赘述



#### 步骤三：获取数据分区

```java
int partition = partition(record, serializedKey, serializedValue, cluster)
Integer partition = record.partition();
        return partition != null ?
                partition :
                partitioner.partition(
                        record.topic(), record.key(), serializedKey, record.value(), serializedValue, cluster);

if (keyBytes == null) {
            return stickyPartitionCache.partition(topic, cluster);
        } 
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        // hash the keyBytes to choose a partition
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
```



#### 步骤四：检查消息的大小

​		检查消息的大小，单条消息的大小既不能大于设置的最大值，也不能大于缓冲区的大小

```java
if (size > this.maxRequestSize)
            throw new RecordTooLargeException("The message is " + size +
                    " bytes when serialized which is larger than the maximum request size you have configured with the " +
                    ProducerConfig.MAX_REQUEST_SIZE_CONFIG +
                    " configuration.");
        if (size > this.totalMemorySize)
            throw new RecordTooLargeException("The message is " + size +
                    " bytes when serialized which is larger than the total memory buffer you have configured with the " +
                    ProducerConfig.BUFFER_MEMORY_CONFIG +
                    " configuration.");
```



#### 步骤五：绑定回调函数

```java
Callback interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);
```



#### 步骤六：消息追加到缓冲区

​		将消息追加到缓冲区，这里会把消息封装为一个batches，这里有很多巧妙的设计。

```java
appendsInProgress.incrementAndGet();
        ByteBuffer buffer = null;
        if (headers == null) headers = Record.EMPTY_HEADERS;
        try {
            // 这里检查对应Topic的Deque是否已经创建
            Deque<ProducerBatch> dq = getOrCreateDeque(tp);
           // 这里是多线程可以同时操作，所以在这里加锁 
            synchronized (dq) {
                if (closed)
                    throw new KafkaException("Producer closed while send in progress");
              /**
               * 将消息添加到Toic对应的Batch中
               */
                RecordAppendResult appendResult = tryAppend(timestamp, key, value, headers, callback, dq);
                if (appendResult != null)
                    return appendResult;
            }

            // we don't have an in-progress record batch try to allocate a new batch
            if (abortOnNewBatch) {
                // Return a result that will cause another call to append.
                return new RecordAppendResult(null, false, false, true);
            }
            
            byte maxUsableMagic = apiVersions.maxUsableProduceMagic();
            int size = Math.max(this.batchSize, AbstractRecords.estimateSizeInBytesUpperBound(maxUsableMagic, compression, key, value, headers));
            log.trace("Allocating a new {} byte message buffer for topic {} partition {}", size, tp.topic(), tp.partition());
            //内存池申请
            buffer = free.allocate(size, maxTimeToBlock);
            synchronized (dq) {
                // Need to check if producer is closed again after grabbing the dequeue lock.
                if (closed)
                    throw new KafkaException("Producer closed while send in progress");
                RecordAppendResult appendResult = tryAppend(timestamp, key, value, headers, callback, dq);
                if (appendResult != null) {
                    // Somebody else found us a batch, return the one we waited for! Hopefully this doesn't happen often...
                    return appendResult;
                }

                MemoryRecordsBuilder recordsBuilder = recordsBuilder(buffer, maxUsableMagic);
                ProducerBatch batch = new ProducerBatch(tp, recordsBuilder, time.milliseconds());
                FutureRecordMetadata future = Objects.requireNonNull(batch.tryAppend(timestamp, key, value, headers,
                        callback, time.milliseconds()));

                dq.addLast(batch);
                incomplete.add(batch);

                // Don't deallocate this buffer in the finally block as it's being used in the record batch
                buffer = null;
                return new RecordAppendResult(future, dq.size() > 1 || batch.isFull(), true, false);
            }
        } finally {
            if (buffer != null)
                free.deallocate(buffer);
            appendsInProgress.decrementAndGet();
        }
```

​		代码中使用了分段加锁，用来提高性能。其中创建Batches使用了自己创建的数据结构，是一个读写分离的Map,也是为了提高性能进行设计的。

​		上述中有内存池的设计，内存池设计示意图如下：

![内存池设计示意图](/Users/dengguoqing/IdeaProjects/bigData/笔记/kafka/内存池设计.png)

#### 步骤七：当满足发送数据的条件时，进行数据的发送

即batches满足大小，或者时间达到了发送的条件。