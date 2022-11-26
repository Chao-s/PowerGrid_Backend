# Grid Graph Processing Demo

## 项目介绍
+ 参考https://github.com/Rapter1990/SpringBootNeo4jShortestPath而搭建了各目录，将来写测试时也可参考
+ model/下为实体类：docGraph为按文档建的实体类，其中Relation仅供参考关系实体类的建法；\
nariGraph为南瑞数据库中实际上的实体类；直接在model/下的是为mysql而建的实体类，AutoDevice映射自动化开关
+ 直接在repository/下AutoDeviceRepository的为mysql的jpa接口，默认提供固定格式的crud，可写自定义sql语句实现额外方法
+ repository/graph/NodeRepository为neo4j实体类的通用SDN接口
+ 建仓库映射实体类实现存取是SDN（Spring Data Neo4j，项目中使用SDN6）的思路，还可以使用neo4j-java-driver直接执行cypher语句，\
在util/Neo4JUtil，util裁剪自https://github.com/MiracleTanC/Neo4j-KGBuilder项目
  + 在上述高度封装的操作方法之外，考虑如何使用原生java neo4j API的方法和节点类型操纵neo4j
+ service/中，DriverGraphService仅将https://github.com/MiracleTanC/Neo4j-KGBuilder中的KGGraphRepository进行搬运和粗略调整；\
SDNGraphService计划通过Neo4jTemplate（SDN的底层）/NodeRepository实现通用的crud
+ MyTest/中为各方法的测试（没有事务回滚，需在测试环境中运行），先在此处测试后再搬至项目中，其中添加自动化开关的方法运行过慢，\
需要在批量处理减少数据库连接等方面进行优化，读取自动化开关的方式也可写自定义sql优化；\
上下游搜索算法未实现，直接搬带换行的cypher语句不知道如何执行
+ 瓶颈：通过java操作neo4j以及映射节点的方式，需查阅文档熟悉一下；前期大部分时间耗在学习SDN和通用实体类映射上了，但也许直接关注节点操作节点就好

## 主要任务
+ 利用现有的通用SDN操作(NodeRepository/Neo4jTemplate)或原生java neo4j API实现操作节点，写上下游搜索算法
+ 实现自动化开关添加方法的优化（目前慢到根本跑不完）

## maybe we could try（不是主要矛盾）
+ 对各实体类的通用crud方法（已通过类继承大体实现）的其他可能实现方式
  + repository创造器，达到使用所有repository操纵实体类的效果:
    + RepositoryFactorySupport factory = … // Instantiate factory here\
      UserRepository repository = factory.getRepository(UserRepository.class);\
      见：https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#repositories
  + 通过custom的repository继承多个已有repository，达到通用repository的效果

## 观察记录（与理解项目无关，可略去不看）
+ 对比neo4jRepository和neo4jTemplate的findById，前者的返回类型似乎一定要有@Node否则无法映射，但标签无所谓，然后返回对象可以在类型的子类中寻找匹配；\
后者根据传入的类型的class的标签进行搜索，标签决定搜索结果，因此传入的类也需要有@Node，然后返回对象可以在类型的子类中寻找匹配；\
二者似乎都不能用接口类承接结果


