package wangmin.common.cache.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import wangmin.common.cache.BaseCacheRepository;
import wangmin.common.cache.CacheRepositoryImpl;

import java.lang.reflect.Method;

/**
 * Created by wm on 2017/3/22.
 */
@Aspect
public class RedisAnnotationCacheAspect implements Ordered {
    private final static Logger logger = LoggerFactory.getLogger(RedisAnnotationCacheAspect.class);

    private CacheRepositoryImpl cacheRepository;
    public void setCacheRepository(CacheRepositoryImpl cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @Pointcut("@annotation(wangmin.common.cache.annotation.RedisAnnotationCache)")
    public void cacheService() {
    }

    @Around("cacheService()")
    public Object interceptCacheMethod(ProceedingJoinPoint pjp) throws Throwable {
        RedisAnnotationCache redisAnnotationCache = getRedisAnnotationCache(pjp);
        if (null == redisAnnotationCache)
            throw new RuntimeException("RedisAnnotationCache is null");

        if (StringUtils.isEmpty(redisAnnotationCache.type()))
            throw new RuntimeException("type should not be empty");

        Object[] args = pjp.getArgs();
        int keyIndex = redisAnnotationCache.keyIndex();
        if (keyIndex < 0 || args.length <= keyIndex)
            throw new RuntimeException("keyIndex is invalid");
        Object key = args[keyIndex];
        if (null == key)
            throw new RuntimeException("key should not be null");

        String keyStr = generateKeyStr(redisAnnotationCache.type(), key);
        try {
            Object result = cacheRepository.get(null, keyStr, false, redisAnnotationCache.expiration());
            return result;
        } catch (BaseCacheRepository.NoCacheException e) {
            Object result = pjp.proceed();
            try {
                cacheRepository.set(null, keyStr, result, redisAnnotationCache.expiration());
            } catch (Throwable throwable) {
                logger.info("", throwable);
            }
            return result;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }


    /**
     * 生成key的字符串
     * **/
    private static String generateKeyStr(String type, Object key) {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append('~');
        sb.append(key);
        return sb.toString();
    }
    /**
     * 获取注解信息
     * **/
    private RedisAnnotationCache getRedisAnnotationCache(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        RedisAnnotationCache sunriseCache = method.getAnnotation(RedisAnnotationCache.class);

        if (sunriseCache == null) {
            try {
                Method targetMethod = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());

                if (targetMethod != null) {
                    sunriseCache = targetMethod.getAnnotation(RedisAnnotationCache.class);
                }

            } catch (NoSuchMethodException e) {
                sunriseCache = null;
            }

        }
        return sunriseCache;
    }
}
