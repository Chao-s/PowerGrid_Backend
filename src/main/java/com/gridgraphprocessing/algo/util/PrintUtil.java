package com.gridgraphprocessing.algo.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class PrintUtil {

    public static  <T>void printCollection(Collection<T> collection){
        System.out.println(collection.size()+" in all");
        int i=1,max=10;
        for(T element:collection){
            System.out.println(element);
            if(i>=max){
                break;
            }
            i+=1;
        }
    }

    public static <T>void printCollection(Collection<T>  collection, Function<T,Collection<Map<String,Object>>> function){
        System.out.println(collection.size()+" in all");
        int i=1,max=10;
        for(T element:collection){
            System.out.println("raw: "+element);
            System.out.println("derived: "+function.apply(element));
            if(i>=max){
                break;
            }
            i+=1;
        }
    }
}
