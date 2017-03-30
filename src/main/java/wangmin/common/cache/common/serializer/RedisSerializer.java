package wangmin.common.cache.common.serializer;

/**
 * Created by wm on 2017/3/21.
 */
public interface RedisSerializer {
    /**序列化对象和class**/
    byte[] serializeObjectAndClass(Object object);
    /**反序列化对象, byte[]包含类信息**/
    <T> T deserializeObject(byte[] byteArray);

    /**序列化对象, 不包含class信息**/
    byte[] serializeObject(Object object);
    /**反序列化对象**/
    <T> T deserializeObjectByClass(byte[] byteArray, Class<T> clazz);
}
