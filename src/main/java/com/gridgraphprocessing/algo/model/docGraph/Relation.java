package com.gridgraphprocessing.algo.model.docGraph;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.List;
import java.util.Map;

/**
 * 定义一个关系节点
 */
@RelationshipProperties
public class Relation {//仅提供关系类创建的参考

    @RelationshipId
    private Long id;

    private final List<Map<String,Object>> properties;

    @TargetNode
    private final DocDevice device;

    public Relation(DocDevice device, List<Map<String,Object>> properties) {
        this.device = device;
        this.properties = properties;
    }

    public List<Map<String,Object>> getProperties() {
        return properties;
    }
}
