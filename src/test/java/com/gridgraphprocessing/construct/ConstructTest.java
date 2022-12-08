package com.gridgraphprocessing.construct;

import com.gridgraphprocessing.construct.service.construct.ConstructService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Yunthin.Chow
 * @date 2022/12/2 17:52
 * @description
 */
@SpringBootTest
public class ConstructTest {

    @Autowired
    ConstructService constructService;

    @Test
    void testWash() {
        constructService.adjustUpstreamBreaker();
    }
}
