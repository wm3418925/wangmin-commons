package wangmin.common.cache.common;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 */
public interface BinaryCacheRepositoryInterface {
    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: byte数组长度为0代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    byte[] get(byte[] keyBytes, boolean setExpire, int expirationSeconds) throws NoCacheException;

    /**
     * valueBytes 为空或者长度为0, 代表添加一个空的cache
     * */
    void set(byte[] keyBytes, byte[] valueBytes, int expirationSeconds) throws Throwable;

    void del(byte[] keyBytes) throws Throwable;
}

