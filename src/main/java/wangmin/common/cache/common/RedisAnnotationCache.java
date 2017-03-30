package wangmin.common.cache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wm on 2017/3/22.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface RedisAnnotationCache {
    String type();                  // 所属业务, 当做key的前缀, 不能为空
    int keyIndex() default 0;       // 缓存key参数位置, 从0开始
    int expiration() default 30000; // 缓存超时毫秒数, 默认30秒
}
