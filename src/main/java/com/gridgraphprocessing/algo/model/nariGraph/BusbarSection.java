package com.gridgraphprocessing.algo.model.nariGraph;


import com.gridgraphprocessing.algo.model.docGraph.DocDevice;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * @description 母线表410
 */
@Node("BUSBARSECTION")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class BusbarSection extends Device implements EMS {
    //dyncComponentId等可能要考虑使用动态properties
    //图库中的BusbarSection大部分为此类型节点，存在小部分按文档构造的节点（无EMS标签），考虑以后为此类添加上文档特有的属性以实现节点的完备性

//    @Id //@GeneratedValue
//    private Long id;//开关ID号（约束/索引

    private String name;//名称

    @Property("st_id")
    private Long stId;//所属厂站ID（索引

    @Property("bv_id")
    private Long bvId;//电压类型ID（索引

    private Long nd;//节点号

    @Property("bs_type")
    private Long bsType;//母线类型

    private Double v;//线电压

    @Property("v_qual")
    private Long vQual;//线电压质量码

    @Property("component_id")
    private Long componentId;

    @Property("dync_component_id")
    private Long dyncComponentId;

    private Long point;

}
