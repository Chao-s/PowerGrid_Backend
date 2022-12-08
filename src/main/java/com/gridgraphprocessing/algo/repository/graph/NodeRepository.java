package com.gridgraphprocessing.algo.repository.graph;

import com.gridgraphprocessing.algo.model.nariGraph.BaseNode;
import com.gridgraphprocessing.algo.model.nariGraph.Device;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NodeRepository extends Neo4jRepository<BaseNode, Long> {

    //__默认实现通用的save/queryByExample（Example可以实现对象属性的匹配）等方法__

    //__以下crud方法仅作为自定义查询语句书写格式的举例(cypher随便写的，搜不到会报错，不做参考)，实际上不需要，用上面的默认实现方法或neo4jTemplate均可以代替__
//    @Query("MATCH (n:DMS_CB_DEVICE {id: $id}) RETURN n")//同下
//    DmsCbDevice findDmsCbDeviceById(Long id);
//    @Query("MATCH (n:CONDUCTIVEEQUIPMENT {id: $id}) RETURN n")
//    Device findCONDUCTIVEEQUIPMENTById(Long id);//没有标准映射类的节点会映射到相容的类上；可用默认实现的findOne(Example.of(new Device()))代替
//    @Query("MATCH (n:CONDUCTIVEEQUIPMENT) RETURN n")
//    List<Device> findAllDevices();//获取节点列表并映射到各子类上，遇到无映射子类的节点会报错；可用默认实现的findAll(Example.of(new Device()))代替

    //__特殊的自定义Cypher__//toDO 此处搜索cypher为旧版本
    @Query("MATCH (targetCB:DMS_CB_DEVICE{id: $id}),\n" +
            "      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
            "WITH targetCB, collect(busBarSections) AS endNodes\n" +
            "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'CONNECT_WITH', limit : 2})\n" +
            "YIELD path\n" +
            "WITH nodes(path) AS pathNodes, targetCB\n" +
            "match (cbEnd: DMS_CB_DEVICE) where cbEnd in pathNodes\n" +
            "call apoc.path.spanningTree(targetCB, {whiteListNodes: pathNodes, relationshipFilter: 'CONNECT_WITH', terminatorNodes: cbEnd,  limit : 1})\n" +
            "yield path as res\n" +
            "with nodes(res) as cbs, targetCB\n" +
            "MATCH (n:DMS_CB_DEVICE)  where n in cbs and n <> targetCB\n" +
            "return n;")
    Set<Device> searchUpstream(Long id);//此处必须返回Set，用List返回结果重复了

    //__END__
}
