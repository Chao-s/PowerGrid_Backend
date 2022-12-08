package com.gridgraphprocessing.construct.service.construct.impl;

import com.gridgraphprocessing.algo.util.Neo4jUtil;
import com.gridgraphprocessing.construct.enums.DeviceType;
import com.gridgraphprocessing.construct.service.construct.ConstructService;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.plaf.IconUIResource;
import java.util.List;

/**
 * @author Yunthin.Chow
 * @date 2022/12/2 15:57
 * @description 图数据库构建服务接口实现类
 */
@Service
public class ConstructServiceImpl implements ConstructService {

    @Autowired
    Neo4jUtil neo4jUtil;

    /**
     * 调整上游的 BREAKER 、DISCONNECTOR、GROUND_DISCONNECTOR 的连接关系
     */
    @Override
    public void adjustUpstreamBreaker() {

        // 找到所有和 BREAKER 有连接关系的 DISCONNECTOR
        List<Record> disonnectorRawRecords = Neo4jUtil.getRawRecords("MATCH (n:DISCONNECTOR) return n;");
        int moreThanOnecount = 0;
        int matchCount = 0;
        for (Record disconnectorRaw : disonnectorRawRecords) {
            Node disconnectorNode = disconnectorRaw.get(0).asNode();
            // 如果这个 DISCONNECTOR 是压变，那么删除所有与 BREAKER 的联系
            if (disconnectorNode.get("name").asString().contains("压变")) {
                continue;
//                Neo4jUtil.runCypherSql(String.format(
//                        "MATCH (n:DISCONNECTOR {cusId: ''})-[r:CONNECT_WITH]->(m:BREAKER) delete r;", disconnectorNode.get("cusId")
//                ));
            }
            // 对于每个 DISCONNECTOR
            String cusId = disconnectorNode.get("cusId").asString();
            // 找它的邻居
            List<Record> neighborRawRecords = Neo4jUtil.getRawRecords(
                    String.format("MATCH (n:DISCONNECTOR {cusId: '%s'}) CALL apoc.neighbors.athop(n, 'CONNECT_WITH', 1) YIELD node RETURN node;",
                            cusId));
            // 目标 BREAKER
            Node targetBreakerNode = null;
            int breakerCount = 0;
            // 对于每个 DISCONNECTOR 的邻居
            for (Record neighborRecord : neighborRawRecords) {
                Node neighborNode = neighborRecord.get(0).asNode();
                if (neighborNode.hasLabel(DeviceType.BREAKER.toString())) {
                    // 如果有 BREAKER，把BREAKER进行匹配
                    ++breakerCount;
                    if (matchDisconnectorandBreaker(neighborNode, disconnectorNode)) {
                        targetBreakerNode = neighborNode;
                        ++matchCount;
                    }
                }
            }
            if (breakerCount > 1) {
                ++moreThanOnecount;
                if (targetBreakerNode == null) {
                    System.out.println();
                    System.out.println();
                    System.out.println(disconnectorNode.get("cusId").asString());
                }
            }
        }
        System.out.println("moreThanOnecount" + moreThanOnecount);
        System.out.println("matchCount                " + matchCount);

        // 对于每个找到的 DISCONNECTOR，找到与它相连的 BREAKER

        // 按照名称进行匹配，只保留匹配成功的 BREAKER - DISCONNECTOR 连接关系，其他的都删掉


        // 第二部分
        // 将 DISCONNECTOR 的连接关系转接到 BREAKER 上
        // 将 GROUND_DISCONNECTOR 的连接关系转接到 BREAKER 上
    }

    /**
     * 将 DISCONNECTOR 和 BREAKER 按照 . 号后面的内容做匹配
     */
    private boolean matchDisconnectorandBreaker(Node breakerNode, Node disconnectorNode) {
        String breakerName = breakerNode.get("name").asString();
        // 删除开关名称后面的手车
        if (breakerName.endsWith("手车") || breakerName.endsWith("开关")) {
            breakerName = breakerName.replaceAll("手车", "");
            breakerName = breakerName.replaceAll("开关", "");
        }
        // 取出 . 号后面的字符串
        String disconnectorName = disconnectorNode.get("name").asString();
        int breakerSubIndex = breakerName.lastIndexOf(".");
        int disconnectorSubIndex = disconnectorName.lastIndexOf(".");
        if (breakerSubIndex == -1 || disconnectorSubIndex == -1) {
            return false;
        }
        String breakerSubStr = breakerName.substring(breakerSubIndex);
        String disconnectorSubStr = disconnectorName.substring(disconnectorSubIndex);
        // 进一步删除这部分字符串的标点符号
        breakerSubStr = removeComma(breakerSubStr);
        disconnectorSubStr = removeComma(disconnectorSubStr);
        return breakerSubStr.contains(disconnectorSubStr) || disconnectorSubStr.contains(breakerSubStr);
    }

    /**
     * 将字符串去除中英文标点符号
     */
    private String removeComma(String s) {
        return s.replaceAll("\\pP|\\pS|\\pC|\\pN|\\pZ", "");
    }

    private void constructRelationship(String idFrom, String idTo, String nd) {
        Neo4jUtil.runCypherSql(String.format("match (n), (m) where n.cusId = '%s' and m.cusId= '%s'" +
                "create (n)-[:CONNECT_WITH{srcid:'%s', dstid:'%s', nd: '%s'}->(m)", idFrom, idTo,
                idFrom, idTo, nd));
    }

    private void deleteRelationship(String idFrom, String idTo) {
        Neo4jUtil.runCypherSql(String.format("match (n)-[r:CONNECT_WITH]->(m) where n.cusId = '%s' and m.cusId = '%s' " +
                        "delete r;", idFrom, idTo));
        Neo4jUtil.runCypherSql(String.format("match (n)-[r:CONNECT_WITH]->(m) where n.cusId = '%s' and m.cusId = '%s' " +
                "delete r;", idTo, idFrom));
    }
}
