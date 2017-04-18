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

    public <T> T get(Enum type, String key, boolean setExpire, int expireSeconds) throws NoCacheException {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        return get(keyBytes, setExpire, expireSeconds);
    }
    public <T> T get(byte[] keyBytes, boolean setExpire, int expireSeconds) throws NoCacheException {
        byte[] valueBytes = binaryCacheRepositoryInterface.get(keyBytes, setExpire, expireSeconds);

        if (valueBytes.length <= 0)
            return null;
        return (T) serializer.deserializeObject(valueBytes);
    }

    public void set(Enum type, String key, Object value) throws Throwable {
        set(MyCacheUtils.generateKeyBytes(type, key), value);
    }
    public void set(byte[] keyBytes, Object value) throws Throwable {
        byte[] valueBytes;
        if (null == value) {
            valueBytes = BinaryUtils.emptyByteArray;
        } else {
            valueBytes = serializer.serializeObjectAndClass(value);
        }

        binaryCacheRepositoryInterface.set(keyBytes, valueBytes);
    }

    public void set(Enum type, String key, Object value, int expireSeconds) throws Throwable {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        set(keyBytes, value, expireSeconds);
    }
    public void set(byte[] keyBytes, Object value, int expireSeconds) throws Throwable {
        byte[] valueBytes;
        if (null == value) {
            valueBytes = BinaryUtils.emptyByteArray;
        } else {
            valueBytes = serializer.serializeObjectAndClass(value);
        }

        binaryCacheRepositoryInterface.set(keyBytes, valueBytes, expireSeconds);
    }

    public void del(Enum type, String key) throws Throwable {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        binaryCacheRepositoryInterface.del(keyBytes);
    }
}

