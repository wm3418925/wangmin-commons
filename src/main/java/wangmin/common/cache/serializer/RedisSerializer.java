package wangmin.common.cache.serializer;

import java.util.List;

/**
 * Created by wm on 2017/3/21.
 */
public interface RedisSerializer {
    byte[] serialize(Object object);
    <T> T deserialize(byte[] byteArray, Class<T> clazz);
    <E> List<E> deserializeForList(byte[] byteArray, Class<E> itemClazz);
}
