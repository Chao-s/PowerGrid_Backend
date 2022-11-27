package com.gridgraphprocessing.algo.model.nariGraph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseNode {
    @Id //@GeneratedValue
    private Long id;//开关ID号 （一般作为：约束/索引
}
