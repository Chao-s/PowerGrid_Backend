package com.gridgraphprocessing.algo.repository.graph;

import com.gridgraphprocessing.algo.model.nariGraph.BaseNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

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

    //__特殊的自定义Cypher__
    //toDo

    //__END__
}
