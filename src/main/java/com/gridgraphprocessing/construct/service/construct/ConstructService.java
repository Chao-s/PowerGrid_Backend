package com.gridgraphprocessing.construct.service.construct;

/**
 * @author Yunthin.Chow
 * @date 2022/12/2 15:56
 * @description 图数据库构建服务接口类
 */
public interface ConstructService {

    /**调整上游的 BREAKER 、DISCONNECTOR、GROUND_DISCONNECTOR 的连接关系*/
    void adjustUpstreamBreaker();
}
