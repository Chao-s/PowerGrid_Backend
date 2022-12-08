package com.gridgraphprocessing.algo.util;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
@Lazy(false)
public class Neo4jUtil implements AutoCloseable {//仅仅将https://github.com/MiracleTanC/Neo4j-KGBuilder项目中Neo4JUtil搬过来
    //默认执行方式均为auto-commit方式(session.run())，call {} in transactions(仅仅call的话没问题)和periodic commit的语句必须用auto-commit执行
    //注意：原方法用uuid接受entity.id()，现为表述清晰，全部改为internalId，但DriverGraphService中仍存在uuid，其属性含义有待判断
    //所有传出neo4j底层type数据（Path/Node等）的方法都标记Raw，不要修改Path/Node这类底层type数据

    private static Driver neo4jDriver;

    private static final Logger log = LoggerFactory.getLogger(Neo4jUtil.class);

    private static final String[] commonLabels = {"CONDUCTIVEEQUIPMENT","DMS","EMS"};//似乎说数组不要加final

    private static final String internalIdField = "internalId";

    private static final String labelsField = "labelStringIterator";

    private static final String realIdField = "id";

//    public static String getLabelsField(){return Neo4jUtil.labelsField;}

    public static String getRealIdField(){return Neo4jUtil.realIdField;}

    @Autowired
    @Lazy
    public void setNeo4jDriver(Driver neo4jDriver) {
        Neo4jUtil.neo4jDriver = neo4jDriver;
    }

    @Override
    public void close() throws Exception {
        neo4jDriver.close();
    }

    /**
     * 测试neo4j连接是否打开
     */
    public static boolean isNeo4jOpen() {
        try (Session session = neo4jDriver.session()) {
            log.debug("连接成功：" + session.isOpen());
            return session.isOpen();
        }
    }

    /**
     * neo4j驱动执行cypher
     *
     * @param cypherSql cypherSql
     */

    public static void runCypherSql(String cypherSql) {
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            session.run(cypherSql);
        }
    }

    public static boolean batchRunCypherWithTx(List<String> cyphers) {//避免频繁的数据库连接
        Session session = neo4jDriver.session();
        try (Transaction tx = session.beginTransaction()) {
            for (String cypher : cyphers) {
                tx.run(cypher);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        }
        return true;
    }

    public <T> List<T> readCyphers(String cypherSql, Function<Record, T> mapper) {//可改善
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            return result.list(mapper);
        }
    }

    //获取cypher语句的直接结果
    public static List<Record> getRawRecords(String cypherSql){//返回Result时报错，说已经被消费掉、传出去
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            return session.run(cypherSql).list();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public static<T> List<T> getRawRecords(String cypherSql,Function<Record,T> function){//返回Result时报错，说已经被消费掉、传出去
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            return session.run(cypherSql).list(function);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    //路径可能重复
    public static List<Path> getRawPaths(String cypherSql){
        return Neo4jUtil.getRawRecords(cypherSql, record -> record.values().get(0).asPath());
    }

    //尽管set中路径本身不会重复，但路径之间仍有重复部分，可以用树结构返回，已有nodeTree方法
    public static Set<Path> getRawPathSet(String cypherSql){
        List<Path> paths = Neo4jUtil.getRawRecords(cypherSql, record -> record.values().get(0).asPath());
        assert paths != null;
        return new HashSet<>(paths);
    }

    public static List<HashMap<String,Object>> getNodeTree(String cypherSql,Map<Integer,LinkedList<Integer>> mapListToTree){
        Collection<Path> paths = getRawPathSet(cypherSql);
        return getNodeTree(paths,mapListToTree);
    }

    public static List<Node> getRawNodeTree(String cypherSql,Map<Integer,LinkedList<Integer>> mapListToTree){//传出原Node而不是Copy，但是一个语句的结果只能consume一次，乍一看不会出错
        Collection<Path> paths = getRawPathSet(cypherSql);
        return getRawNodeTree(paths,mapListToTree);
    }

    public static List<Node> getRawNodeTree(Collection<Path> paths,Map<Integer,LinkedList<Integer>> mapListToTree){
        return getNodeTree(paths,mapListToTree,t->t);//要注意此处是直接t->t传出原Node而不是传出Copy
    }

    //暂时只想到三种实现方式实现树结构路径信息返回：1)返回树结构2)返回无重复节点数组+Map<指示数组内部树结构连接的索引>3)返回无重复节点数组+List<每条路径在数组中对应的位置数组>
    //目前实现第二种方式，用Map记录数组中树结构连接的索引
    //因为没搜到neo4j的tree返回方法而写了此方法，没细搜，有更好方案可以更换
    public static <T> List<T> getNodeTree(Collection<Path> paths,Map<Integer,LinkedList<Integer>> mapListToTree,Function<Node,T> transFunc){//只能解析Tree，当前写法无法应对Net（可按注释另写实现，同时PrintUtil也要另写打印方法）
        if(paths.isEmpty())return new ArrayList<>();
        List<T> nodeTree = new ArrayList<>();
        Set<Long> keysSet = new HashSet<>();
        int treeIdx = -1;
        Map<Long,Integer> idToIdx = new HashMap<>();
        for(Path path:paths){
            int branchIdx = -1;//路径必从同一节点开始，故必有branchIdx>=0
            for (Node node : path.nodes()) {
                boolean notDup = addNodeNotDuplicate(nodeTree,node,keysSet,transFunc);//使用节点内部id为key，因为外置id的字段可能变化
                if(notDup){
                    treeIdx++;
                    idToIdx.put(node.id(),treeIdx);//就算存在回收机制，同一批path结果中的节点内部id应当也是不会重复的
                    if(branchIdx!=-1){//!=-1意味着刚从重复进入不重复，此刻branchIdx指向分支点
                        mapListToTree.putIfAbsent(branchIdx,new LinkedList<>(List.of(branchIdx+1)));
                        mapListToTree.get(branchIdx).add(treeIdx);
                        branchIdx=-1;
                    }
                }else {
                    branchIdx = idToIdx.get(node.id());//亦可为：branchIdx = mapListToTree.containsKey(branchIdx)?idToIdx.get(node.id()):branchIdx+1;
                }
            }
            mapListToTree.put(treeIdx,new LinkedList<>());//若默认已有路径的终点必不为分支点，则此处以空列表表示终点即可，不会在putIfAbsent处出错；否则应写为mapListToTree.putIfAbsent(treeIdx,new LinkedList<>());mapListToTree.get(treeIdx).add(-1);

        }
        return nodeTree;
    }

    public static List<HashMap<String,Object>> getNodeTree(Collection<Path> paths,Map<Integer,LinkedList<Integer>> mapListToTree){
        return getNodeTree(paths,mapListToTree,Neo4jUtil::nodeToMap);
    }

    public static HashMap<Integer,LinkedList<Integer>> treeIdxMapDeepCopy(Map<Integer,LinkedList<Integer>> map){
        HashMap<Integer,LinkedList<Integer>> mapCopy = new HashMap<>(map);
        mapCopy.forEach((key,valList)->{
            mapCopy.put(key, new LinkedList<>(valList));
        });
        return mapCopy;
    }

    public static HashMap<String,Object>nodeToMap(Node node,String internalId){
        HashMap<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> nodeMap = node.asMap();
        for (Entry<String, Object> entry : nodeMap.entrySet()) {
            String key = entry.getKey();
            map.put(key, entry.getValue());
        }
        map.put(Neo4jUtil.internalIdField, internalId);
        map.put(Neo4jUtil.labelsField,node.labels());
        return map;
    }

    public static HashMap<String,Object>nodeToMap(Node node){
        String internalId = getInternalId(node);
        return nodeToMap(node,internalId);
    }

    public static String getInternalId(Entity entity){//该方法最大作用为把.id()显式地标记为图库内部ID，返回值不一定要是String
        return String.valueOf(entity.id());
    }

    public static String getNodeRealId(Node node){return String.valueOf(node.get(Neo4jUtil.realIdField).asLong());}//主要为了封装asLong的信息

    public static String getNodePathFeature(List<Node> nodes,String delimiter){
        return nodes.stream().map((Neo4jUtil::getNodeFeature)).collect(Collectors.joining(delimiter));
    }

    public static String getNodeFeature(Node node){
        return String.format("[%s|id:%s]",getNodePriLabel(node.labels()),getNodeRealId(node));
    }

    public static String getDeviceFeature(HashMap<String,Object> device){
        return String.format("[%s|id:%s]",getNodePriLabel(getDeviceLabels(device)),device.get(Neo4jUtil.realIdField));
    }

    public static String getDeviceToShow(HashMap<String,Object> device){return String.format("%s%s",getDeviceFeature(device),device);}

    public static Iterable<String> getDeviceLabels(HashMap<String,Object> device){return (Iterable<String>) device.get(Neo4jUtil.labelsField);}

    public static String getNodePriLabel(Iterable<String> labels){
        for (String label:labels){
            boolean getFeature = true;
            for(String commonLabel:Neo4jUtil.commonLabels){
                if(label.equals(commonLabel)){
                    getFeature = false;
                    break;
                }
            }
            if(getFeature){
                return label;
            }
        }
        return labels.toString();
    }

    public static <T> boolean contains(Iterable<T> iterable,T element){
        for (T tmp : iterable) {
            if (tmp.equals(element)) return true;
        }
        return false;
    }

    /**
     * 返回节点集合，此方法不保留关系
     *
     * @param cypherSql cypherSql
     */
    public static List<HashMap<String, Object>> getGraphNode(String cypherSql) {
        return getGraphNode(cypherSql,true);
    }


    public static <T,K> boolean addEntityNotDuplicate(Collection<T> collection,Entity element,Set<K> keysSet,Function<Entity,K> keyFunc,Function<Entity,T> transFunc){
        K key = keyFunc.apply(element);
        if(!keysSet.contains(key)){
            keysSet.add(key);
            collection.add(transFunc.apply(element));
            return true;
        }
        return false;
    }

    public static <T,K> boolean addNodeNotDuplicate(Collection<T> collection,Node element,Set<K> keysSet,Function<Entity,K> keyFunc,Function<Node,T> transFunc){
        K key = keyFunc.apply(element);
        if(!keysSet.contains(key)){
            keysSet.add(key);
            collection.add(transFunc.apply(element));
            return true;
        }
        return false;
    }

    public static <T> boolean addNodeNotDuplicate(Collection<T> collection,Node element,Set<Long> keysSet,Function<Node,T> transFunc){
        return addNodeNotDuplicate(collection,element,keysSet,Entity::id,transFunc);
    }

    public static boolean addNodeNotDuplicate(Collection<HashMap<String,Object>> collection,Node node,Set<String> keysSet){
//        return addEntityNotDuplicate(collection,node,keysSet,Neo4jUtil::getInternalId,o->nodeToMap((Node) o));//性能上不能代替
        String internalId = getInternalId(node);
        if(!keysSet.contains(internalId)){
            keysSet.add(internalId);
            collection.add(nodeToMap(node,internalId));
            return true;
        }
        return false;
    }

    public static List<HashMap<String, Object>> getGraphNode(String cypherSql,boolean matchNodeTypeOnly){//默认去重
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        Set<String> keysSet = new HashSet<String>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
//                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Value value : recordItem.values()) {
                        String typeName = value.type().name();
                        if (typeName.equals("NODE")) {
                            addNodeNotDuplicate(ents,value.asNode(),keysSet);
                        }else if(!matchNodeTypeOnly){
                            if ("PATH".equals(typeName)) {
                                Path path = value.asPath();
                                for (Node node : path.nodes()) {
                                    addNodeNotDuplicate(ents,node,keysSet);
                                }
                            } else if (typeName.contains("LIST")) {
                                Iterable<Value> vals=value.values();
                                for(Value val:vals){
                                    if(val.type().name().equals("NODE")){
                                        addNodeNotDuplicate(ents,val.asNode(),keysSet);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }

    //————————————————————————————————————————以下方法均没有经过验证——————————————————————————————————————————————————————

    /**
     * 获取数据库索引
     * @return
     */
    public static List<HashMap<String, Object>> getGraphIndex() {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            String cypherSql="call db.indexes";
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        if(key.equalsIgnoreCase("labelsOrTypes")){
                            String objects = value.asList().stream().map(n->n.toString()).collect(Collectors.joining(","));
                            rss.put(key, objects);
                        }else{
                            rss.put(key, value);
                        }
                    }
                    ents.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }
    public static List<HashMap<String, Object>> getGraphLabels() {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            String cypherSql="call db.labels";
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        if(key.equalsIgnoreCase("label")){
                            String objects =value.toString().replace("\"","");
                            rss.put(key, objects);
                        }else{
                            rss.put(key, value);
                        }
                    }
                    ents.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }
    /**
     * 删除索引
     * @param label
     */
    public static void deleteIndex(String label) {
        try (Session session = neo4jDriver.session()) {
            String cypherSql=String.format("DROP INDEX ON :`%s`(name)",label);
            session.run(cypherSql);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 创建索引
     * @param label
     * @param prop
     */
    public static void createIndex(String label,String prop) {
        try (Session session = neo4jDriver.session()) {
            String cypherSql=String.format("CREATE INDEX ON :`%s`(%s)",label,prop);
            session.run(cypherSql);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    public static HashMap<String, Object> getSingleGraphNode(String cypherSql) {
        List<HashMap<String, Object>> ent = getGraphNode(cypherSql);
        if (ent.size() > 0) {
            return ent.get(0);
        }
        return null;
    }

    /**
     * 获取一个标准的表格，一般用于语句里使用as
     *
     * @param cypherSql
     * @return
     */
    public static List<HashMap<String, Object>> getGraphTable(String cypherSql) {
        List<HashMap<String, Object>> resultData = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        rss.put(key, value);
                    }
                    resultData.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return resultData;
    }

    /**
     * 返回关系，不保留节点内容
     *
     * @param cypherSql
     * @return
     */
    public static List<HashMap<String, Object>> getGraphRelationShip(String cypherSql) {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Pair<String, Value> pair : f) {
                        HashMap<String, Object> rss = new HashMap<String, Object>();
                        String typeName = pair.value().type().name();
                        if (typeName.equals("RELATIONSHIP")) {
                            Relationship rship = pair.value().asRelationship();
                            String internalId = String.valueOf(rship.id());
                            String sourceId = String.valueOf(rship.startNodeId());
                            String targetId = String.valueOf(rship.endNodeId());
                            Map<String, Object> map = rship.asMap();
                            for (Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                rss.put(key, entry.getValue());
                            }
                            rss.put(Neo4jUtil.internalIdField, internalId);
                            rss.put("sourceId", sourceId);
                            rss.put("targetId", targetId);
                            ents.add(rss);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }


    /**
     * 获取值类型的结果,如count,internalId
     *
     * @return 1 2 3 等数字类型
     */
    public static long getGraphValue(String cypherSql) {
        long val = 0;
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result cypherResult = session.run(cypherSql);
            if (cypherResult.hasNext()) {
                Record record = cypherResult.next();
                for (Value value : record.values()) {
                    val = value.asLong();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return val;
    }

    /**
     * 返回节点和关系，节点node,关系relationship,路径path,集合list,map
     *
     * @param cypherSql
     * @return
     */
    public static HashMap<String, Object> getGraphNodeAndShip(String cypherSql) {
        HashMap<String, Object> mo = new HashMap<String, Object>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
                List<HashMap<String, Object>> ships = new ArrayList<HashMap<String, Object>>();
                List<String> internalIds = new ArrayList<String>();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Pair<String, Value> pair : f) {
                        HashMap<String, Object> rShips = new HashMap<String, Object>();
                        HashMap<String, Object> rss = new HashMap<String, Object>();
                        String typeName = pair.value().type().name();
                        if ("NULL".equals(typeName)) {
                            continue;
                        }
                        if ("NODE".equals(typeName)) {
                            Node noe4jNode = pair.value().asNode();
                            Map<String, Object> map = noe4jNode.asMap();
                            String internalId = String.valueOf(noe4jNode.id());
                            if (!internalIds.contains(internalId)) {
                                for (Entry<String, Object> entry : map.entrySet()) {
                                    String key = entry.getKey();
                                    rss.put(key, entry.getValue());
                                }
                                rss.put(Neo4jUtil.internalIdField, internalId);
                                internalIds.add(internalId);
                            }
                            if (!rss.isEmpty()) {
                                ents.add(rss);
                            }
                        } else if ("RELATIONSHIP".equals(typeName)) {
                            Relationship rship = pair.value().asRelationship();
                            String internalId = String.valueOf(rship.id());
                            String sourceId = String.valueOf(rship.startNodeId());
                            String targetId = String.valueOf(rship.endNodeId());
                            Map<String, Object> map = rship.asMap();
                            for (Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                rShips.put(key, entry.getValue());
                            }
                            rShips.put(Neo4jUtil.internalIdField, internalId);
                            rShips.put("sourceId", sourceId);
                            rShips.put("targetId", targetId);
                            ships.add(rShips);
                        } else if ("PATH".equals(typeName)) {
                            Path path = pair.value().asPath();
                            for (Node nodeItem : path.nodes()) {
                                Map<String, Object> map = nodeItem.asMap();
                                String internalId = String.valueOf(nodeItem.id());
                                rss = new HashMap<String, Object>();
                                if (!internalIds.contains(internalId)) {
                                    for (Entry<String, Object> entry : map.entrySet()) {
                                        String key = entry.getKey();
                                        rss.put(key, entry.getValue());
                                    }
                                    rss.put(Neo4jUtil.internalIdField, internalId);
                                    internalIds.add(internalId);
                                }
                                if (!rss.isEmpty()) {
                                    ents.add(rss);
                                }
                            }
                            for (Relationship next : path.relationships()) {
                                rShips = new HashMap<String, Object>();
                                String internalId = String.valueOf(next.id());
                                String sourceId = String.valueOf(next.startNodeId());
                                String targetId = String.valueOf(next.endNodeId());
                                Map<String, Object> map = next.asMap();
                                for (Entry<String, Object> entry : map.entrySet()) {
                                    String key = entry.getKey();
                                    rShips.put(key, entry.getValue());
                                }
                                rShips.put(Neo4jUtil.internalIdField, internalId);
                                rShips.put("sourceId", sourceId);
                                rShips.put("targetId", targetId);
                                ships.add(rShips);
                            }
                        } else if (typeName.contains("LIST")) {
                            Iterable<Value> val = pair.value().values();
                            Value next = val.iterator().next();
                            String type = next.type().name();
                            if ("RELATIONSHIP".equals(type)) {
                                Relationship rship = next.asRelationship();
                                String internalId = String.valueOf(rship.id());
                                String sourceId = String.valueOf(rship.startNodeId());
                                String targetId = String.valueOf(rship.endNodeId());
                                Map<String, Object> map = rship.asMap();
                                for (Entry<String, Object> entry : map.entrySet()) {
                                    String key = entry.getKey();
                                    rShips.put(key, entry.getValue());
                                }
                                rShips.put(Neo4jUtil.internalIdField, internalId);
                                rShips.put("sourceId", sourceId);
                                rShips.put("targetId", targetId);
                                ships.add(rShips);
                            }
                        } else if (typeName.contains("MAP")) {
                            rss.put(pair.key(), pair.value().asMap());
                        } else {
                            rss.put(pair.key(), pair.value().toString());
                            ents.add(rss);
                        }
                    }
                }
                mo.put("node", ents);
                mo.put("relationship", toDistinctList(ships));
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return mo;
    }


    /**
     * 去掉json键的引号，否则neo4j会报错
     *
     * @param jsonStr
     * @return
     */
    public static String getFilterPropertiesJson(String jsonStr) {
        return jsonStr.replaceAll("\"(\\w+)\"(\\s*:\\s*)", "$1$2"); // 去掉key的引号
    }

    /**
     * 对象转json，key=value,用于 cypher set语句
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String getKeyValCyphersql(T obj) {
        Map<String, Object> map = new HashMap<String, Object>();
        List<String> sqlList = new ArrayList<String>();
        // 得到类对象
        Class userCla = obj.getClass();
        /* 得到类中的所有属性集合 */
        Field[] fs = userCla.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            Class type = f.getType();

            f.setAccessible(true); // 设置些属性是可以访问的
            Object val = new Object();
            try {
                val = f.get(obj);
                if (val == null) {
                    val = "";
                }
                String sql = "";
                String key = f.getName();
                if (val instanceof String[]) {
                    //如果为true则强转成String数组
                    String[] arr = (String[]) val;
                    String v = "";
                    for (int j = 0; j < arr.length; j++) {
                        arr[j] = "'" + arr[j] + "'";
                    }
                    v = String.join(",", arr);
                    sql = "n." + key + "=[" + val + "]";
                } else if (val instanceof List) {
                    //如果为true则强转成String数组
                    List<String> arr = (ArrayList<String>) val;
                    List<String> aa = new ArrayList<String>();
                    String v = "";
                    for (String s : arr) {
                        s = "'" + s + "'";
                        aa.add(s);
                    }
                    v = String.join(",", aa);
                    sql = "n." + key + "=[" + v + "]";
                } else {
                    // 得到此属性的值
                    map.put(key, val);// 设置键值
                    if (type.getName().equals("int")) {
                        sql = "n." + key + "=" + val + "";
                    } else {
                        sql = "n." + key + "='" + val + "'";
                    }
                }

                sqlList.add(sql);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
        return String.join(",", sqlList);
    }

    /**
     * 将haspmap集合反序列化成对象集合
     *
     * @param maps
     * @param type
     * @param <T>
     * @return
     */
    public static <T> List<T> hashMapToObject(List<HashMap<String, Object>> maps, Class<T> type) {
        try {
            List<T> list = new ArrayList<T>();
            for (HashMap<String, Object> r : maps) {
                T t = type.newInstance();
                Iterator iter = r.entrySet().iterator();// 该方法获取列名.获取一系列字段名称.例如name,age...
                while (iter.hasNext()) {
                    Entry entry = (Entry) iter.next();// 把hashmap转成Iterator再迭代到entry
                    String key = entry.getKey().toString(); // 从iterator遍历获取key
                    Object value = entry.getValue(); // 从hashmap遍历获取value
                    if ("serialVersionUID".toLowerCase().equals(key.toLowerCase())) {
                        continue;
                    }
                    Field field = type.getDeclaredField(key);// 获取field对象
                    if (field != null) {
                        //System.out.print(field.getType());
                        field.setAccessible(true);
                        //System.out.print(field.getType().getName());
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0);// 设置值
                            } else {
                                field.set(t, Integer.parseInt(value.toString()));// 设置值
                            }
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0);// 设置值
                            } else {
                                field.set(t, Long.parseLong(value.toString()));// 设置值
                            }

                        } else if (field.getType() == Double.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0.0);// 设置值
                            } else {
                                field.set(t, Double.parseDouble(value.toString()));// 设置值
                            }

                        } else {
                            if (field.getType().equals(List.class)) {
                                if (value == null || StringUtil.isBlank(value.toString())) {
                                    field.set(t, null);
                                } else {
                                    field.set(t, value);// 设置值
                                }
                            } else {
                                field.set(t, value);// 设置值
                            }
                        }
                    }

                }
                list.add(t);
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将haspmap反序列化成对象
     *
     * @param map
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T hashMapToObjectItem(HashMap<String, Object> map, Class<T> type) {
        try {
            T t = type.newInstance();
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();// 把hashmap转成Iterator再迭代到entry
                String key = entry.getKey().toString(); // 从iterator遍历获取key
                Object value = entry.getValue(); // 从hashmap遍历获取value
                if ("serialVersionUID".toLowerCase().equals(key.toLowerCase())) {
                    continue;
                }
                Field field = type.getDeclaredField(key);// 获取field对象
                if (field != null) {
                    field.setAccessible(true);
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0);// 设置值
                        } else {
                            field.set(t, Integer.parseInt(value.toString()));// 设置值
                        }
                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0);// 设置值
                        } else {
                            field.set(t, Long.parseLong(value.toString()));// 设置值
                        }

                    } else if (field.getType() == Double.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0.0);// 设置值
                        } else {
                            field.set(t, Double.parseDouble(value.toString()));// 设置值
                        }

                    } else {
                        if (field.getType().equals(List.class)) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, null);
                            } else {
                                field.set(t, value);// 设置值
                            }
                        } else {
                            field.set(t, value);// 设置值
                        }

                    }
                }

            }

            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回单个节点信息
     */
    public static HashMap<String, Object> getOneNode(String cypherSql) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                Record record = result.list().get(0);
                Pair<String, Value> f = record.fields().get(0);
                String typeName = f.value().type().name();
                if ("NODE".equals(typeName)) {
                    Node noe4jNode = f.value().asNode();
                    String internalId = String.valueOf(noe4jNode.id());
                    Map<String, Object> map = noe4jNode.asMap();
                    for (Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        ret.put(key, entry.getValue());
                    }
                    ret.put(Neo4jUtil.internalIdField, internalId);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ret;
    }




    public static List<HashMap<String, Object>> toDistinctList(List<HashMap<String, Object>> list) {
        Set<String> keysSet = new HashSet<String>();
        Iterator<HashMap<String, Object>> it = list.iterator();
        while (it.hasNext()) {
            HashMap<String, Object> map = it.next();
            String internalId = (String) map.get(Neo4jUtil.internalIdField);
            int beforeSize = keysSet.size();
            keysSet.add(internalId);
            int afterSize = keysSet.size();
            if (afterSize != (beforeSize + 1)) {
                it.remove();
            }
        }
        return list;
    }
}
