// 取出所有的 DISCONNECTOR
MATCH (n:DISCONNECTOR) return n;
// 取出某个 DISCONNECTOR 相连的所有设备
MATCH (n:DISCONNECTOR {cusId: ''}) CALL apoc.neighbors.athop(n, 'CONNECT_WITH', 1) YIELD node RETURN node;

// 匹配和 BREAKER 相连的 DISCONNECTOR
// 记录所有其他的联系，准备都连到所连接的 BREAKER 上
// 删除所有其他的联系，只保留和 BEAKER 的

MATCH (n:DISCONNECTOR {cusId: ''})-[r:CONNECT_WITH]->(m:BREAKER) delete r;

// 展示
MATCH (target:DISCONNECTOR{cusId: 'DISCONNECTOR114841791034819745'})
call apoc.path.spanningTree(target, {}) yield path
return path;