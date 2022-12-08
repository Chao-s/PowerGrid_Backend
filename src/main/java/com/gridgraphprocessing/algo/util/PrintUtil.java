package com.gridgraphprocessing.algo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gridgraphprocessing.algo.util.Neo4jUtil.treeIdxMapDeepCopy;

public class PrintUtil {

    public static <T> void printCollection(Collection<T> collection){
        printCollection(collection,t->t);
    }

    public static <T,R>void printCollection(Collection<T>  collection, Function<T,R> function){//R 比如 Collection<Map<String,Object>>
        printCollection(collection,t->{
            System.out.println(function.apply(t));
        });
    }

    public static <T> void printCollection(Collection<T> collection, Consumer<T> action){
        if(collection.isEmpty()){
            System.out.println("__0 in all__");
            return;
        }
        System.out.printf("%d in all, shown as below:%n",collection.size());
        int i=1,max=10;
        for(T element:collection){
            action.accept(element);
            if(i>=max){
                System.out.println("...(省略其余部分)");
                break;
            }
            i++;
        }
        System.out.println("___");
    }

    //打印语句与返回的集合的装饰器
    public static<T> Collection<T> printDecorator(String sql, Function<String, Collection<T>> function){
        System.out.println(sql);
        System.out.println("__Just Wait For the Results...__");
        Collection<T> resultList = function.apply(sql);
        assert resultList != null;
        printCollection(resultList);
        return resultList;
    }

    //可作为通过mapListToTree遍历数组的代码模板
    public static <T,R> void printListTreeWithMap(List<T> list, Map<Integer, LinkedList<Integer>> mapListToTree,Function<T,R> function){//从每一行的结尾节点进入下面的路径分支
        if(list.isEmpty()) {
            System.out.println("__No PathTree here__");
            return;
        }
        Map<Integer, LinkedList<Integer>> mapCopy = treeIdxMapDeepCopy(mapListToTree);
        Stack<Integer> nodeStack = new Stack<>();//缓存进入下一条支路的分支点
        Stack<String> prefixStack = new Stack<>();
        Integer treeIdx = 0;

        int nameLenLimit = 20;
        String elementForm = "%."+nameLenLimit+"s";
        System.out.printf("__PathTree shown as below | 只从每行的末尾节点进入分支；节点名超出限长部分不打印 | %s node(s) in all__%n",list.size());
        int prefixUnit = 2;//经实践，”已走过的路径长度“是不适当的前缀，前缀不用太长，此处设定为进入支路的前缀增加两个空格
        String prefix = "";
        String separator = "-";
        String dirChar = "|";
        String newBranchPrefix = String.format("%"+prefixUnit+"s%s","",dirChar);
        while(true){
            System.out.printf(elementForm,function.apply(list.get(treeIdx)));//此处为唯一的节点遍历入口
            if(mapCopy.containsKey(treeIdx)){//根据当前逻辑，此处遇见的分支点和末尾节点都是第一次遇见，不存在从支路末尾返回后从这里进入新支路的情况
                LinkedList<Integer> nextNodes = mapCopy.get(treeIdx);
                if(nextNodes.isEmpty()){//根据当前逻辑，此处必定在支路的末尾节点(支路末尾的索引为空列表)，因为遍历完支线的分支点不会入栈
                    if(nodeStack.isEmpty()){
                        System.out.println();
                        break;
                    }
                    treeIdx = nodeStack.peek();
                    prefix = prefixStack.peek();
                    nextNodes = mapCopy.get(treeIdx);
                }else {//第一次遇到分支点
                    nodeStack.push(treeIdx);
                    prefixStack.push(prefix);
                }
                //进入新的分支
                if(nextNodes.size()==1){//不回到已经没有支路要走的branchIdx；此时为再次遇到分支点的情况，因为第一次遇到的分支点必有分支数>1
                    nodeStack.pop();
                    prefixStack.pop();
                }
                treeIdx = nextNodes.remove(0);//删除要踏上的支路的记录
                prefix = prefix.concat(newBranchPrefix);
                System.out.println();
                System.out.print(prefix);
                System.out.print(separator);
                if(nextNodes.isEmpty()){
                    prefix = prefix.substring(0,prefix.length()-dirChar.length());
                }
            }else {//在当前路径中前进
                treeIdx++;
                System.out.print(separator);
            }
        }
        System.out.println("__PathTree shown as above__");
    }

    public static boolean confirmByConsole(String question){
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String ans = null;
        try {
            while(true){//需要在IDEA Help -> Edit Custom VM Options输入-Deditable.java.test.console=true
                System.out.printf("%s(是/否 or yes/no)：%n",question);
                ans = br.readLine();
                if(ans.equals("是")||ans.equals("yes")){
                    return true;
                }else if(ans.equals("否")||ans.equals("no")){
                    return false;
                }else {
                    System.out.println("Error input|请再次进行规范的输入");
                }
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
