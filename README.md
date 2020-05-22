### 1.介绍
本篇将介绍,如何进行seata1.2.0、sharding-sphere4.1.0和dubbo2.7.5 的整合,以及使用nacos作为我们的配置中心和注册中心。如果你还是一个初学者，先建议学习一下，陈建斌的[七步带你集成Seata 1.2 高可用搭建](https://mp.weixin.qq.com/s/2KSidJ72YsovpJ94P1aK1g)，这篇文章清楚的阐述了初学者容易遇到的5个问题，并且都提供完整的解决思路。

### 2.环境配置
 - mysql: 5.7.12

- nacos:  1.2.1

- spring-boot:  2.2.6.RELEASE

- seata: 1.2.0

- dubbo:2.7.5

- sharding-sphere: 4.1.0

- 开发环境: jdk1.8.0 


#### 2.1 nacos安装
nacos下载：[https://github.com/alibaba/nacos/releases/tag/1.2.1](https://github.com/alibaba/nacos/releases/tag/1.2.1)

Nacos 快速入门：[https://nacos.io/en-us/docs/quick-start.html](https://nacos.io/en-us/docs/quick-start.html)

```shell
sh startup.sh -m standalone
```

在浏览器打开Nacos web 控制台：http://127.0.0.1:8848/nacos/index.html

输入nacos的账号和密码 分别为`nacos：nacos`

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521172522791.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
这是时候naocs 就正常启动了。
#### 2.2 seata1.2.0安装

##### 2.2.1 在 [Seata Release](https://github.com/seata/seata/releases/tag/v1.2.0) 下载最新版的 Seata Server 并解压得到如下目录：


```shell
.
├──bin
├──conf
└──lib
```
##### 2.2.2 修改 conf/registry.conf 配置，
目前seata支持如下的file、nacos 、apollo、zk、consul的注册中心和配置中心。这里我们以`nacos` 为例。
将 type 改为 nacos

```bash
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"

  nacos {
    application = "seata-server"
    serverAddr = "127.0.0.1:8848"
    namespace = "40508bb4-179e-4c98-a2f1-c2c031c20b3c"
    cluster = "default"
    username = "worker2"
    password = "xxxxxxx"
  }
}

config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "nacos"

  nacos {
    serverAddr = "127.0.0.1:8848"
    namespace = "40508bb4-179e-4c98-a2f1-c2c031c20b3c"
    group = "SEATA_GROUP"
    username = "worker2"
    password = "xxxxxxx"
  }
}
```
- serverAddr = "127.0.0.1:8848"   ：nacos 的地址
- namespace = "" ：nacos的命名空间默认为``
- cluster = "default"  ：集群设置未默认 `default`

##### 2.2.3 修改 conf/config.txt配置

```
service.vgroupMapping.order-service-seata-service-group=default
service.vgroupMapping.account-service-seata-service-group=default
service.vgroupMapping.storage-service-seata-service-group=default
service.vgroupMapping.business-service-seata-service-group=default
store.mode=db
store.db.driverClassName=com.mysql.jdbc.Driver
store.db.datasource=druid
store.db.dbType=mysql
store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true
store.db.user=root
store.db.password=123456
store.db.minConn=1
store.db.maxConn=3
store.db.global.table=global_table
store.db.branch.table=branch_table
store.db.query-limit=100
store.db.lockTable=lock_table
```
配置的详细说明参考官网：[https://seata.io/zh-cn/docs/user/configurations.html](https://seata.io/zh-cn/docs/user/configurations.html)

这里主要修改了如下几项：
- store.mode :存储模式 默认file  这里我修改为db 模式 ，并且需要三个表`global_table`、`branch_table`和`lock_table`
- store.db.driverClassName： 0.8.0版本默认没有，会报错。添加了 `com.mysql.jdbc.Driver`
- store.db.datasource=dbcp ：数据源 dbcp
- store.db.db-type=mysql : 存储数据库的类型为`mysql`
- store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true : 修改为自己的数据库`url`、`port`、`数据库名称`
- store.db.user=lidong :数据库的账号
- store.db.password=cwj887766@@ :数据库的密码
- service.vgroupMapping.order-service-seata-service-group=default
- service.vgroupMapping.account-service-seata-service-group=default
- service.vgroupMapping.storage-service-seata-service-group=default
- service.vgroupMapping.business-service-seata-service-group=default

##### 2.2.4 db模式下的所需的三个表
数据库脚本位于[https://github.com/seata/seata/tree/develop/script/server/db](https://github.com/seata/seata/tree/develop/script/server/db)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521173848590.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
这里我用的是mysql数据库，直接下载mysq.sql就可以了。

`global_table`的表结构

```sql
CREATE TABLE `global_table` (
  `xid` varchar(128) NOT NULL,
  `transaction_id` bigint(20) DEFAULT NULL,
  `status` tinyint(4) NOT NULL,
  `application_id` varchar(64) DEFAULT NULL,
  `transaction_service_group` varchar(64) DEFAULT NULL,
  `transaction_name` varchar(64) DEFAULT NULL,
  `timeout` int(11) DEFAULT NULL,
  `begin_time` bigint(20) DEFAULT NULL,
  `application_data` varchar(2000) DEFAULT NULL,
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  PRIMARY KEY (`xid`),
  KEY `idx_gmt_modified_status` (`gmt_modified`,`status`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

```

`branch_table`的表结构

```sql
CREATE TABLE `branch_table` (
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(128) NOT NULL,
  `transaction_id` bigint(20) DEFAULT NULL,
  `resource_group_id` varchar(32) DEFAULT NULL,
  `resource_id` varchar(256) DEFAULT NULL,
  `lock_key` varchar(128) DEFAULT NULL,
  `branch_type` varchar(8) DEFAULT NULL,
  `status` tinyint(4) DEFAULT NULL,
  `client_id` varchar(64) DEFAULT NULL,
  `application_data` varchar(2000) DEFAULT NULL,
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


```
`lock_table`的表结构

```sql
create table `lock_table` (
  `row_key` varchar(128) not null,
  `xid` varchar(96),
  `transaction_id` long ,
  `branch_id` long,
  `resource_id` varchar(256) ,
  `table_name` varchar(32) ,
  `pk` varchar(32) ,
  `gmt_create` datetime ,
  `gmt_modified` datetime,
  primary key(`row_key`)
);
```

##### 2.2.5 将 Seata 配置添加到 Nacos 中

nacos导入脚本位于[https://github.com/seata/seata/tree/develop/script/config-center/nacos](https://github.com/seata/seata/tree/develop/script/config-center/nacos)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521174115613.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
使用方法：

```bash
sh ${SEATAPATH}/script/config-center/nacos/nacos-config.sh -h localhost -p 8848 -g SEATA_GROUP -t 40508bb4-179e-4c98-a2f1-c2c031c20b3c -u worker-w xxxxxx
```
参数描述:

- -h: host, 默认值 localhost.

- -p: port, 默认值 is 8848.

- -g: 配置分组 默认值 'SEATA_GROUP'.

- -t: 命名空间.

- -u: 用户名, nacos 1.2.0+ 之后添加权限验证 默认为“”

- -w: 密码, nacos 1.2.0+ 之后添加权限验证 默认为“”
- 
在 Nacos 管理页面应该可以看到Group 为SEATA_GROUP的配置 
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521174606735.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
这样seata-sever就搭建完成。

### 3.sharding-sphere中seata柔性事务实现
#### 3.1 实现原理
整合`Seata AT`事务时，需要把`TM`，`RM`，`TC`的模型融入到`ShardingSphere` 分布式事务的`SPI`的生态中。在数据库资源上，`Seata`通过对接`DataSource`接口，让`JDBC`操作可以同`TC`进行`RPC`通信。同样，`ShardingSphere`也是面向`DataSource`接口对用户配置的物理`DataSource`进行了聚合，因此把物理`DataSource`二次包装为`Seata `的`DataSource`后，就可以把`Seata AT`事务融入到`ShardingSphere`的分片中。
#### 3.2实现原理图
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521175420580.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
#### 3.3 实现的步骤

 1. Init（Seata引擎初始化）
包含Seata柔性事务的应用启动时，用户配置的数据源会按seata.conf的配置，适配为Seata事务所需的DataSourceProxy，并且注册到RM中。

 2. Begin（开启Seata全局事务）
TM控制全局事务的边界，TM通过向TC发送Begin指令，获取全局事务ID，所有分支事务通过此全局事务ID，参与到全局事务中；全局事务ID的上下文存放在当前线程变量中。

3. 执行分片物理SQL
处于Seata全局事务中的分片SQL通过RM生成undo快照，并且发送participate指令到TC，加入到全局事务中。ShardingSphere的分片物理SQL是按多线程方式执行，因此整合Seata AT事务时，需要在主线程和子线程间进行全局事务ID的上下文传递，这同服务间的上下文传递思路完全相同。

5. Commit/rollback（提交Seata事务）
提交Seata事务时，TM会向TC发送全局事务的commit和rollback指令，TC根据全局事务ID协调所有分支事务进行commit和rollback。

### 4.sharding-sphere中seata的整合
##### 4.1使用Spring-boot引入Maven依赖

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
    <version>${shardingsphere.version}</version>
</dependency>

<!-- 使用BASE事务时，需要引入此模块 -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>sharding-transaction-base-seata-at</artifactId>
    <version>${sharding-sphere.version}</version>
</dependency>
```

##### 4.2.Seata的AT模式使用的BASE柔性事务管理器
在每一个分片数据库实例中执创建undo_log表（以MySQL为例）

```sql
CREATE TABLE IF NOT EXISTS `undo_log`
(
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT 'increment id',
  `branch_id`     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
  `xid`           VARCHAR(100) NOT NULL COMMENT 'global transaction id',
  `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
  `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
  `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
  `log_created`   DATETIME     NOT NULL COMMENT 'create datetime',
  `log_modified`  DATETIME     NOT NULL COMMENT 'modify datetime',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';
```
##### 4.3.在classpath中增加seata.conf

```shell
client {
    application.id = example    ## 应用唯一id
    transaction.service.group = my_test_tx_group   ## 所属事务组
}
```




##### 4.4业务方发起全局事务，配置柔性事务类型

```java
 @GlobalTransactional(timeoutMills = 300000, name = "dubbo-gts-seata-example")
 @Override
public ObjectResponse handleBusiness(BusinessDTO businessDTO) {
        TransactionTypeHolder.set(TransactionType.BASE);
        //执行业务逻辑
}
```
**备注**：也可是使用注解` @ShardingTransactionType`的形式
```java
 @GlobalTransactional(timeoutMills = 300000, name = "dubbo-gts-seata-example")
 @ShardingTransactionType(TransactionType.BASE)
@Override
 public ObjectResponse handleBusiness(BusinessDTO businessDTO) {
     //执行业务逻辑  
}
        
```

### 5.案例实现

参考官网中用户购买商品的业务逻辑。整个业务逻辑由4个微服务提供支持：

- 库存服务：扣除给定商品的存储数量。
- 订单服务：根据购买请求创建订单。
- 帐户服务：借记用户帐户的余额。
- 业务服务：处理业务逻辑。

请求逻辑架构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190905111031350.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9saWRvbmcxNjY1LmJsb2cuY3Nkbi5uZXQ=,size_16,color_FFFFFF,t_70)

#### 5.1 源码地址
- samples-common ：公共模块

- samples-account ：用户账号模块

- samples-order ：订单模块

- samples-storage ：库存模块

- samples-business ：业务模块

#### 5.2 数据库
注意: MySQL必须使用`InnoDB engine`.

如下，并且每个库中都需要一个undo_log表
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200521180601443.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTAwNDY5MDg=,size_16,color_FFFFFF,t_70)
#### 5.3 以账号服务为例 
分析需要项目中所需要的配置
##### 5.3.1 引入的依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.6.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <artifactId>seata-spring-boot-dubbo-nacos-shardingsphere-examples</artifactId>
    <packaging>pom</packaging>
    <name>seata-spring-boot-dubbo-nacos-shardingsphere-examples</name>
    <groupId>io.seata</groupId>
    <version>1.2.0</version>
    <description>Demo project for Spring Boot Dubbo</description>

    <modules>
        <module>samples-common-service</module>
        <module>samples-account-service</module>
        <module>samples-order-service</module>
        <module>samples-storage-service</module>
        <module>samples-business-service</module>
    </modules>

    <properties>
        <springboot.verison>2.2.6.RELEASE</springboot.verison>
        <java.version>1.8</java.version>
        <mybatis-plus.version>2.3</mybatis-plus.version>
        <nacos.version>0.2.3</nacos.version>
        <lombok.version>1.16.22</lombok.version>
        <dubbo.version>2.7.5</dubbo.version>
        <nacos-client.verison>1.2.1</nacos-client.verison>
        <seata.version>1.2.0</seata.version>
        <netty.version>4.1.32.Final</netty.version>
        <sharding-sphere.version>4.1.0</sharding-sphere.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${springboot.verison}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>${springboot.verison}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${springboot.verison}</version>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.3.1</version>
        </dependency>


        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
            <version>${dubbo.version}</version>
            <exclusions>
                <exclusion>
                    <artifactId>spring</artifactId>
                    <groupId>org.springframework</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
            <version>${dubbo.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.dubbo/dubbo-config-spring -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-configcenter-nacos</artifactId>
            <version>${dubbo.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-registry-nacos</artifactId>
            <version>${dubbo.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-metadata-report-nacos</artifactId>
            <version>${dubbo.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/io.seata/seata-all -->

        <dependency>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
            <version>${seata.version}</version>
        </dependency>


        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
            <version>${nacos-client.verison}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>${springboot.verison}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
        </dependency>


        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>${netty.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5</version>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.47</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-core</artifactId>
            <version>${sharding-sphere.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>${sharding-sphere.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-transaction-base-seata-at</artifactId>
            <version>${sharding-sphere.version}</version>
        </dependency>

        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>3.3.1</version>
        </dependency>
    </dependencies>


    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```
注意：
- `seata-spring-boot-starter`: 这个是spring-boot seata 所需的主要依赖，1.0.0版本开始加入支持。
- `dubbo-spring-boot-starter`:   springboot dubbo的依赖
- `sharding-transaction-base-seata-at` ：sharding和seata整合的依赖

其他的就不一一介绍，其他的一目了然，就知道是干什么的。

##### 5.3.2  application.yml配置

```yml
server:
  port: 8102
spring:
  shardingsphere:
    datasource:
      names: ds0
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3306/ds0?useUnicode=true&characterEncoding=UTF-8&useSSL=false
        username: root
        password: 123456
    sharding:
      tables:
        t_account:
          actual-data-nodes: ds0.t_account$->{0..1}
          table-strategy:
            inline:
              sharding-column: id
              algorithm-expression: t_account$->{id % 2}
    props:
      sql.show: true

#====================================Dubbo config===============================================
dubbo:
  application:
    id: dubbo-account-example
    name: dubbo-account-example
    qosEnable: false
  protocol:
    id: dubbo
    name: dubbo
    port: 20883
  registry:
    id: dubbo-account-example-registry
    address: nacos://127.0.0.1:8848?namespace=40508bb4-179e-4c98-a2f1-c2c031c20b3c
  config-center:
    address: nacos://127.0.0.1:8848?namespace=40508bb4-179e-4c98-a2f1-c2c031c20b3c
  metadata-report:
    address: nacos://127.0.0.1:8848?namespace=40508bb4-179e-4c98-a2f1-c2c031c20b3c
#====================================mybatis-plus config===============================================
mybatis-plus:
  mapperLocations: classpath*:/mapper/*.xml
  typeAliasesPackage: io.seata.samples.integration.*.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

#====================================Seata Config===============================================

seata:
  enabled: true
  application-id: account-seata-example
  tx-service-group: account-service-seata-service-group # 事务群组（可以每个应用独立取名，也可以使用相同的名字）
  registry:
    file:
      name: file.conf
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: 40508bb4-179e-4c98-a2f1-c2c031c20b3c
      cluster: default
  config:
    file:
      name: file.conf
    type: nacos
    nacos:
      namespace: 40508bb4-179e-4c98-a2f1-c2c031c20b3c
      server-addr: localhost:8848
      group: SEATA_GROUP
  enable-auto-data-source-proxy: true
  use-jdk-proxy: true

```
##### 5.3.3  在classpath中增加seata.conf

```bash
client {
    application.id = account-seata-example ## 应用唯一id
    transaction.service.group = account-service-seata-service-group ## 所属事务组
}
```

##### 5.3.4 启动所有的sample模块
启动 `samples-account-service`、`samples-order-service`、`samples-storage-service`、`samples-business-service`

并且在nocos的控制台查看注册情况: http://192.168.10.200:8848/nacos/#/serviceManagement

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190905131449502.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9saWRvbmcxNjY1LmJsb2cuY3Nkbi5uZXQ=,size_16,color_FFFFFF,t_70)
我们可以看到上面的服务都已经注册成功。

### 6.测试结果
### 6. 1 发送一个下单请求(正常情况)
使用postman 发送 ：[http://localhost:8104/business/dubbo/buy](http://localhost:8104/business/dubbo/buy) 

请求参数：

```json
{
    "userId": 1,
    "commodityCode":"C201901140001",
    "name":"fan",
    "count":50,
    "amount":"100"
}
```
返回参数

```json
{
    "status": 200,
    "message": "成功",
    "data": null
}
```
这时候控制台：
##### 6.1.1 BusinessService 服务日志

```bash
2020-05-22 09:15:54.763  INFO 13384 --- [nio-8104-exec-4] i.s.s.i.c.controller.BusinessController  : 请求参数：BusinessDTO(userId=1, commodityCode=C201901140001, name=fan, count=50, amount=100)
2020-05-22 09:15:54.794  INFO 13384 --- [nio-8104-exec-4] i.seata.tm.api.DefaultGlobalTransaction  : Begin new global transaction [192.168.10.107:8091:2012243535]
2020-05-22 09:15:54.794  INFO 13384 --- [nio-8104-exec-4] i.s.s.i.c.service.BusinessServiceImpl    : 开始全局事务，XID = 192.168.10.107:8091:2012243535
2020-05-22 09:15:55.527  INFO 13384 --- [nio-8104-exec-4] i.seata.tm.api.DefaultGlobalTransaction  : [192.168.10.107:8091:2012243535] commit status: Committed
```
##### 6.1.2 AccountService 服务日志

```bash
2020-05-22 09:15:54.959  INFO 8792 --- [:20883-thread-3] i.s.s.i.a.dubbo.AccountDubboServiceImpl  : 全局事务id ：192.168.10.107:8091:2012243535
Creating a new SqlSession
Registering transaction synchronization for SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@29020a6b]
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@23fa611b] will be managed by Spring
==>  Preparing: update t_account set amount = amount-100.0 where id = 1 
==> Parameters: 
2020-05-22 09:15:54.960  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT id, amount FROM t_account WHERE id = 1 FOR UPDATE
2020-05-22 09:15:54.960  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@60fae881, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@66f8eb00), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@66f8eb00, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=16, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@5f10e4e6, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@57d40a06, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@27d65b5e, containsSubquery=false)
2020-05-22 09:15:54.960  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, amount FROM t_account1 WHERE id = 1 FOR UPDATE
2020-05-22 09:15:54.962  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Logic SQL: update t_account set amount = amount-100.0 where id = 1
2020-05-22 09:15:54.962  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@5ba704b6, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@294b424c), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@294b424c)
2020-05-22 09:15:54.962  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: update t_account1 set amount = amount-100.0 where id = 1
2020-05-22 09:15:54.964  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT id, amount FROM t_account WHERE id in (?)
2020-05-22 09:15:54.964  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@697bcc02, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@299ee29), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@299ee29, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=16, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@1b05b402, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@68afcc0c, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@4352f7f9, containsSubquery=false)
2020-05-22 09:15:54.964  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, amount FROM t_account1 WHERE id in (?) ::: [1]
<==    Updates: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@29020a6b]
Transaction synchronization committing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@29020a6b]
Transaction synchronization deregistering SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@29020a6b]
Transaction synchronization closing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@29020a6b]
2020-05-22 09:15:55.078  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:15:55.079  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@354923b3, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2ebdd5c8), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2ebdd5c8, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@5fd3f282, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@2e3bb335], parameters=[2012243539, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@d4e81a62, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:15:55.079  INFO 8792 --- [:20883-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243539, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@d4e81a62, 0]
2020-05-22 09:15:55.562  INFO 8792 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243535,branchId=2012243539,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds0,applicationData=null
2020-05-22 09:15:55.563  INFO 8792 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch committing: 192.168.10.107:8091:2012243535 2012243539 jdbc:mysql://127.0.0.1:3306/ds0 null
2020-05-22 09:15:55.564  INFO 8792 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch commit result: PhaseTwo_Committed
2020-05-22 09:15:56.217  INFO 8792 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?) 
2020-05-22 09:15:56.217  INFO 8792 --- [  AsyncWorker_1] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@5b1d0b10, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@5f2284ab), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@5f2284ab)
2020-05-22 09:15:56.217  INFO 8792 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?)  ::: [2012243539, 192.168.10.107:8091:2012243535]

```
##### 6.1.3 StorageService 服务日志

```bash
2020-05-22 09:15:54.796  INFO 9580 --- [:20888-thread-3] i.s.s.i.s.dubbo.StorageDubboServiceImpl  : 全局事务id ：192.168.10.107:8091:2012243535
Creating a new SqlSession
Registering transaction synchronization for SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@42cfeb03] will be managed by Spring
==>  Preparing: SELECT id,commodity_code,name,count FROM t_storage WHERE (commodity_code = ?) 
==> Parameters: C201901140001(String)
2020-05-22 09:15:54.798  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT  id,commodity_code,name,count  FROM t_storage 
 
 WHERE (commodity_code = ?)
2020-05-22 09:15:54.798  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@5ec9be76, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7cd6d3e), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7cd6d3e, projectionsContext=ProjectionsContext(startIndex=8, stopIndex=35, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=name, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@738d41ac, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@3680c897, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@b2abab7, containsSubquery=false)
2020-05-22 09:15:54.798  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT  id,commodity_code,name,count  FROM t_storage0 
 
 WHERE (commodity_code = ?) ::: [C201901140001]
2020-05-22 09:15:54.798  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT  id,commodity_code,name,count  FROM t_storage1 
 
 WHERE (commodity_code = ?) ::: [C201901140001]
<==    Columns: id, commodity_code, name, count
<==        Row: 1, C201901140001, 水杯, 650
<==      Total: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
Fetched SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073] from current transaction
==>  Preparing: update t_storage set count = count-50 where id = 1 
==> Parameters: 
2020-05-22 09:15:54.802  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT id, count FROM t_storage WHERE id = 1 FOR UPDATE
2020-05-22 09:15:54.802  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@1460703d, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@63ac0932), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@63ac0932, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=15, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@44882968, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@7694cb15, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@15dbf90b, containsSubquery=false)
2020-05-22 09:15:54.802  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, count FROM t_storage1 WHERE id = 1 FOR UPDATE
2020-05-22 09:15:54.804  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Logic SQL: update t_storage set count = count-50 where id = 1
2020-05-22 09:15:54.804  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@7029adff, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7341e361), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7341e361)
2020-05-22 09:15:54.804  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: update t_storage1 set count = count-50 where id = 1
2020-05-22 09:15:54.817  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT id, count FROM t_storage WHERE id in (?)
2020-05-22 09:15:54.817  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@44300acf, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4680ebc4), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4680ebc4, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=15, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@3e86252e, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@42a08374, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@e1a5a04, containsSubquery=false)
2020-05-22 09:15:54.817  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, count FROM t_storage1 WHERE id in (?) ::: [1]
<==    Updates: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
Transaction synchronization committing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
Transaction synchronization deregistering SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
Transaction synchronization closing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@48378073]
2020-05-22 09:15:54.885  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:15:54.885  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@23a45738, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6d82d00), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6d82d00, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@10b328c0, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@48eb75ea], parameters=[2012243537, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@d6553184, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:15:54.885  INFO 9580 --- [:20888-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243537, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@d6553184, 0]
2020-05-22 09:15:55.528  INFO 9580 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243535,branchId=2012243537,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds2,applicationData=null
2020-05-22 09:15:55.529  INFO 9580 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch committing: 192.168.10.107:8091:2012243535 2012243537 jdbc:mysql://127.0.0.1:3306/ds2 null
2020-05-22 09:15:55.529  INFO 9580 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch commit result: PhaseTwo_Committed
2020-05-22 09:15:55.532  INFO 9580 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?) 
2020-05-22 09:15:55.532  INFO 9580 --- [  AsyncWorker_1] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@6eb59ea6, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6a8a17a8), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6a8a17a8)
2020-05-22 09:15:55.532  INFO 9580 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?)  ::: [2012243537, 192.168.10.107:8091:2012243535]

```

##### 6.1.4 OrderService 服务日志

```bash
2020-05-22 09:15:54.956  INFO 6268 --- [:20880-thread-3] i.s.s.i.o.dubbo.OrderDubboServiceImpl    : 全局事务id ：192.168.10.107:8091:2012243535
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@23bc1e40] was not registered for synchronization because synchronization is not active
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@2d7be447] will not be managed by Spring
==>  Preparing: insert into t_order values(?,?,1,?,50,100.0) 
==> Parameters: 1263639694564524034(String), 4e7d738e311a40cd8176795aafb8a247(String), C201901140001(String)
2020-05-22 09:15:55.132  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Logic SQL: insert into t_order values(?,?,1,?,50,100.0)
2020-05-22 09:15:55.132  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@385e4984, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@29447530), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@29447530, columnNames=[id, order_no, user_id, commodity_code, count, amount], insertValueContexts=[InsertValueContext(parametersCount=3, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=27, stopIndex=27, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=29, stopIndex=29, parameterMarkerIndex=1), LiteralExpressionSegment(startIndex=31, stopIndex=31, literals=1), ParameterMarkerExpressionSegment(startIndex=33, stopIndex=33, parameterMarkerIndex=2), LiteralExpressionSegment(startIndex=35, stopIndex=36, literals=50), LiteralExpressionSegment(startIndex=38, stopIndex=42, literals=100.0)], parameters=[1263639694564524034, 4e7d738e311a40cd8176795aafb8a247, C201901140001])], generatedKeyContext=Optional.empty)
2020-05-22 09:15:55.132  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: insert into t_order1 values(?, ?, 1, ?, 50, 100.0) ::: [1263639694564524034, 4e7d738e311a40cd8176795aafb8a247, C201901140001]
2020-05-22 09:15:55.135  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM t_order WHERE id in (?)
2020-05-22 09:15:55.135  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@64adbefc, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@5de023c8), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@5de023c8, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=order_no, alias=Optional.empty), ColumnProjection(owner=null, name=user_id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@578730b1, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@c691133, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@7ecd35e7, containsSubquery=false)
2020-05-22 09:15:55.135  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order0 WHERE id in (?) ::: [1263639694564524034]
2020-05-22 09:15:55.135  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order1 WHERE id in (?) ::: [1263639694564524034]
2020-05-22 09:15:55.202  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:15:55.202  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@79890300, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1f521715), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1f521715, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@6d458949, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@3204530f], parameters=[2012243541, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@c45d57c1, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:15:55.203  INFO 6268 --- [:20880-thread-3] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243541, 192.168.10.107:8091:2012243535, serializer=jackson, javax.sql.rowset.serial.SerialBlob@c45d57c1, 0]
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@23bc1e40]
2020-05-22 09:15:55.596  INFO 6268 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243535,branchId=2012243541,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds1,applicationData=null
2020-05-22 09:15:55.597  INFO 6268 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch committing: 192.168.10.107:8091:2012243535 2012243541 jdbc:mysql://127.0.0.1:3306/ds1 null
2020-05-22 09:15:55.597  INFO 6268 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch commit result: PhaseTwo_Committed
2020-05-22 09:15:56.526  INFO 6268 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?) 
2020-05-22 09:15:56.526  INFO 6268 --- [  AsyncWorker_1] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@3d80f8b5, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1c45d32), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1c45d32)
2020-05-22 09:15:56.526  INFO 6268 --- [  AsyncWorker_1] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE  branch_id IN  (?)  AND xid IN  (?)  ::: [2012243541, 192.168.10.107:8091:2012243535]

```

### 6. 2 发送一个下单请求(异常回滚情况)
我们`samples-business`将`BusinessServiceImpl`的`handleBusiness2` 下面的代码去掉注释

```java
if (!flag) {
  throw new RuntimeException("测试抛异常后，分布式事务回滚！");
}
```
使用postman 发送 ：[http://localhost:8104/business/dubbo/buy2](http://localhost:8104/business/dubbo/buy2) 


```json
{
    "userId":1,
    "commodityCode":"C201901140001",
    "name":"fan",
    "count":50,
    "amount":"100"
}
```

响应结果：

```json
{
    "timestamp": "2020-05-22T01:27:53.517+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "测试抛异常后，分布式事务回滚！",
    "path": "/business/dubbo/buy2"
}
```
##### 6.2.1 BusinessService 服务日志

```shell
2020-05-22 09:27:52.386  INFO 13384 --- [nio-8104-exec-7] i.s.s.i.c.controller.BusinessController  : 请求参数：BusinessDTO(userId=1, commodityCode=C201901140001, name=fan, count=50, amount=100)
2020-05-22 09:27:52.422  INFO 13384 --- [nio-8104-exec-7] i.seata.tm.api.DefaultGlobalTransaction  : Begin new global transaction [192.168.10.107:8091:2012243545]
2020-05-22 09:27:52.422  INFO 13384 --- [nio-8104-exec-7] i.s.s.i.c.service.BusinessServiceImpl    : 开始全局事务，XID = 192.168.10.107:8091:2012243545
2020-05-22 09:27:53.515  INFO 13384 --- [nio-8104-exec-7] i.seata.tm.api.DefaultGlobalTransaction  : [192.168.10.107:8091:2012243545] rollback status: Rollbacked
2020-05-22 09:27:53.516 ERROR 13384 --- [nio-8104-exec-7] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is java.lang.RuntimeException: 测试抛异常后，分布式事务回滚！] with root cause

java.lang.RuntimeException: 测试抛异常后，分布式事务回滚！
	at io.seata.samples.integration.call.service.BusinessServiceImpl.handleBusiness2(BusinessServiceImpl.java:99) ~[classes/:na]
	at io.seata.samples.integration.call.service.BusinessServiceImpl$$FastClassBySpringCGLIB$$2ab3d645.invoke(<generated>) ~[classes/:na]
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218) ~[spring-core-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.invokeJoinpoint(CglibAopProxy.java:771) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:749) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at io.seata.spring.annotation.GlobalTransactionalInterceptor$1.execute(GlobalTransactionalInterceptor.java:109) ~[seata-all-1.2.0.jar:1.2.0]
	at io.seata.tm.api.TransactionalTemplate.execute(TransactionalTemplate.java:104) ~[seata-all-1.2.0.jar:1.2.0]
	at io.seata.spring.annotation.GlobalTransactionalInterceptor.handleGlobalTransaction(GlobalTransactionalInterceptor.java:106) ~[seata-all-1.2.0.jar:1.2.0]
	at io.seata.spring.annotation.GlobalTransactionalInterceptor.invoke(GlobalTransactionalInterceptor.java:83) ~[seata-all-1.2.0.jar:1.2.0]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:749) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:691) ~[spring-aop-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at io.seata.samples.integration.call.service.BusinessServiceImpl$$EnhancerBySpringCGLIB$$11be97b5.handleBusiness2(<generated>) ~[classes/:na]
	at io.seata.samples.integration.call.controller.BusinessController.handleBusiness2(BusinessController.java:48) ~[classes/:na]
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_144]
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_144]
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_144]
	at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_144]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:190) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:138) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:105) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:879) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:793) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1040) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:943) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:909) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:660) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:741) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202) ~[tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:373) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1594) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) [na:1.8.0_144]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) [na:1.8.0_144]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) [tomcat-embed-core-9.0.33.jar:9.0.33]
	at java.lang.Thread.run(Thread.java:748) [na:1.8.0_144]
```

##### 6.2.2 AccountService 服务日志

```bash
2020-05-22 09:27:52.635  INFO 8792 --- [:20883-thread-4] i.s.s.i.a.dubbo.AccountDubboServiceImpl  : 全局事务id ：192.168.10.107:8091:2012243545
Creating a new SqlSession
Registering transaction synchronization for SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@58622fda]
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@529a44aa] will be managed by Spring
==>  Preparing: update t_account set amount = amount-100.0 where id = 1 
==> Parameters: 
2020-05-22 09:27:52.637  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT id, amount FROM t_account WHERE id = 1 FOR UPDATE
2020-05-22 09:27:52.637  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@60fae881, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1798d09d), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1798d09d, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=16, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@5ecd214b, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@2645a0de, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@26fa0fa8, containsSubquery=false)
2020-05-22 09:27:52.637  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, amount FROM t_account1 WHERE id = 1 FOR UPDATE
2020-05-22 09:27:52.640  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Logic SQL: update t_account set amount = amount-100.0 where id = 1
2020-05-22 09:27:52.640  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@5ba704b6, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@73f934c4), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@73f934c4)
2020-05-22 09:27:52.640  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: update t_account1 set amount = amount-100.0 where id = 1
2020-05-22 09:27:52.643  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT id, amount FROM t_account WHERE id in (?)
2020-05-22 09:27:52.643  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@697bcc02, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@38a68471), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@38a68471, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=16, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@77bfb086, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@4ddaf5a1, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@13ea38b2, containsSubquery=false)
2020-05-22 09:27:52.643  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, amount FROM t_account1 WHERE id in (?) ::: [1]
<==    Updates: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@58622fda]
Transaction synchronization committing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@58622fda]
Transaction synchronization deregistering SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@58622fda]
Transaction synchronization closing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@58622fda]
2020-05-22 09:27:52.757  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:27:52.757  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@354923b3, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4d520553), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4d520553, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@5fd3f282, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@2e3bb335], parameters=[2012243550, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@4e4593ee, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:27:52.757  INFO 8792 --- [:20883-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243550, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@4e4593ee, 0]
2020-05-22 09:27:53.190  INFO 8792 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243545,branchId=2012243550,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds0,applicationData=null
2020-05-22 09:27:53.190  INFO 8792 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacking: 192.168.10.107:8091:2012243545 2012243550 jdbc:mysql://127.0.0.1:3306/ds0
2020-05-22 09:27:53.191  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE
2020-05-22 09:27:53.191  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@6481a025, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@59a7b23f), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@59a7b23f, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=branch_id, alias=Optional.empty), ColumnProjection(owner=null, name=xid, alias=Optional.empty), ColumnProjection(owner=null, name=context, alias=Optional.empty), ColumnProjection(owner=null, name=rollback_info, alias=Optional.empty), ColumnProjection(owner=null, name=log_status, alias=Optional.empty), ColumnProjection(owner=null, name=log_created, alias=Optional.empty), ColumnProjection(owner=null, name=log_modified, alias=Optional.empty), ColumnProjection(owner=null, name=ext, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@5760477b, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@535fd94f, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@165d4d40, containsSubquery=false)
2020-05-22 09:27:53.191  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE ::: [2012243550, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:53.193  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM t_account WHERE id in (?)
2020-05-22 09:27:53.193  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@761b9b19, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2781f95b), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2781f95b, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@a39c945, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@7672b20f, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@7de97fec, containsSubquery=false)
2020-05-22 09:27:53.194  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_account1 WHERE id in (?) ::: [1]
2020-05-22 09:27:53.195  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: UPDATE t_account SET amount = ? WHERE id = ?
2020-05-22 09:27:53.195  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@4df03ffe, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@113d823e), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@113d823e)
2020-05-22 09:27:53.195  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: UPDATE t_account1 SET amount = ? WHERE id = ? ::: [3600.0, 1]
2020-05-22 09:27:53.212  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE branch_id = ? AND xid = ?
2020-05-22 09:27:53.212  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@2baab4f5, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@723ca8dc), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@723ca8dc)
2020-05-22 09:27:53.212  INFO 8792 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE branch_id = ? AND xid = ? ::: [2012243550, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:53.286  INFO 8792 --- [atch_RMROLE_1_8] i.s.r.d.undo.AbstractUndoLogManager      : xid 192.168.10.107:8091:2012243545 branch 2012243550, undo_log deleted with GlobalFinished
2020-05-22 09:27:53.287  INFO 8792 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacked result: PhaseTwo_Rollbacked
```

##### 6.2.3 StorageService 服务日志

```bash
2020-05-22 09:27:52.425  INFO 9580 --- [:20888-thread-4] i.s.s.i.s.dubbo.StorageDubboServiceImpl  : 全局事务id ：192.168.10.107:8091:2012243545
Creating a new SqlSession
Registering transaction synchronization for SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@3052f6a2] will be managed by Spring
==>  Preparing: SELECT id,commodity_code,name,count FROM t_storage WHERE (commodity_code = ?) 
==> Parameters: C201901140001(String)
2020-05-22 09:27:52.428  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT  id,commodity_code,name,count  FROM t_storage 
 
 WHERE (commodity_code = ?)
2020-05-22 09:27:52.428  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@5ec9be76, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@75506ecc), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@75506ecc, projectionsContext=ProjectionsContext(startIndex=8, stopIndex=35, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=name, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@1b7a39b9, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@44efbcfd, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@632fb524, containsSubquery=false)
2020-05-22 09:27:52.428  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT  id,commodity_code,name,count  FROM t_storage0 
 
 WHERE (commodity_code = ?) ::: [C201901140001]
2020-05-22 09:27:52.428  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT  id,commodity_code,name,count  FROM t_storage1 
 
 WHERE (commodity_code = ?) ::: [C201901140001]
<==    Columns: id, commodity_code, name, count
<==        Row: 1, C201901140001, 水杯, 600
<==      Total: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
Fetched SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2] from current transaction
==>  Preparing: update t_storage set count = count-50 where id = 1 
==> Parameters: 
2020-05-22 09:27:52.432  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT id, count FROM t_storage WHERE id = 1 FOR UPDATE
2020-05-22 09:27:52.432  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@1460703d, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@782b9d5a), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@782b9d5a, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=15, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@1131e855, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@585126e2, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@2209f5bf, containsSubquery=false)
2020-05-22 09:27:52.432  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, count FROM t_storage1 WHERE id = 1 FOR UPDATE
2020-05-22 09:27:52.433  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Logic SQL: update t_storage set count = count-50 where id = 1
2020-05-22 09:27:52.433  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@7029adff, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@18814e21), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@18814e21)
2020-05-22 09:27:52.433  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: update t_storage1 set count = count-50 where id = 1
2020-05-22 09:27:52.445  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT id, count FROM t_storage WHERE id in (?)
2020-05-22 09:27:52.445  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@44300acf, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@247fbd61), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@247fbd61, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=15, distinctRow=false, projections=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@41198f32, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@53fb3176, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@2a5b57d9, containsSubquery=false)
2020-05-22 09:27:52.445  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT id, count FROM t_storage1 WHERE id in (?) ::: [1]
<==    Updates: 1
Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
Transaction synchronization committing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
Transaction synchronization deregistering SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
Transaction synchronization closing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3aaa9dd2]
2020-05-22 09:27:52.541  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:27:52.541  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@23a45738, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7401fc21), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@7401fc21, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@10b328c0, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@48eb75ea], parameters=[2012243547, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@e6006345, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:27:52.541  INFO 9580 --- [:20888-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243547, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@e6006345, 0]
2020-05-22 09:27:53.340  INFO 9580 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243545,branchId=2012243547,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds2,applicationData=null
2020-05-22 09:27:53.340  INFO 9580 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacking: 192.168.10.107:8091:2012243545 2012243547 jdbc:mysql://127.0.0.1:3306/ds2
2020-05-22 09:27:53.340  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE
2020-05-22 09:27:53.340  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@361d5f71, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@12dcbe90), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@12dcbe90, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=branch_id, alias=Optional.empty), ColumnProjection(owner=null, name=xid, alias=Optional.empty), ColumnProjection(owner=null, name=context, alias=Optional.empty), ColumnProjection(owner=null, name=rollback_info, alias=Optional.empty), ColumnProjection(owner=null, name=log_status, alias=Optional.empty), ColumnProjection(owner=null, name=log_created, alias=Optional.empty), ColumnProjection(owner=null, name=log_modified, alias=Optional.empty), ColumnProjection(owner=null, name=ext, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@1c1b52bb, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@1824d5e0, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@5d265880, containsSubquery=false)
2020-05-22 09:27:53.341  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE ::: [2012243547, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:53.343  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM t_storage WHERE id in (?)
2020-05-22 09:27:53.343  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@19c73cee, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6c3144f4), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6c3144f4, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=name, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@52840747, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@3d09a7cf, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@5dbcf0f1, containsSubquery=false)
2020-05-22 09:27:53.343  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_storage1 WHERE id in (?) ::: [1]
2020-05-22 09:27:53.344  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: UPDATE t_storage SET count = ? WHERE id = ?
2020-05-22 09:27:53.344  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: UpdateStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement@2c4d9b68, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@49808f57), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@49808f57)
2020-05-22 09:27:53.344  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: UPDATE t_storage1 SET count = ? WHERE id = ? ::: [600, 1]
2020-05-22 09:27:53.346  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE branch_id = ? AND xid = ?
2020-05-22 09:27:53.346  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@9b8689c, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@39477e77), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@39477e77)
2020-05-22 09:27:53.346  INFO 9580 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE branch_id = ? AND xid = ? ::: [2012243547, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:53.399  INFO 9580 --- [atch_RMROLE_1_8] i.s.r.d.undo.AbstractUndoLogManager      : xid 192.168.10.107:8091:2012243545 branch 2012243547, undo_log deleted with GlobalFinished
2020-05-22 09:27:53.399  INFO 9580 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacked result: PhaseTwo_Rollbacked
```

##### 6.2.4 OrderService 服务日志

```bash
2020-05-22 09:27:52.615  INFO 6268 --- [:20880-thread-4] i.s.s.i.o.dubbo.OrderDubboServiceImpl    : 全局事务id ：192.168.10.107:8091:2012243545
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3be964] was not registered for synchronization because synchronization is not active
JDBC Connection [io.seata.rm.datasource.ConnectionProxy@efc0713] will not be managed by Spring
==>  Preparing: insert into t_order values(?,?,1,?,50,100.0) 
==> Parameters: 1263642704673898497(String), 72ac94267b7f4f729b42ff72e641a0c4(String), C201901140001(String)
2020-05-22 09:27:52.799  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Logic SQL: insert into t_order values(?,?,1,?,50,100.0)
2020-05-22 09:27:52.799  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@385e4984, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@ac38214), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@ac38214, columnNames=[id, order_no, user_id, commodity_code, count, amount], insertValueContexts=[InsertValueContext(parametersCount=3, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=27, stopIndex=27, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=29, stopIndex=29, parameterMarkerIndex=1), LiteralExpressionSegment(startIndex=31, stopIndex=31, literals=1), ParameterMarkerExpressionSegment(startIndex=33, stopIndex=33, parameterMarkerIndex=2), LiteralExpressionSegment(startIndex=35, stopIndex=36, literals=50), LiteralExpressionSegment(startIndex=38, stopIndex=42, literals=100.0)], parameters=[1263642704673898497, 72ac94267b7f4f729b42ff72e641a0c4, C201901140001])], generatedKeyContext=Optional.empty)
2020-05-22 09:27:52.799  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: insert into t_order1 values(?, ?, 1, ?, 50, 100.0) ::: [1263642704673898497, 72ac94267b7f4f729b42ff72e641a0c4, C201901140001]
2020-05-22 09:27:52.802  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM t_order WHERE id in (?)
2020-05-22 09:27:52.802  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@64adbefc, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@68010a02), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@68010a02, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=order_no, alias=Optional.empty), ColumnProjection(owner=null, name=user_id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@2feef267, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@4a8eb7b2, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@13b4c98b, containsSubquery=false)
2020-05-22 09:27:52.802  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order0 WHERE id in (?) ::: [1263642704673898497]
2020-05-22 09:27:52.802  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order1 WHERE id in (?) ::: [1263642704673898497]
2020-05-22 09:27:52.875  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Logic SQL: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now())
2020-05-22 09:27:52.875  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : SQLStatement: InsertStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement@79890300, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2160f297), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2160f297, columnNames=[branch_id, xid, context, rollback_info, log_status, log_created, log_modified], insertValueContexts=[InsertValueContext(parametersCount=5, valueExpressions=[ParameterMarkerExpressionSegment(startIndex=109, stopIndex=109, parameterMarkerIndex=0), ParameterMarkerExpressionSegment(startIndex=112, stopIndex=112, parameterMarkerIndex=1), ParameterMarkerExpressionSegment(startIndex=115, stopIndex=115, parameterMarkerIndex=2), ParameterMarkerExpressionSegment(startIndex=118, stopIndex=118, parameterMarkerIndex=3), ParameterMarkerExpressionSegment(startIndex=121, stopIndex=121, parameterMarkerIndex=4), org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@6d458949, org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ExpressionProjectionSegment@3204530f], parameters=[2012243552, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@582376a0, 0])], generatedKeyContext=Optional.empty)
2020-05-22 09:27:52.875  INFO 6268 --- [:20880-thread-4] ShardingSphere-SQL                       : Actual SQL: ds0 ::: INSERT INTO undo_log (branch_id, xid, context, rollback_info, log_status, log_created, log_modified) VALUES (?, ?, ?, ?, ?, now(), now()) ::: [2012243552, 192.168.10.107:8091:2012243545, serializer=jackson, javax.sql.rowset.serial.SerialBlob@582376a0, 0]
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@3be964]
2020-05-22 09:27:52.980  INFO 6268 --- [atch_RMROLE_1_8] i.s.core.rpc.netty.RmMessageListener     : onMessage:xid=192.168.10.107:8091:2012243545,branchId=2012243552,branchType=AT,resourceId=jdbc:mysql://127.0.0.1:3306/ds1,applicationData=null
2020-05-22 09:27:52.980  INFO 6268 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacking: 192.168.10.107:8091:2012243545 2012243552 jdbc:mysql://127.0.0.1:3306/ds1
2020-05-22 09:27:52.980  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE
2020-05-22 09:27:52.980  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@24965d4, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1a720567), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@1a720567, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=branch_id, alias=Optional.empty), ColumnProjection(owner=null, name=xid, alias=Optional.empty), ColumnProjection(owner=null, name=context, alias=Optional.empty), ColumnProjection(owner=null, name=rollback_info, alias=Optional.empty), ColumnProjection(owner=null, name=log_status, alias=Optional.empty), ColumnProjection(owner=null, name=log_created, alias=Optional.empty), ColumnProjection(owner=null, name=log_modified, alias=Optional.empty), ColumnProjection(owner=null, name=ext, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@14b5e859, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@108a6e17, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@5588e262, containsSubquery=false)
2020-05-22 09:27:52.981  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM undo_log WHERE branch_id = ? AND xid = ? FOR UPDATE ::: [2012243552, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:52.983  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: SELECT * FROM t_order WHERE id in (?)
2020-05-22 09:27:52.983  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: SelectStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement@64adbefc, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6497500b), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@6497500b, projectionsContext=ProjectionsContext(startIndex=7, stopIndex=7, distinctRow=false, projections=[ShorthandProjection(owner=Optional.empty, actualColumns=[ColumnProjection(owner=null, name=id, alias=Optional.empty), ColumnProjection(owner=null, name=order_no, alias=Optional.empty), ColumnProjection(owner=null, name=user_id, alias=Optional.empty), ColumnProjection(owner=null, name=commodity_code, alias=Optional.empty), ColumnProjection(owner=null, name=count, alias=Optional.empty), ColumnProjection(owner=null, name=amount, alias=Optional.empty)])]), groupByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext@272e6058, orderByContext=org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext@35a71d2d, paginationContext=org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext@369b70d4, containsSubquery=false)
2020-05-22 09:27:52.983  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order0 WHERE id in (?) ::: [1263642704673898497]
2020-05-22 09:27:52.983  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: SELECT * FROM t_order1 WHERE id in (?) ::: [1263642704673898497]
2020-05-22 09:27:52.985  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: DELETE FROM t_order WHERE id = ?
2020-05-22 09:27:52.985  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@2f28ed8, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2778c7cc), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@2778c7cc)
2020-05-22 09:27:52.985  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM t_order0 WHERE id = ? ::: [1263642704673898497]
2020-05-22 09:27:52.985  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM t_order1 WHERE id = ? ::: [1263642704673898497]
2020-05-22 09:27:53.022  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Logic SQL: DELETE FROM undo_log WHERE branch_id = ? AND xid = ?
2020-05-22 09:27:53.022  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : SQLStatement: DeleteStatementContext(super=CommonSQLStatementContext(sqlStatement=org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement@15c0ff43, tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4cfe761d), tablesContext=org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext@4cfe761d)
2020-05-22 09:27:53.022  INFO 6268 --- [atch_RMROLE_1_8] ShardingSphere-SQL                       : Actual SQL: ds0 ::: DELETE FROM undo_log WHERE branch_id = ? AND xid = ? ::: [2012243552, 192.168.10.107:8091:2012243545]
2020-05-22 09:27:53.127  INFO 6268 --- [atch_RMROLE_1_8] i.s.r.d.undo.AbstractUndoLogManager      : xid 192.168.10.107:8091:2012243545 branch 2012243552, undo_log deleted with GlobalFinished
2020-05-22 09:27:53.128  INFO 6268 --- [atch_RMROLE_1_8] io.seata.rm.AbstractRMHandler            : Branch Rollbacked result: PhaseTwo_Rollbacked
```

我们查看数据库数据，已经回滚，和上面的数据一致。

到这里一个简单的`seata1.2.0`、`sharding-sphere4.1.0`和`dubbo2.7.5` 的整合案例基本就分析结束。感谢你的学习。如果想交流的可以私信我。
