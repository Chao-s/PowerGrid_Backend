package com.gridgraphprocessing.algo.model.nariGraph;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;

@Node("CONDUCTIVEEQUIPMENT")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
//@AllArgsConstructor
@NoArgsConstructor
public class Device extends BaseNode implements ConductiveEquipment{
    //实现接口是为了格式上的规范，从而用接口表示标签；而不添加@Node就无法获得接口中的标签，故维持@Node中的标签与接口始终相一致
    //此类对象的建立是为了用相同的父类操作一系列不同的具有某种共同特征的子类，因为BaseNode无法既标记@Node又不含标签，所以需要此类对象
    //若能让图数据库中所有节点具有一个共同基础标签，则此类对象不再必要

    public Device(Long id){
        this.setId(id);
    }
}
