package wangmin.common.cache.common;

import com.google.common.collect.Lists;
import wangmin.common.utils.BinaryUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by wm on 2017/3/30.
 */
public abstract class MyCacheUtils {
    public static final byte[] NX = {'N', 'X'}; // 不存在
    public static final byte[] XX = {'X', 'X'}; // 存在

    public static final byte[] EX = {'E', 'X'}; // 单位秒
    public static final byte[] PX = {'P', 'X'}; // 单位毫秒


    public static String generateKey(Enum type, String key) {
        if (null == type) {
            throw new RuntimeException("empty type for key:"+key);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(type.name());
            sb.append(':');
            sb.append(key);
            return sb.toString();
        }
    }
    public static byte[] generateKeyBytes(Enum type, String key) {
        return BinaryUtils.strToBytes(generateKey(type, key));
    }




    // 将字符串list转化为固定的字符串, 按照元素排序
    private static String strListToConstStr(List<String> strList, final String arrayInterStr) {
        // 排序
        Collections.sort(strList);

        // 使用连接符连成字符串
        StringBuilder sb = new StringBuilder();
        for (String str : strList) {
            sb.append(str);
            sb.append(arrayInterStr);
        }
        return sb.toString();
    }
    // 将集合类转化为固定的字符串, 按照元素排序
    public static String collectionToConstStr(Collection c, final String arrayInterStr) {
        // 获取list
        List<String> list = Lists.newArrayList();
        for (Object obj : c) {
            list.add(String.valueOf(obj));
        }

        return strListToConstStr(list, arrayInterStr);
    }
    // 将数组转化为固定的字符串, 按照元素排序
    public static String arrayToConstStr(Object[] a, final String arrayInterStr) {
        // 获取list
        List<String> list = Lists.newArrayList();
        for (Object obj : a) {
            list.add(String.valueOf(obj));
        }

        return strListToConstStr(list, arrayInterStr);
    }

    public static String generateKey(String type, Object key, final String arrayInterStr) {
        if (null == type) {
            throw new RuntimeException("empty type for key:"+key);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(type);
            sb.append(':');

            if (key instanceof Collection) {
                sb.append(collectionToConstStr((Collection) key, arrayInterStr));
            } else if (key != null && key.getClass().isArray()) {
                sb.append(arrayToConstStr((Object[]) key, arrayInterStr));
            } else {
                sb.append(key);
            }

            return sb.toString();
        }
    }
    public static byte[] generateKeyBytes(String type, Object key, final String arrayInterStr) {
        return BinaryUtils.strToBytes(generateKey(type, key, arrayInterStr));
    }
}
