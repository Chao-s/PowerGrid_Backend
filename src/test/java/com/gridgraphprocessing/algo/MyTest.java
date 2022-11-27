package com.gridgraphprocessing.algo;

import com.gridgraphprocessing.algo.model.AutoDevice;
import com.gridgraphprocessing.algo.model.nariGraph.Device;
import com.gridgraphprocessing.algo.model.nariGraph.DmsBsDevice;
import com.gridgraphprocessing.algo.model.nariGraph.DmsCbDevice;
import com.gridgraphprocessing.algo.repository.AutoDeviceRepository;
import com.gridgraphprocessing.algo.util.Neo4jUtil;
import com.gridgraphprocessing.algo.model.nariGraph.*;

import com.gridgraphprocessing.algo.repository.graph.NodeRepository;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;

import java.util.*;

import static org.testcontainers.shaded.org.apache.commons.lang3.math.NumberUtils.min;


@SpringBootTest
public class MyTest {//注意，此处没有事务回滚，请在测试环境中跑以免污染数据

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    AutoDeviceRepository autoDeviceRepository;


    <T>void printCollection(Collection<T>  collection){
        System.out.println(collection.size()+" in all");
        int i=1,max=10;
        for(T device:collection){
            System.out.println(device);
            if(i>=max){
                break;
            }
            i+=1;
        }
    }

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

        System.out.println(autoDeviceRepository.existsById("3801319560578176491"));
    }

    @Test//添加自动化开关
    void addAutoPropTest(@Autowired Neo4jTemplate neo4jTemplate) throws Exception {
//        List<DmsCbDevice> devices = neo4jTemplate.findAll(DmsCbDevice.class);
        List<DmsCbDevice> devices = nodeRepository.findAll(Example.of(new DmsCbDevice()));
        int num = devices.size();
        int i=1;
        System.out.println(num+" in all");
        for (DmsCbDevice cb:devices) {
            Optional<AutoDevice> autoDevice = autoDeviceRepository.findById(cb.getId().toString());//此处不需要取全表，可以优化
            if(autoDevice.isEmpty()||
                    autoDevice.get().getIsAuto()<0||
                    autoDevice.get().getIsAuto()>1){
                cb.setIsAuto(-1L);
                System.out.println("not hit !");
            }else if(autoDevice.get().getIsAuto()==0){
                cb.setIsAuto(0L);
            }else{
                cb.setIsAuto(1L);
            }
            System.out.printf("%s/%s; %s:%s%n",i,num,cb.getId(),cb.getIsAuto());

            Map<String,Object> parameters = new HashMap<>();
            parameters.put("id",cb.getId());
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
        String cypherSqlFmt1 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s})," +
                "(busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamPathNodes, targetCB\n" +
                "call apoc.path.subgraphAll(targetCB, {whitelistNodes: upstreamPathNodes, labelFilter: '/DMS_CB_DEVICE'})\n" +
                "yield nodes\n" +
                "return nodes;";

        String cypherSqlFmt2 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s})," +
                "(busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'CONNECT_WITH', limit : 2})\n" +
                "YIELD path\n" +
                "WITH nodes(path) AS pathNodes, targetCB\n" +
                "match (cbEnd: DMS_CB_DEVICE) where cbEnd in pathNodes\n" +
                "call apoc.path.spanningTree(targetCB, {whiteListNodes: pathNodes, relationshipFilter: 'CONNECT_WITH', terminatorNodes: cbEnd,  limit : 1})\n" +
                "yield path as res\n" +
                "with nodes(res) as cbs, targetCB\n" +
                "MATCH (n:DMS_CB_DEVICE)  where n in cbs and n <> targetCB\n" +
                "return n;";
        String cypherSql = String.format(cypherSqlFmt2, "3800475135564560470");
        List<HashMap<String, Object>> graphNodeList = Neo4jUtil.getGraphNode(cypherSql);
        System.out.println(cypherSql);
        System.out.println("___");
        System.out.println(graphNodeList.size()+" in all as in List, Set shown as below:");
        Set<HashMap<String, Object>> nodeSet = new HashSet<>(graphNodeList);
        printCollection(nodeSet);
    }

    @Test
    void downstreamSearchByDriverTest() throws Exception {
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
        List<HashMap<String, Object>> graphNodeList = Neo4jUtil.getGraphNode(cypherSql);
        List<Record> records = Neo4jUtil.getRareResult(cypherSql);
        System.out.println(cypherSql);
        System.out.println("___");
        printCollection(records);
        System.out.println("___");
        System.out.println(graphNodeList.size()+" in all as in List, Set shown as below:");
        Set<HashMap<String, Object>> nodeSet = new HashSet<>(graphNodeList);
        printCollection(nodeSet);
        System.out.println(Neo4jUtil.isNeo4jOpen());

    }



}
