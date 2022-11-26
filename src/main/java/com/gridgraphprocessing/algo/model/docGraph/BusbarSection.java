package com.gridgraphprocessing.algo.model.docGraph;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Property;

//@Node("BUSBARSECTION")//发电机组表411
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusbarSection implements DocDevice {
    @Id
    private String id;//设备文件中要求类型为String（约束/索引

    @Property
    private String name;//名称

    @Property
    private String st_id;//所属厂站ID（索引

    @Property
    private String bay_id;//间隔ID

    @Property
    private String bv_id;//电压类型ID（索引

    @Property
    private Long nd;//节点号

    @Property
    private Integer bs_type;//母线类型

    @Property
    private Integer tpcolor;//拓扑着色

    @Property
    private Float v;//线电压

    @Property
    private Integer v_qual;//线电压质量码
}
