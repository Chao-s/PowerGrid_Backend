package com.gridgraphprocessing.algo.model.docGraph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * @description 配网开关表13502
 */
//@Node("DMS_CB_DEVICE")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocDmsCbDevice implements DocDevice {
    @Id //@GeneratedValue(UUIDStringGenerator.class)
    private String id;//开关ID号（约束/索引

    private String name;//开关名称

    @Property("feeder_id")
    private String feederId;//所属馈线ID（索引

    @Property("combined_id")
    private String combinedId;//所属开关站ID

    @Property("bv_id")
    private String bvId;//电压类型ID（索引

    private Long ind;//节点1号

    private Long jnd;//节点2号

    private Integer tpcolor;//拓扑着色

    private Integer point;//遥信值
    private Integer status;//遥信质量码
    private Float p;//有功值
    private Float q;//无功值

    @Property("i_a_value")
    private Float iAValue;//A相电流幅值
    @Property("i_b_value")
    private Float iBValue;//B相电流幅值
    @Property("i_c_value")
    private Float iCValue;//C相电流幅值

    //新增自动化开关属性！！
    @Property("is_auto")
    private Integer isAuto;//是否为自动化开关
}
