package com.gridgraphprocessing.algo.model.docGraph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 配网母线表13506
 */
//@Node("DMS_BS_DEVICE")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocDmsBsDevice implements DocDevice {
    @Id //@GeneratedValue(UUIDStringGenerator.class)
    private String id;//开关ID号（约束/索引

    private String name;//开关名称

    @Property("feeder_id")
    private String feederId;//所属馈线ID（索引

    @Property("combined_id")
    private String combinedId;//所属开关站ID

    @Property("bv_id")
    private String bvId;//电压类型ID（索引

    private Long nd;//节点号

    private Integer tpcolor;//拓扑着色

    private Float v;//线电压幅值

    // 定义一个关系，注意directiond代表箭头方向，INCOMING箭头指向自己，OUTGOING箭头指向TargetNode
    @Relationship(type = "CONNECT_WITH", direction = Relationship.Direction.OUTGOING)
    private List<Relation> relations = new ArrayList<>();

}
