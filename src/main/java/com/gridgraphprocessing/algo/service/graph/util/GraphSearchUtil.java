package com.gridgraphprocessing.algo.service.graph.util;

import com.gridgraphprocessing.algo.util.Neo4jUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static com.gridgraphprocessing.algo.util.PrintUtil.printCollection;

@Component
public class GraphSearchUtil {
    public static List<HashMap<String, Object>> upstreamSearch(String dmsCbDeviceId) {
        String cypherSqlFmt1 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s})," +
                "(busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamPathNodes, targetCB\n" +
                "call apoc.path.subgraphAll(targetCB, {whitelistNodes: upstreamPathNodes, labelFilter: '/DMS_CB_DEVICE'})\n" +
                "yield nodes\n" +
                "return nodes;";

        String cypherSqlFmt2 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s})," +
                "(busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'CONNECT_WITH', limit : 2})\n" +
                "YIELD path\n" +
                "WITH nodes(path) AS pathNodes, targetCB\n" +
                "match (cbEnd: DMS_CB_DEVICE) where cbEnd in pathNodes\n" +
                "call apoc.path.spanningTree(targetCB, {whiteListNodes: pathNodes, relationshipFilter: 'CONNECT_WITH', terminatorNodes: cbEnd,  limit : 1})\n" +
                "yield path as res\n" +
                "with nodes(res) as cbs, targetCB\n" +
                "MATCH (n:DMS_CB_DEVICE)  where n in cbs and n <> targetCB\n" +
                "return n;";
        String cypherSql = String.format(cypherSqlFmt1, dmsCbDeviceId);
        List<HashMap<String, Object>> graphNodeList = Neo4jUtil.getGraphNode(cypherSql,false);
        System.out.println(cypherSql);
        System.out.println("___");
//        System.out.println(graphNodeList.size()+" in all as in List, Set shown as below:");
//        Set<HashMap<String, Object>> nodeSet = new HashSet<>(graphNodeList);
        printCollection(graphNodeList);
        System.out.println("___");
        return graphNodeList;
    }

    public static List<HashMap<String, Object>> downstreamSearch(String dmsCbDeviceId) {
        String cypherSqlFmt1 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
                "      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamNodes, targetCB\n" +
                "match (targetCB),\n" +
                "      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)\n" +
                "  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id\n" +
                "  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id\n" +
                "with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes\n" +
                "call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,\n" +
                "                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})\n" +
                "yield path\n" +
                "return path;";

        String cypherSqlFmt2 = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
                "      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})\n" +
                "WITH targetCB, collect(busBarSections) AS endNodes\n" +
                "CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})\n" +
                "YIELD path\n" +
                "with nodes(path) as upstreamNodes, targetCB\n" +
                "match (targetCB),\n" +
                "      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)\n" +
                "  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id\n" +
                "  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id\n" +
                "with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes\n" +
                "call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,\n" +
                "                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})\n" +
                "yield path\n" +
                "with nodes(path) as downStreamNodes, targetCB, endNodes\n" +
                "call apoc.path.subgraphAll(targetCB, {whitelistNodes: downStreamNodes,\n" +
                "                                       relationshipFilter: 'CONNECT_WITH', endNodes: endNodes})\n" +
                "YIELD nodes\n" +
                "return nodes;";
        String cypherSql = String.format(cypherSqlFmt1, dmsCbDeviceId);
        List<HashMap<String, Object>> graphNodeList = Neo4jUtil.getGraphNode(cypherSql,false);
        System.out.println(cypherSql);
        System.out.println("___");
        printCollection(graphNodeList);
        System.out.println("___");
        return graphNodeList;
    }
}
