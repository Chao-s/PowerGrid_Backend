// 返回整个搜索路径
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564560470}),
      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})
YIELD path
return path;

// 方法1
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564528897}),
      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})
YIELD path
with nodes(path) as upstreamPathNodes, targetCB
call apoc.path.subgraphAll(targetCB, {whitelistNodes: upstreamPathNodes, labelFilter: '/DMS_CB_DEVICE'})
yield nodes
return nodes

// 方法2
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564528897}),
      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'CONNECT_WITH', limit : 2})
YIELD path
WITH nodes(path) AS pathNodes, targetCB
// 选上游的第一个开关
match (cbEnd: DMS_CB_DEVICE) where cbEnd in pathNodes
call apoc.path.spanningTree(targetCB, {whiteListNodes: pathNodes, relationshipFilter: 'CONNECT_WITH', terminatorNodes: cbEnd,  limit : 1})
yield path as res
with nodes(res) as cbs, targetCB
// 返回结果
MATCH (n:DMS_CB_DEVICE)  where n in cbs and n <> targetCB
return n;



