package com.gridgraphprocessing.algo;

import com.gridgraphprocessing.algo.service.graph.algo.GraphSearchAlgorithm;
import com.gridgraphprocessing.algo.util.Neo4jUtil;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.com.google.common.base.Stopwatch;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
class TimeCostTest {

    private long times = 1000L;
    private String formatter = "%s %d times using %d ms";

    @Autowired
    GraphSearchAlgorithm graphSearchAlgorithm;



    @Before
    public void setUp() throws Exception {

    }

    public <T> long calTimeCost(String testName,T testCase,Consumer<T> action){
        Stopwatch watch = Stopwatch.createStarted();
        for (long i = 0; i < times; i++) {
            action.accept(testCase);
        }
        watch.stop();
        long timeCost = watch.elapsed(TimeUnit.MILLISECONDS);
        String result = String.format(formatter, testName, times, timeCost);
        System.out.println(result);
        return timeCost;
    }


    @Test
    public void test(){
//        List<Path> paths = graphSearchAlgorithm.searchUpstreamReturnPaths("3800475135564528897").subList(0,1);
//        graphSearchAlgorithm.searchFaultUpstream("3800475135564528897");

        long time1=0 ,time2 = 0;
        for(int i=0;i<20;i++){
            if(i%4==0||i%4==3) {

            }
            if(i%4==1||i%4==2){

            }
            if(i%2==1){
                System.out.printf("time1:%d vs time2:%d%n",time1,time2);
            }
        }



    }



}
