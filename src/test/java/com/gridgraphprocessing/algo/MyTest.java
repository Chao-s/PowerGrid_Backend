package com.gridgraphprocessing.algo;

import com.gridgraphprocessing.algo.model.nariGraph.Device;
import com.gridgraphprocessing.algo.model.nariGraph.DmsBsDevice;
import com.gridgraphprocessing.algo.model.nariGraph.DmsCbDevice;
import com.gridgraphprocessing.algo.repository.AutoDeviceRepository;
import com.gridgraphprocessing.algo.service.graph.algo.GraphSearchAlgorithm;
import com.gridgraphprocessing.algo.util.Neo4jUtil;

import com.gridgraphprocessing.algo.repository.graph.NodeRepository;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;

import java.util.*;

import static com.gridgraphprocessing.algo.util.PrintUtil.printCollection;
import static com.gridgraphprocessing.algo.util.PrintUtil.printListTreeWithMap;
import static org.testcontainers.shaded.org.apache.commons.lang3.math.NumberUtils.min;


@SpringBootTest
public class MyTest {//注意，此处没有事务回滚，如果有write操作请在测试环境中跑以免污染数据；正式的测试需另写

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    AutoDeviceRepository autoDeviceRepository;

    @Autowired
    GraphSearchAlgorithm graphSearchAlgorithm;




    @Test
    void graphSaveTest() throws Exception {
        DmsBsDevice device = new DmsBsDevice();
        device.setId(332L);
        device.setDmsLandId(25L);
        DmsBsDevice returnDevice = nodeRepository.save(device);
        System.out.println(returnDevice);
    }

    @Test
    void graphFindOneTest(@Autowired Neo4jTemplate neo4jTemplate) throws Exception {
        //以下三个方法均可，没有标准映射类的节点会映射到相容的类上，搜不到则Optional方法会返回empty结果
        Optional<Device> device = nodeRepository.findOne(Example.of(new Device(115404742196200620L)));
//        Device device = nodeRepository.findCONDUCTIVEEQUIPMENTById(392L);
//        Optional<Device> device = neo4jTemplate.findById(115404742196200620L,Device.class);
        System.out.println(device);
    }

    @Test
    void graphFindAllTest(@Autowired Neo4jTemplate neo4jTemplate) throws Exception {
        //在构建好图库中所有Device对应实体类前先用这个
        List<Device> devices = nodeRepository.findAll(Example.of(new DmsCbDevice()));

        //以下三个方法可以获取各子类，但遇到不能映射的子类会报错
//        List<Device> devices = nodeRepository.findAll(Example.of(new Device()));
//        List<Device> devices = nodeRepository.findAllDevices();
//        List<Device> devices = neo4jTemplate.findAll(Device.class);
        printCollection(devices);
    }

    @Test
    void sqlGetTest() throws Exception {
//        Optional<AutoDevice> autoDevice = autoDeviceRepository.findById("3801319560578176491");
//        System.out.println(autoDevice.toString());

//        System.out.println(autoDeviceRepository.existsById("3801319560578176491"));
//        System.out.println(autoDeviceRepository.existsByIsAutoAndId(1,"3801319560578176491"));
        System.out.println(autoDeviceRepository.findIsAutoById("3801319560578176491"));
    }

    @Test//添加自动化开关
    void addAutoPropTest(@Autowired Neo4jTemplate neo4jTemplate) throws Exception {
//        List<DmsCbDevice> devices = neo4jTemplate.findAll(DmsCbDevice.class);
        List<DmsCbDevice> devices = nodeRepository.findAll(Example.of(new DmsCbDevice()));
        int num = devices.size();
        int i=1;
        System.out.println(num+" in all");
        for (DmsCbDevice cb:devices) {
            Integer isAuto = autoDeviceRepository.findIsAutoById(cb.getId().toString());
            if(isAuto==null){
                cb.setIsAuto(-1L);
                System.out.println("not hit !");
            }else if(isAuto==0){
                cb.setIsAuto(0L);
            }else if(isAuto==1){
                cb.setIsAuto(1L);
            }else{
                throw new Exception("is_auto existed is neither 0 nor 1 !!!");
            }
            System.out.printf("%s/%s; %s:%s%n",i,num,cb.getId(),cb.getIsAuto());

            //toDO 待总结与优化
            Map<String,Object> parameters = new HashMap<>();
            parameters.put(Neo4jUtil.getRealIdField(),cb.getId());
            parameters.put("is_auto",cb.getIsAuto());
            QueryFragmentsAndParameters queryFragmentsAndParameters =
                    new QueryFragmentsAndParameters(
                            "MATCH (n:DMS_CB_DEVICE{id: $id}) SET n.is_auto = $is_auto",
                            parameters);
            neo4jTemplate.toExecutableQuery(
                    DmsCbDevice.class,
                    queryFragmentsAndParameters);
            //save()方法不知道为何报警告，将视作新节点而不能以此方式merge更新属性，故改用neo4jTemplate执行cypher
            //不知是否与repository是Autowired还是final有关，之前一定情况下save()可以完成merge更新的
            i+=1;
        }
    }

    @Test
    void upstreamSearchBySDNTest() throws Exception {
        Set<Device> devices = nodeRepository.searchUpstream(3800475135564560470L);
        System.out.println("___");
        printCollection(devices);
    }

    @Test
    void upstreamSearchByDriverTest() throws Exception {
        graphSearchAlgorithm.searchUpstreamReturnPaths("3800475135564560470");
    }

    @Test
    void downstreamSearchByDriverTest() throws Exception {
        graphSearchAlgorithm.searchDownstreamReturnPaths("3800475135564528897");//3800475135564528897
    }

    @Test
    void downstreamSearchByCostomFunctionTest() throws Exception {
        String cypherSqlFmt1 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
                "      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamNodes, targetCB\n" +
                "match (targetCB),\n" +
                "      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)\n" +
                "  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id\n" +
                "  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id\n" +
                "with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes\n" +
                "call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,\n" +
                "                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})\n" +
                "yield path\n" +
                "return path;";

        String cypherSqlFmt2 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
                "      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamNodes, targetCB\n" +
                "match (targetCB),\n" +
                "      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)\n" +
                "  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id\n" +
                "  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id\n" +
                "with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes\n" +
                "call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,\n" +
                "                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})\n" +
                "yield path\n" +
                "with nodes(path) as downStreamNodes, targetCB, endNodes\n" +
                "call apoc.path.subgraphAll(targetCB, {whitelistNodes: downStreamNodes,\n" +
                "                                       relationshipFilter: 'CONNECT_WITH', endNodes: endNodes})\n" +
                "YIELD nodes\n" +
                "return nodes;";
        String cypherSql = String.format(cypherSqlFmt2, "3800475135564528897");
        List<Record> recordList = Neo4jUtil.getRawRecords(cypherSql);
        assert recordList != null;
        Set<Record> records = new HashSet<>(recordList);
        System.out.println(cypherSql);
        System.out.println("___");
        printCollection(records,(record -> {
            Set<Map<String, Object>> ents = new HashSet<Map<String, Object>>();
            List<Pair<String, Value>> f = record.fields();
//            System.out.println(f.size());
            for (Pair<String, Value> pair : f) {
                HashMap<String, Object> rss = new HashMap<String, Object>();
                String typeName = pair.value().type().name();
//                System.out.println(typeName);
                List<Value> values = pair.value().asList(o->o);
                for (Value value:values){
                    if (value.type().name().equals("NODE")) {
                        ents.add(Neo4jUtil.nodeToMap(value.asNode()));
                    }
                }
            }
            return ents;
        }));
    }

    @Test
    void getNodeTreeTest() throws Exception{//当cypher没有limit:2时返回路径非常多且有重复，故需要新的数据模型与搜索算法，此测试为路径的树结构建立与打印的测试
        List<Path> paths = graphSearchAlgorithm.searchUpstreamReturnPaths("3800475135564528897");//"3800475135564528897" "3800475135564560470"
        Map<Integer,LinkedList<Integer>> map = new HashMap<>();
        List<Path> partOfPaths = new LinkedList<>(paths).subList(0, Math.min(paths.size(), 100));
        List<HashMap<String, Object>> nodeTree = Neo4jUtil.getNodeTree(partOfPaths,map);
        printListTreeWithMap(nodeTree,map, t->Neo4jUtil.getNodePriLabel(Neo4jUtil.getDeviceLabels(t)));

    }

    //    在此处测试上下游搜索算法
    @Test
    void  searchAlgorithm() throws Exception {
        String faultId = "3800475135564528897";//"3800475135564528897" "3800475135564560470"
        List<List<Node>> faultNodePaths = graphSearchAlgorithm.searchFaultUpstream(faultId);
        //下游搜索
        System.out.println();
        System.out.println("__下游情况如下__");
        List<List<Node>> UserEquipNodePaths = graphSearchAlgorithm.searchAffectedEquipDownstream(faultId);
    }
}
