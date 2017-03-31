package wangmin.common.cache.common;

import wangmin.common.utils.BinaryUtils;

/**
 * Created by wm on 2017/3/30.
 */
public abstract class MyCacheUtils {
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

}
