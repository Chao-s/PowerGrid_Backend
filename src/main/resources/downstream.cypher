// 展示开关的总览
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564528897})
call apoc.path.expand(targetCB, 'CONNECT_WITH', null, 1, 20) yield path
return path;



// 展示下游的搜索路径
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564528897}),
      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})
YIELD path
with nodes(path) as upstreamNodes, targetCB
// 找所有的路径，只不过要将 upstreamNodes 当作黑名单，剩下来的就是下游的了
// 找所有的 cb，其中 point = 0 或 - 1
match (targetCB),
      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)
  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id
  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id
with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes
call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,
                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})
yield path
return path;



// 首先找上游的路径
MATCH (targetCB:DMS_CB_DEVICE{id: 3800475135564528897}),
      (busBarSections:BUSBARSECTION{dync_component_id: targetCB.dync_component_id, component_id: targetCB.component_id})
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'POWER_CONNECT', limit : 2})
YIELD path
with nodes(path) as upstreamNodes, targetCB
// 找所有的路径，只不过要将 upstreamNodes 当作黑名单，剩下来的就是下游的了
// 找所有的 cb，其中 point = 0 或 - 1
match (targetCB),
      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)
  where (disconnectCb.point = 0 or disconnectCb.point = -1) and disconnectCb.feeder_id = targetCB.feeder_id
  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id
with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes
call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,
                                        bfs: true, relationshipFilter: 'CONNECT_WITH'})
yield path
with nodes(path) as downStreamNodes, targetCB, endNodes
// 找出下游的开关、LD、TR
call apoc.path.subgraphAll(targetCB, {whitelistNodes: downStreamNodes,
                                       relationshipFilter: 'CONNECT_WITH', endNodes: endNodes})
YIELD nodes
return nodes;