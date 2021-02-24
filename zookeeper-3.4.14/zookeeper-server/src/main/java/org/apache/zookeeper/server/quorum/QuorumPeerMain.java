/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.server.quorum;

import java.io.File;
import java.io.IOException;

import javax.management.JMException;
import javax.security.sasl.SaslException;

import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.jmx.ManagedUtil;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

/**
 *
 * <h2>Configuration file</h2>
 *
 * When the main() method of this class is used to start the program, the first
 * argument is used as a path to the config file, which will be used to obtain
 * configuration information. This file is a Properties file, so keys and
 * values are separated by equals (=) and the key/value pairs are separated
 * by new lines. The following is a general summary of keys used in the
 * configuration file. For full details on this see the documentation in
 * docs/index.html
 * <ol>
 * <li>dataDir - The directory where the ZooKeeper data is stored.</li>
 * <li>dataLogDir - The directory where the ZooKeeper transaction log is stored.</li>
 * <li>clientPort - The port used to communicate with clients.</li>
 * <li>tickTime - The duration of a tick in milliseconds. This is the basic
 * unit of time in ZooKeeper.</li>
 * <li>initLimit - The maximum number of ticks that a follower will wait to
 * initially synchronize with a leader.</li>
 * <li>syncLimit - The maximum number of ticks that a follower will wait for a
 * message (including heartbeats) from the leader.</li>
 * <li>server.<i>id</i> - This is the host:port[:port] that the server with the
 * given id will use for the quorum protocol.</li>
 * </ol>
 * In addition to the config file. There is a file in the data directory called
 * "myid" that contains the server id as an ASCII decimal value.
 *
 * // TODO_MA 注释： QuorumPeerMain = QuorumPeer + Main
 * // TODO_MA 注释： 每个节点的启动命令由于是一样的额，所以每个节点的启动都是执行这个类
 *  // TODO_MA 注释：QuorumPeer 就是对每一个 zookeeper 服务节点的 抽象
 *      类似于 HDFS ： namenode（NameNode） datanode（DataNode）
 *  // TODO_MA 注释： 提供一个 main 方法
 */
@InterfaceAudience.Public
public class QuorumPeerMain {
    private static final Logger LOG = LoggerFactory.getLogger(QuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";

    protected QuorumPeer quorumPeer;

    /**
     * To start the replicated server specify the configuration file name on
     * the command line.
     * @param args path to the configfile
     */
    public static void main(String[] args) {

        // TODO_MA 注释：初始化一个对象：QuorumPeerMain
        QuorumPeerMain main = new QuorumPeerMain();
        try {

            /*************************************************
             * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
             *  注释： 调用 initializeAndRun 方法执行启动
             */
            main.initializeAndRun(args);
        } catch(IllegalArgumentException e) {
            LOG.error("Invalid arguments, exiting abnormally", e);
            LOG.info(USAGE);
            System.err.println(USAGE);
            System.exit(2);
        } catch(ConfigException e) {
            LOG.error("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            System.exit(2);
        } catch(Exception e) {
            LOG.error("Unexpected exception, exiting abnormally", e);
            System.exit(1);
        }

        // TODO_MA 注释： JVM 退出
        LOG.info("Exiting normally");
        System.exit(0);
    }

    /*************************************************
     * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
     *  注释： 这个方法做了三件事：
     *  1、解析参数
     *  2、启动了一个定时服务： 每隔一段时间，对过期的旧快照数据文件，执行删除操作
     *  3、启动
     *          分两种模式：
     *          1、单机 local
     *          2、集群 cluster
     */
    protected void initializeAndRun(String[] args) throws ConfigException, IOException {


        /*************************************************
         * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
         *  注释： 第一件事
         *  1、args[0] = zoo.cfg 的路径
         *  2、做了两件事：
         *      1、解析 zoo.cfg
         *      2、解析 myid
         */
        QuorumPeerConfig config = new QuorumPeerConfig();
        if(args.length == 1) {
            config.parse(args[0]);
        }

        /*************************************************
         * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
         *  注释： 第二件事
         *  启动定时任务
         */
        // Start and schedule the the purge task
        DatadirCleanupManager purgeMgr = new DatadirCleanupManager(config.getDataDir(), config.getDataLogDir(), config.getSnapRetainCount(),
                config.getPurgeInterval());
        purgeMgr.start();


        /*************************************************
         * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
         *  注释： 第三件事
         *  两个条件：
         *  1、 main 方法只有一个参数： zoo.cfg
         *  2、当从 zoo.cfg 中解析出来的  servers（）
         */
        if(args.length == 1 && config.servers.size() > 0) {

            // TODO_MA 注释： 启动集群模式
            runFromConfig(config);
        } else {

            // TODO_MA 注释： standalone 本地模式
            LOG.warn("Either no config or no quorum defined in config, running " + " in standalone mode");
            // there is only server in the quorum -- run as standalone
            ZooKeeperServerMain.main(args);
        }
    }

    /*************************************************
     * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
     *  注释： 重点就做了两件事：
     *  1、 初始化一个 ServerCnxn 的工厂对象，用来监听客户端的链接请求，然后每接收到一个客户单的链接请求
     *  就会使用这个工厂对象，初始化一个 ServerCnxn 来执行服务
     *  具体的类是： ServerCnxnFactory， 他里面有一个 Listener ，绑定了服务端口（2181） 等待客户端发送链接请求过来执行处理
     *  -
     *  2、初始化 QuorumPeer 并启动
     *      1、从 磁盘快照文件中执行数据恢复
     *      2、启动的时候，肯定是需要执行选举的呀
     */
    public void runFromConfig(QuorumPeerConfig config) throws IOException {
        try {
            ManagedUtil.registerLog4jMBeans();
        } catch(JMException e) {
            LOG.warn("Unable to register log4j JMX control", e);
        }

        LOG.info("Starting quorum peer");
        try {

            /*************************************************
             * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
             *  注释： 第一件事
             *  1、NIOServerCnxnFactory cnxnFactory
             *  2、NIOServerCnxnFactory.configure(....)
             */
            ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
            cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());


            /*************************************************
             * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
             *  注释： 第二件事
             *  1、构建实例：  quorumPeer = getQuorumPeer();
             *  2、设置参数：  quorumPeer.setXXX(xxx);   参数的来源，集市 config 对象
             *  3、启动：  quorumPeer.start()
             *  -
             *  注意： QuorumPeer 对于一个 zookeeper 节点的抽象
             */
            quorumPeer = getQuorumPeer();

            quorumPeer.setQuorumPeers(config.getServers());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config.getDataLogDir()), new File(config.getDataDir())));
            quorumPeer.setElectionType(config.getElectionAlg());
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setQuorumListenOnAllIPs(config.getQuorumListenOnAllIPs());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(config.getPeerType());
            quorumPeer.setSyncEnabled(config.getSyncEnabled());

            // sets quorum sasl authentication configurations
            quorumPeer.setQuorumSaslEnabled(config.quorumEnableSasl);
            if(quorumPeer.isQuorumSaslAuthEnabled()) {
                quorumPeer.setQuorumServerSaslRequired(config.quorumServerRequireSasl);
                quorumPeer.setQuorumLearnerSaslRequired(config.quorumLearnerRequireSasl);
                quorumPeer.setQuorumServicePrincipal(config.quorumServicePrincipal);
                quorumPeer.setQuorumServerLoginContext(config.quorumServerLoginContext);
                quorumPeer.setQuorumLearnerLoginContext(config.quorumLearnerLoginContext);
            }

            quorumPeer.setQuorumCnxnThreadsSize(config.quorumCnxnThreadsSize);
            quorumPeer.initialize();

            /*************************************************
             * TODO_MA 马中华 https://blog.csdn.net/zhongqi2513
             *  注释： 启动
             *  终于从 QuorumPeerMain.main() 跳转到  QuorumPeer.start()
             *  在这个过程中：
             *  1、 解析了参数
             *  2、 初始化和启动了服务端
             */
            quorumPeer.start();
            quorumPeer.join();
        } catch(InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Quorum Peer interrupted", e);
        }
    }

    // @VisibleForTesting
    protected QuorumPeer getQuorumPeer() throws SaslException {
        return new QuorumPeer();
    }
}
