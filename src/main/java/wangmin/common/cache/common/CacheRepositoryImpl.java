package wangmin.common.cache.common;

import wangmin.common.cache.common.serializer.RedisSerializer;
import wangmin.common.utils.BinaryUtils;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 * 只封装了cache的get 和 set, 但是 避免了 缓存击穿的情况
 */
public class CacheRepositoryImpl {
    // 默认过期时间
    private int defaultExpiration;
    public void setDefaultExpiration(int defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }

    // 序列化
    private RedisSerializer serializer;
    public void setSerializer(RedisSerializer serializer) {
        this.serializer = serializer;
    }

    // 二进制接口
    private BinaryCacheRepositoryInterface binaryCacheRepositoryInterface;
    public void setBinaryCacheRepositoryInterface(BinaryCacheRepositoryInterface binaryCacheRepositoryInterface) {
        this.binaryCacheRepositoryInterface = binaryCacheRepositoryInterface;
    }

    public <T> T get(Enum type, String key, boolean setExpire) throws NoCacheException {
        return get(type, key, setExpire, defaultExpiration);
    }
    public <T> T get(byte[] keyBytes, boolean setExpire, int expiration) throws NoCacheException {
        byte[] valueBytes = binaryCacheRepositoryInterface.get(keyBytes, setExpire, expiration);

        if (valueBytes.length <= 0)
            return null;
        return (T) serializer.deserializeObject(valueBytes);
    }
    public <T> T get(Enum type, String key, boolean setExpire, int expiration) throws NoCacheException {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        return get(keyBytes, setExpire, expiration);
    }

    public void set(Enum type, String key, Object value) throws Throwable {
        set(type, key, value, this.defaultExpiration);
    }
    public void set(Enum type, String key, Object value, int expiration) throws Throwable {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        set(keyBytes, value, expiration);
    }
    public void set(byte[] keyBytes, Object value, int expiration) throws Throwable {
        byte[] valueBytes;
        if (null == value) {
            valueBytes = BinaryUtils.emptyByteArray;
        } else {
            valueBytes = serializer.serializeObjectAndClass(value);
        }

        binaryCacheRepositoryInterface.set(keyBytes, valueBytes, expiration);
    }

    public void del(Enum type, String key) throws Throwable {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        binaryCacheRepositoryInterface.del(keyBytes);
    }
}

