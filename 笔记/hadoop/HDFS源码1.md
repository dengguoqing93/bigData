## NameNode启动

NameNode类关键注释和代码：

```java
/*
* The NameNode controls two critical tables:
*   1)  filename->blocksequence (namespace)   文件名和块信息存储表
*   2)  block->machinelist ("inodes")   块和dataNode存储映射关系
* The first table is stored on disk and is very precious.  第一个表由于非常重要，所以存储在硬盘
* The second table is rebuilt every time the NameNode comes up.第二个表会在每次启动NameNode时重建
*/
//创建NameNode
NameNode namenode = createNameNode(argv, null);

//启动HTTPServer以及RPCServer;HTTPServer默认端口号：50070 

```