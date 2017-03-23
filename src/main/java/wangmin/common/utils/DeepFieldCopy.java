package wangmin.common.utils;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * Created by wm on 2017/3/21.
 */
public abstract class DeepFieldCopy {
    public static <FromType,ToType> ToType transform(FromType sourceObject, Class<ToType> resultClass) {
        return JSON.parseObject(JSON.toJSONString(sourceObject), resultClass);
    }

    public static <FromType,ToType> List<ToType> transformList(List<FromType> sourceList, Class<ToType> resultClass) {
        return JSON.parseArray(JSON.toJSONString(sourceList), resultClass);
    }

    public static void main(String[] argv) {
        Object source = new Object();

        System.out.println("result="+transform(source, String.class));
    }
}
