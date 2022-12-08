// 展示
MATCH (targetCB:DMS_CB_DEVICE{cusId: 'DMS_CB_DEVICE3889'})
call apoc.path.spanningTree(targetCB, {}) yield path
return path;

// 返回上游搜索路径
MATCH (targetCB:DMS_CB_DEVICE{cusId: 'DMS_CB_DEVICE3889'}),
      (busBarSections:BUSBARSECTION)
WITH targetCB, collect(busBarSections) AS terminatorNodes
CALL apoc.path.spanningTree(targetCB, {terminatorNodes: terminatorNodes, relationshipFilter: 'CONNECT_WITH', bfs: true, limit: 2})
YIELD path
return path;

// 展示下游的搜索路径
MATCH (targetCB:DMS_CB_DEVICE{cusId: 'DMS_CB_DEVICE3889'}),
      (busBarSections:BUSBARSECTION)
WITH targetCB, collect(busBarSections) AS endNodes
CALL apoc.path.spanningTree(targetCB, {endNodes: endNodes, relationshipFilter: 'CONNECT_WITH'})
YIELD path
with nodes(path) as upstreamNodes, targetCB
// 找所有的路径，只不过要将 upstreamNodes 当作黑名单，剩下来的就是下游的了
// 找所有的 cb，其中 point = 0 或 - 1
match (targetCB),
      (disconnectCb:DMS_CB_DEVICE), (lds: DMS_LD_DEVICE), (trs: DMS_TR_DEVICE)
  where
  //  (disconnectCb.point = 0 or disconnectCb.point = -1) and
  disconnectCb.feeder_id = targetCB.feeder_id
  and lds.feeder_id = targetCB.feeder_id and trs.feeder_id = targetCB.feeder_id
with targetCB, [disconnectCb, lds, trs] as endNodes, upstreamNodes
call apoc.path.spanningTree(targetCB, {endNodes: endNodes, blacklistNodes: upstreamNodes,
                                       bfs: true, relationshipFilter: 'CONNECT_WITH'})
yield path
return path;