package wangmin.common.cache;

import com.google.common.collect.Lists;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import wangmin.common.cache.serializer.ObjectSerializer;
import wangmin.common.utils.BinaryUtils;

import java.util.List;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 * 只封装了cache的get 和 set, 但是 避免了 缓存击穿的情况
 */
public class CacheRepositoryImpl extends BaseCacheRepository {
    private static byte[] generateKeyBytes(Enum type, String key) {
        if (null == type) {
            return BinaryUtils.strToBytes(key);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(type.name());
            sb.append('~');
            sb.append(key);
            return BinaryUtils.strToBytes(sb.toString());
        }
    }

    // redis 连接工厂
    private JedisConnectionFactory connFactory;
    @Override
    protected JedisConnectionFactory getConnFactory() {
        return connFactory;
    }
    public void setConnFactory(JedisConnectionFactory connFactory) {
        this.connFactory = connFactory;
    }

    // 默认过期时间
    private int defaultExpiration;
    public void setDefaultExpiration(int defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }

    // 序列化
    private ObjectSerializer<Object> objectSerializer;
    public void setObjectSerializer(ObjectSerializer<Object> objectSerializer) {
        this.objectSerializer = objectSerializer;
    }

    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: null代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    public <T> T get(Enum type, String key, boolean setExpire) throws NoCacheException {
        return get(type, key, setExpire, defaultExpiration);
    }
    public <T> T get(Enum type, String key, boolean setExpire, int expiration) throws NoCacheException {
        byte[] keyBytes = generateKeyBytes(type, key);
        byte[] valueBytes = super.get(keyBytes, setExpire, expiration);

        if (valueBytes.length <= 0)
            return null;
        return (T) objectSerializer.deserialize(valueBytes);
    }

    public <T> List<T> mGet(Enum type, String... keys) {
        byte[][] keyBS = new byte[keys.length][];
        for (int i=0; i<keys.length; ++i) {
            byte[] keyBytes = generateKeyBytes(type, keys[i]);
            keyBS[i] = keyBytes;
        }

        List<byte[]> valueBS = super.mGet(keyBS);

        List<T> result = Lists.newArrayListWithCapacity(keys.length);
        for (int i=0; i<keys.length; ++i) {
            byte[] valueB = valueBS.get(i);
            T element;
            if (null == valueB || 0 == valueB.length)
                element = null;
            else
                element = (T) objectSerializer.deserialize(valueBS.get(i));
            result.add(i, element);
        }
        return result;
    }

    public void set(Enum type, String key, Object value) throws Throwable {
        set(type, key, value, this.defaultExpiration);
    }
    public void set(Enum type, String key, Object value, int expiration) throws Throwable {
        byte[] keyBytes = generateKeyBytes(type, key);

        byte[] valueBytes;
        if (null == value) {
            valueBytes = BinaryUtils.emptyByteArray;
        } else {
            valueBytes = objectSerializer.serialize(value);
        }

        super.set(keyBytes, valueBytes, expiration);
    }

    public void del(Enum type, String key) throws Throwable {
        byte[] keyBytes = generateKeyBytes(type, key);
        super.del(keyBytes);
    }
}

