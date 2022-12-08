# Grid Graph Processing Demo

## algo目录介绍
### 介绍
+ 实现上下游搜索算法部分的主要工作在：algo/service/graph/algo,algo/util/下的Neo4jUtil、PrintUtil
+ 参考https://github.com/Rapter1990/SpringBootNeo4jShortestPath而搭建了各目录，将来写测试时也可参考
+ model/下为实体类：docGraph为按文档建的实体类，其中Relation仅供参考关系实体类的建法，目前关系实体类尚未建好；\
nariGraph为南瑞数据库中实际上的实体类；直接在model/下的是为mysql而建的实体类，AutoDevice映射自动化开关
+ 直接在repository/下AutoDeviceRepository的为mysql的jpa接口，默认提供固定格式的crud，可写自定义sql语句实现额外方法
+ 操作neo4j方法1：SDN（Spring Data Neo4j，项目中使用SDN6），建仓库映射实体类实现存取，repository/graph/NodeRepository为neo4j实体类的通用SDN接口（关系类相关方法尚未完善）
+ 操作neo4j方法2：neo4j-java-driver直接执行cypher语句和操纵节点，在util/Neo4JUtil（已完成深度修改），util借鉴自https://github.com/MiracleTanC/Neo4j-KGBuilder项目
+ service/中，algo/GraphSearchAlgorithm为主要工作，实现搜索算法；DriverGraphService仅将https://github.com/MiracleTanC/Neo4j-KGBuilder中的KGGraphRepository进行搬运和粗略调整；
而SDNGraphService计划通过Neo4jTemplate（SDN的底层）/NodeRepository实现通用的crud
+ MyTest/中为各方法的测试（没有事务回滚，需在测试环境中运行），先在此处测试后再搬至项目中，其中添加自动化开关的方法运行过慢，\
需要在批量处理减少数据库连接等方面进行优化；\

### 主要任务
+ 基于neo4j-java-driver操作节点，完善上下游搜索算法（已完成基本功能）
  + https://neo4j.com/docs/api/java-driver/4.4/
  + https://github.com/neo4j/neo4j-documentation/blob/4.4/embedded-examples/src/main/java/org/neo4j/examples/socnet/Person.java
  + https://github.com/neo4j-examples/movies-java-bolt/blob/main/src/main/java/example/movies/backend/MovieService.java
+ 随时可做的任务！！建立DeviceNode extends HashMap<String,Object>，集成所有的internalId,RealId,labels信息，取代Neo4jUtil中的所有相关属性与方法
+ ——————上面最重要——————
+ 实现自动化开关添加方法的优化（目前慢到根本跑不完，所以是实时查询）
+ 思考：节点用Node（使用tx.findNode/CreateNode之类）还是Json对象还是自定义通用实体类，以及是否需要借鉴实体类里藏Node成员变量的做法
  + https://neo4j.com/docs/java-reference/4.3/extending-neo4j/project-setup/
+ 实体类（包括各设备节点与关系）与通用SDN操作(NodeRepository/Neo4jTemplate)的完善（优先级不高）；思考：是否用map<String,Object>和projection机制尝试构建通用实体类
+ 有以下使用cypher查询的方法，用哪个好：
  + 纯neo4j-java-driver：session+tx（目前的Neo4jUtil为driver最基本的使用方式）
  + Cypher-DSL（可用用Neo4jTemplate执行Statement）
    + Statement statement = Cypher.match(shortestPath)\
      .with(p, listWith(name("n"))\
      .in(Functions.nodes(shortestPath))\
      .where(anyNode().named("n").hasLabels("Movie")).returning().as("mn")\
      )\
      .unwind(name("mn")).as("m")\
      .with(p, name("m"))\
      .match(node("Person").named("d")\
      .relationshipTo(anyNode("m"), "DIRECTED").named("r")\
      )\
      .returning(p, Functions.collect(name("r")), Functions.collect(name("d")))\
      .build();
  + Neo4jClient类
    + public Collection<Result> findRelationsToMovie(MovieEntity movie) {\
      return this.neo4jClient\
      .query(""\
      "MATCH (people:Person)-[relatedTo]-(:Movie {title: $title}) "\
      "RETURN people.name AS name, "\
      "       Type(relatedTo) as typeOfRelation"\
      )\
      .bind(movie.getTitle()).to("title")\
      .fetchAs(Result.class)\
      .mappedBy((typeSystem, record) -> new Result(record.get("name").asString(),\
      record.get("typeOfRelation").asString()))\
      .all();\
      }
  + pom添加哪些依赖好（harness等）
    + https://neo4j.com/docs/java-reference/4.3/extending-neo4j/project-setup/

### maybe we could try（次要矛盾中的次要矛盾，边缘问题）
+ 对各实体类的通用crud方法（已通过类继承大体实现）的其他可能实现方式
  + repository创造器，达到使用所有repository操纵实体类的效果:
    + RepositoryFactorySupport factory = … // Instantiate factory here\
      UserRepository repository = factory.getRepository(UserRepository.class);\
      见：https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#repositories
  + 通过custom的repository继承多个已有repository，达到通用repository的效果

### 观察记录（与理解项目无关，可略去不看）
#### SDN用法
+ 对比neo4jRepository和neo4jTemplate的findById，前者的返回类型似乎一定要有@Node否则无法映射，但标签无所谓，然后返回对象可以在类型的子类中寻找匹配；\
后者根据传入的类型的class的标签进行搜索，标签决定搜索结果，因此传入的类也需要有@Node，然后返回对象可以在类型的子类中寻找匹配；\
二者似乎都不能用接口类承接结果




## construct目录介绍
暂无