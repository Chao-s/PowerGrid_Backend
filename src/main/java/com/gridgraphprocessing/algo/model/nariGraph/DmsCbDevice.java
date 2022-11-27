package com.gridgraphprocessing.algo.model.nariGraph;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * @description 配网开关表13502
 */
@Node("DMS_CB_DEVICE")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class DmsCbDevice extends Device implements DMS{//也可让Device实现DMS，这样只用继承Device即可，一切看业务需求
    //dyncComponentId等可能要考虑使用动态properties
    //图库中DmsCbDevice大部分为此类型节点，存在小部分按文档构造的节点（无DMS标签），考虑以后为此类添加上文档特有的属性以实现节点的完备性

//    @Id //@GeneratedValue
//    private Long id;//开关ID号（约束/索引

    private String name;//开关名称

    @Property("feeder_id")
    private Long feederId;//所属馈线ID

    @Property("combined_id")
    private Long combinedId;//所属开关站ID

    private Long ind;//节点1号

    private Long jnd;//节点2号

    @Property("brk_connect_type")
    private Long brkConnectType;
    @Property("component_id")
    private Long componentId;
    @Property("dms_component_id")
    private Long dmsComponentId;
    @Property("dms_dync_component_id")
    private Long dmsDyncComponentId;
    @Property("dms_land_id")
    private Long dmsLandId;
    @Property("dync_component_id")
    private Long dyncComponentId;

    private Long mapId;
    private Long point;
    private String psrid;
    @Property("region_id")
    private Long regionId;
    private Long status;

    //新增自动化开关属性！！
    @Property("is_auto")
    private Long isAuto;//是否为自动化开关
}
