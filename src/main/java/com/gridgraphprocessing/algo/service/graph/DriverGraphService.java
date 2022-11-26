package com.gridgraphprocessing.algo.service.graph;



import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface DriverGraphService {

	/**
	 * 删除Neo4j 标签
	 *
	 * @param label
	 */
	void deleteByLabel(String label);

	/**
	 * 获取节点列表
	 *
	 * @param label
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	HashMap<String, Object> getLabelNodes(String label, Integer pageIndex, Integer pageSize);

	/**
	 * 获取某个领域指定节点拥有的上下级的节点数
	 *
	 * @param label
	 * @param nodeId
	 * @return long 数值
	 */
	long getRelationNodeCount(String label, long nodeId);

	/**
	 * 创建领域,默认创建一个新的节点,给节点附上默认属性
	 *
	 * @param label
	 */
	void createLabel(String label);
	void quickCreateLabel(String label,String nodeName);
	/**
	 * 获取/展开更多节点,找到和该节点有关系的节点
	 *
	 * @param label
	 * @param nodeId
	 * @return
	 */
	HashMap<String, Object> getMoreRelationNode(String label, String nodeId);

	/**
	 * 更新节点名称
	 *
	 * @param label
	 * @param nodeId
	 * @param nodeName
	 * @return 修改后的节点
	 */
	HashMap<String, Object> updateNodeName(String label, String nodeId, String nodeName);

//	/**
//	 * 创建单个节点
//	 *
//	 * @param label
//	 * @param entity
//	 * @return
//	 */
//	HashMap<String, Object> createNode(String label, NodeItem entity);
//	HashMap<String, Object> createNodeWithUUid(String label, NodeItem entity);

	/**
	 * 批量创建节点和关系
	 *
	 * @param label
	 *            领域
	 * @param sourceName
	 *            源节点
	 * @param relation
	 *            关系
	 * @param targetNames
	 *            目标节点数组
	 * @return
	 */
	HashMap<String, Object> batchCreateNode(String label, String sourceName, String relation, String[] targetNames);

	/**
	 * 批量创建下级节点
	 *
	 * @param label
	 *            领域
	 * @param sourceId
	 *            源节点id
	 * @param entityType
	 *            节点类型
	 * @param targetNames
	 *            目标节点名称数组
	 * @param relation
	 *            关系
	 * @return
	 */
	HashMap<String, Object> batchCreateChildNode(String label, String sourceId, Integer entityType,
												 String[] targetNames, String relation);

	/**
	 * 批量创建同级节点
	 *
	 * @param label
	 *            领域
	 * @param entityType
	 *            节点类型
	 * @param sourceNames
	 *            节点名称
	 * @return
	 */
	List<HashMap<String, Object>> batchCreateSameNode(String label, Integer entityType, String[] sourceNames);

	/**
	 * 添加关系
	 *
	 * @param label
	 *            领域
	 * @param sourceId
	 *            源节点id
	 * @param targetId
	 *            目标节点id
	 * @param ship
	 *            关系
	 * @return
	 */
	HashMap<String, Object> createLink(String label, long sourceId, long targetId, String ship);
	HashMap<String, Object> createLinkByUuid(String label, long sourceId, long targetId, String ship);

	/**
	 * 更新关系
	 *
	 * @param label
	 *            领域
	 * @param shipId
	 *            关系id
	 * @param shipName
	 *            关系名称
	 * @return
	 */
	HashMap<String, Object> updateLink(String label, long shipId, String shipName);

	/**
	 * 删除节点(先删除关系再删除节点)
	 *
	 * @param label
	 * @param nodeId
	 * @return
	 */
	List<HashMap<String, Object>> deleteNode(String label, long nodeId);

	/**
	 * 删除关系
	 *
	 * @param label
	 * @param shipId
	 */
	void deleteLink(String label, long shipId);

	/**
	 * 段落识别出的三元组生成图谱
	 *
	 * @param label
	 * @param entityType
	 * @param operateType
	 * @param sourceId
	 * @param rss
	 *            关系三元组
	 *            [[startname;ship;endname],[startname1;ship1;endname1],[startname2;ship2;endname2]]
	 * @return node relationship
	 */
	HashMap<String, Object> createGraphByText(String label, Integer entityType, Integer operateType, Integer sourceId,
			String[] rss);
	/**
	 * 批量创建节点，关系
	 * @param label
	 * @param params 三元组 sourceNode,relationship,targetNode
	 */
	void batchCreateGraph(String label, List<Map<String,Object>> params);


	/**
	 * 更新节点有无附件
	 * @param label
	 * @param nodeId
	 * @param status
	 */
	void updateNodeFileStatus(String label,long nodeId, int status);

	/**
	 * 更新图谱节点的图片
	 * @param label
	 * @param nodeId
	 * @param img
	 */
	void updateNodeImg(String label, long nodeId, String img);

	/**
	 * 导入csv
	 * @param label
	 * @param csvUrl
	 * @param status
	 */
	void batchInsertByCsv(String label, String csvUrl, int status) ;
	void updateCoordinateOfNode(String label, String uuid, Double fx, Double fy);
}
