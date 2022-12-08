package com.gridgraphprocessing.algo.service.graph.algo;

import com.gridgraphprocessing.algo.repository.AutoDeviceRepository;
import com.gridgraphprocessing.algo.util.Neo4jUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.gridgraphprocessing.algo.util.Neo4jUtil.treeIdxMapDeepCopy;
import static com.gridgraphprocessing.algo.util.PrintUtil.*;

@Component
@RequiredArgsConstructor//代替@Autowired
public class GraphSearchAlgorithm {
    private final AutoDeviceRepository autoDeviceRepository;

    private int count = 0;

    private static final String upstreamCypherSqlFmt = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
            "      (busBarSections:BUSBARSECTION)\n" +
            "WITH targetCB, collect(busBarSections) AS terminatorNodes\n" +
            "CALL apoc.path.spanningTree(targetCB, {terminatorNodes: terminatorNodes, relationshipFilter: 'CONNECT_WITH', bfs: true" +
            ", limit: 2" +
            "})\n" +
            "YIELD path\n" +
            "return path;";

    private static final String downstreamCypherSqlFmt = "MATCH (targetCB:DMS_CB_DEVICE{id: %s}),\n" +
            "      (busBarSections:BUSBARSECTION)\n" +
            "WITH targetCB, collect(busBarSections) AS terminatorNodes\n" +
            "CALL apoc.path.spanningTree(targetCB, {terminatorNodes: terminatorNodes, relationshipFilter: 'CONNECT_WITH', bfs: true" +
            ", limit: 2" +
            "})\n" +
            "YIELD path\n" +
            "with nodes(path) as upstreamNodes, targetCB\n" +
            "match (targetCB),\n" +
            "      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)\n" +
            "  where\n" +
            "    (disconnectCb.point = 0 or disconnectCb.point = -1) and\n" +
            "  disconnectCb.feeder_id = targetCB.feeder_id\n" +
            "  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id\n" +
            "with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes\n" +
            "call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,\n" +
            "                                       bfs: true, relationshipFilter: 'CONNECT_WITH'})\n" +
            "yield path\n" +
            "return path;";



    public List<Path> searchUpstreamReturnPaths(String dmsCbDeviceId) {
        String cypherSql = String.format(upstreamCypherSqlFmt, dmsCbDeviceId);
        return new ArrayList<>(printDecorator(cypherSql,Neo4jUtil::getRawPathSet));
    }

    public List<Path> searchDownstreamReturnPaths(String dmsCbDeviceId) {
        String cypherSql = String.format(downstreamCypherSqlFmt, dmsCbDeviceId);
        return new ArrayList<>(printDecorator(cypherSql,Neo4jUtil::getRawPathSet));
    }



    public boolean confirmCBStatus(Node node, String dmsCbDeviceId){
        //toDo 获取电流信息
        int point = node.get("point").asInt();
        System.out.printf("point: %d | ",point);
        System.out.printf("id of auto DMS_CB_DEVICE found now: %s%n",dmsCbDeviceId);
        System.out.println("unfinished function: 获取并比较当前电流与30s(或其他时间间隔)前的电流...");
        boolean isFault = false;
        //手动输入信息
        boolean done_manually = false;
        if(done_manually){
            isFault = confirmByConsole("请输入自动化开关当前电流是否相较之前有变化");
        }else {
            isFault = count%4==1;
            System.out.printf("暂时按给定布尔序列进行电流判断，当前isFault:%b%n",isFault);
            count++;
        }
        return isFault;
    }

    public List<List<Node>> searchFaultUpstream(String faultId){//目前用列表记录faultNode，也可以选择用树结构数组
        Collection<Path> paths = searchUpstreamReturnPaths(faultId);
        Map<Integer,LinkedList<Integer>> mapListToTree = new HashMap<>();
        List<Node> nodeTree = Neo4jUtil.getRawNodeTree(paths,mapListToTree);//
        printListTreeWithMap(nodeTree,mapListToTree,t->Neo4jUtil.getNodePriLabel(t.labels()));

//        List<HashMap<String,Object>> faultNodes = new ArrayList<>();//因为节点的路径信息也包括节点，故暂时注释
        List<List<Node>> faultNodePaths = new ArrayList<>();
        List<Node> faultNodePathNow = new LinkedList<>();

        if(nodeTree.isEmpty()) return faultNodePaths;

        Map<Integer, LinkedList<Integer>> mapCopy = treeIdxMapDeepCopy(mapListToTree);
        Stack<Integer> nodeStack = new Stack<>();
        Stack<Integer> faultNodePathBranchStack = new Stack<>();//此处索引为faultNodePathNow的索引，和treeIdx不一样；存的是分支点索引+1的值
        Integer treeIdx = 0;
        Node node = null;
        while(true){
            node = nodeTree.get(treeIdx);
            faultNodePathNow.add(node);
            if(treeIdx!=0&&Neo4jUtil.contains(node.labels(),"DMS_CB_DEVICE")){//检索是否为有问题的自动化开关
                String dmsCbDeviceId = Neo4jUtil.getNodeRealId(node);
                Integer isAuto = autoDeviceRepository.findIsAutoById(dmsCbDeviceId);
                System.out.printf("DMS_CB_DEVICE found|id:%s",dmsCbDeviceId);
                if(isAuto==null){
                    System.out.println("|property is_auto not found !");
                }else if(isAuto==1){
                    System.out.print("|auto");
                    System.out.printf("|~> at %s%n", Neo4jUtil.getNodePathFeature(faultNodePathNow,"-"));
                    //toDo 判断开关是否有问题，待完善
                    boolean isFault = confirmCBStatus(node,dmsCbDeviceId);
                    if(isFault){//相当于提前遇到支路终点，所以nodeStack出栈
                        //此处为唯一的发现faultNode之后的处理
                        HashMap<String,Object> deviceMap = Neo4jUtil.nodeToMap(node);
//                        faultNodes.add(deviceMap);//记录有问题的自动化开关节点（也可直接返回Node类型的引用）；因为节点的路径信息也包括节点，故暂时注释
                        faultNodePaths.add(new LinkedList<>(faultNodePathNow));//添加节点列表的浅拷贝，预期中只有遍历需求，所以对节点进行重用
                        System.out.printf("!!! faultNode found:%s%n",Neo4jUtil.getDeviceToShow(deviceMap));
                        if(nodeStack.isEmpty()){
                            break;
                        }else {
                            treeIdx = nodeStack.peek();
                            faultNodePathNow.subList(faultNodePathBranchStack.peek(),faultNodePathNow.size()).clear();
                        }
                    }
                }else{
                    System.out.printf("|not auto, value:%d%n",isAuto);
                }
            }
            //以上为唯一的节点遍历入口

            if(mapCopy.containsKey(treeIdx)){//由于遇到faultNode提前出栈的逻辑，此处不仅限末尾节点与第一次遇到分支点，而是包括各种情况
                LinkedList<Integer> nextNodes = mapCopy.get(treeIdx);
                if(nextNodes.isEmpty()){//根据当前逻辑，此处必定在支路的末尾节点
                    System.out.printf("no faultNode found at this branch: %s%n",Neo4jUtil.getNodePathFeature(faultNodePathNow,"-"));
                    if(nodeStack.isEmpty()){
                        break;
                    }
                    treeIdx = nodeStack.peek();
                    faultNodePathNow.subList(faultNodePathBranchStack.peek(),faultNodePathNow.size()).clear();
                    nextNodes = mapCopy.get(treeIdx);
                }else {//遇到分支点（因为有遇faultNode提前从支路返回的机制，所以不仅限第一次遇到）
                    if(!nodeStack.contains(treeIdx)){
                        //第一次遇到分支点的入口；因为找到faultNode后提前出栈回到分支点，所以额外增加了contains判断来筛选出第一次遇到分支点的情况
                        nodeStack.push(treeIdx);
                        faultNodePathBranchStack.push(faultNodePathNow.size());//存的是faultNodePathNow列表中分支点索引+1的值，便于subList.clear处删除
                    }
                }
                //进入新的分支
                if(nextNodes.size()==1){//此时为再次遇到分支点的情况，因为第一次遇到的分支点必有分支数>1；避免回到已经没有支路要走的branchIdx
                    nodeStack.pop();
                    faultNodePathBranchStack.pop();
                }
                treeIdx = nextNodes.remove(0);
            }else {
                treeIdx++;//在路径中前进
            }
        }
        System.out.println("枚举每条支线中第一个问题节点如下（无问题的支线则跳过）:");
        printCollection(faultNodePaths,path->{
            System.out.printf("at %s%n",Neo4jUtil.getNodePathFeature(path,"-"));
            Node faultNode = path.get(path.size()-1);
            System.out.printf("有问题的自动化开关: %s%n",Neo4jUtil.getDeviceToShow(Neo4jUtil.nodeToMap(faultNode)));
        });
        return faultNodePaths;
    }

    public List<List<Node>> searchAffectedEquipDownstream(String faultId){
        Collection<Path> paths = searchDownstreamReturnPaths(faultId);
        Map<Integer,LinkedList<Integer>> mapListToTree = new HashMap<>();
        List<Node> nodeTree = Neo4jUtil.getRawNodeTree(paths,mapListToTree);//
        printListTreeWithMap(nodeTree,mapListToTree,t->Neo4jUtil.getNodePriLabel(t.labels()));

//        List<HashMap<String,Object>> targetNodes = new ArrayList<>();//因为节点的路径信息也包括节点，故暂时注释
        List<List<Node>> targetNodePaths = new ArrayList<>();
        List<Node> targetNodePathNow = new LinkedList<>();

        if(nodeTree.isEmpty()) return targetNodePaths;

        Map<Integer, LinkedList<Integer>> mapCopy = treeIdxMapDeepCopy(mapListToTree);
        Stack<Integer> nodeStack = new Stack<>();
        Stack<Integer> targetNodePathBranchStack = new Stack<>();//此处索引为targetNodePathNow的索引，和treeIdx不一样；存的是分支点索引+1的值
        Integer treeIdx = 0;
        Node node = null;
        node = nodeTree.get(treeIdx);
        targetNodePathNow.add(node);
        while(true){

            if(mapCopy.containsKey(treeIdx)){//仅限末尾节点与第一次遇到分支点的情况
                LinkedList<Integer> nextNodes = mapCopy.get(treeIdx);
                if(nextNodes.isEmpty()){//根据当前逻辑，此处必定在支路的末尾节点
                    if(nodeStack.isEmpty()){
                        break;
                    }
                    treeIdx = nodeStack.peek();
                    targetNodePathNow.subList(targetNodePathBranchStack.peek(),targetNodePathNow.size()).clear();
                    nextNodes = mapCopy.get(treeIdx);
                }else {//第一次遇到分支点（没有遇targetNode提前从支路返回的机制，所以仅限第一次遇到）
                    //若有遇targetNode提前从支路返回的机制，则应添加if(!nodeStack.contains(treeIdx))判断
                    nodeStack.push(treeIdx);
                    targetNodePathBranchStack.push(targetNodePathNow.size());//存的是targetNodePathNow列表中分支点索引+1的值，便于subList.clear处删除
                }
                //进入新的分支
                if(nextNodes.size()==1){//此时为再次遇到分支点的情况，因为第一次遇到的分支点必有分支数>1；避免回到已经没有支路要走的branchIdx
                    nodeStack.pop();
                    targetNodePathBranchStack.pop();
                }
                treeIdx = nextNodes.remove(0);
            }else {
                treeIdx++;//在路径中前进
            }
            node = nodeTree.get(treeIdx);
            targetNodePathNow.add(node);
            Iterable<String> labels = node.labels();
            boolean userEquipmentFound = Neo4jUtil.contains(labels,"DMS_TR_DEVICE")||Neo4jUtil.contains(labels,"DMS_LD_DEVICE");
            boolean breakerFound = Neo4jUtil.contains(labels,"DMS_CB_DEVICE")||Neo4jUtil.contains(labels,"BREAKER");
            if(userEquipmentFound||breakerFound){
                System.out.printf("~> at %s%n", Neo4jUtil.getNodePathFeature(targetNodePathNow,"-"));
                HashMap<String,Object> deviceMap = Neo4jUtil.nodeToMap(node);
                if(userEquipmentFound){
//                    targetNodes.add(deviceMap);//因为节点的路径信息也包括节点，故暂时注释
                    targetNodePaths.add(new LinkedList<>(targetNodePathNow));//添加节点列表的浅拷贝，预期中只有遍历需求，所以对节点进行重用
                    System.out.printf("发现下游用户（暂定中压公变、或专变接入点）:%s%n",Neo4jUtil.getDeviceToShow(deviceMap));
                }else {
                    System.out.printf("发现转供点/常分开关:%s%n",Neo4jUtil.getDeviceToShow(deviceMap));
                    System.out.println("unfinished functions: 确认转供点5min前是否有变位");
                }
            }

        }

        System.out.println("输出下游用户列表如下（暂定中压公变、或专变接入点）：");
        printCollection(targetNodePaths,path->{
            System.out.printf("at %s%n",Neo4jUtil.getNodePathFeature(path,"-"));
            Node targetNode = path.get(path.size()-1);
            System.out.printf("用户设备: %s%n",Neo4jUtil.getDeviceToShow(Neo4jUtil.nodeToMap(targetNode)));
        });
        return targetNodePaths;
    }
}
