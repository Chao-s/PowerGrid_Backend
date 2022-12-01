package com.gridgraphprocessing.algo.service.graph.impl;

import com.gridgraphprocessing.algo.repository.graph.NodeRepository;
import com.gridgraphprocessing.algo.service.graph.SDNGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor//代替@Autowired
public class SDNGraphServiceImpl implements SDNGraphService {

    private final Neo4jTemplate neo4jTemplate;

    private final NodeRepository nodeRepository;

    //理论上可以用neo4jTemplate/nodeRepository实现通用的crud（但要提供映射的实体类），包括但不限于findById/save/...
    //neo4jTemplate也可传入cypher语句执行，总的来说neo4jTemplate全能

}
