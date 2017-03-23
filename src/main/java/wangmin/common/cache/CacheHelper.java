package wangmin.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by wm on 2017/3/21.
 * 缓存操作, 支持二级缓存
 */
public abstract class CacheHelper {
	private static final Logger logger = LoggerFactory.getLogger(CacheHelper.class);
	
	/**
     * 没有缓存抛出该异常, 而不是返回null
     * */
    public static class NoCacheException extends Exception {
		private static final long serialVersionUID = -2111078329121402611L;
    }

    /**
     * 读写缓存接口
     * */
    public interface ReadWriteCacheInterface <KeyType,ValueType> {
        ValueType read(KeyType key) throws NoCacheException,Exception;
        boolean write(KeyType key, ValueType value) throws Exception;
    }
    
    /**
     * 直接从数据源读取数据接口
     * */
    public interface ReadSourceDataInterface <KeyType,ValueType> {
        ValueType readSourceData(KeyType key) throws Exception;
    }
    
    /**
     * 缓存操作需要的类
     * */
    public static class CacheObjects<KeyType,ValueType> {
    	// 一级缓存接口, 不为null
    	public final ReadWriteCacheInterface<KeyType,ValueType> rwci1;
    	// 二级缓存接口, 可为null
    	public final ReadWriteCacheInterface<KeyType,ValueType> rwci2;
    	// 直接从数据源读取数据接口, 不为null
    	public final ReadSourceDataInterface<KeyType,ValueType> rsdi;
    	
    	public CacheObjects(ReadWriteCacheInterface<KeyType,ValueType> rwci1, 
    			ReadWriteCacheInterface<KeyType,ValueType> rwci2, 
    			ReadSourceDataInterface<KeyType,ValueType> rsdi) {
    		assert(null != rwci1);
    		assert(null != rsdi);
    		
    		this.rwci1 = rwci1;
    		this.rwci2 = rwci2;
    		this.rsdi = rsdi;
    	}
    }
    
    
    /**
     * 更新缓存调用的Callable
     * */
    public static class UpdateCacheCallable<KeyType,ValueType> implements Callable<ValueType> {
    	// 缓存操作需要的类, 不能为null
    	private final CacheObjects<KeyType,ValueType> co;
    	// 缓存对应的key, 不能为null
        private final KeyType key;
        // 是否对key使用同步锁
        private final boolean isSyncKey;
        
        public UpdateCacheCallable(CacheObjects<KeyType,ValueType> co, KeyType key, boolean isSyncKey) {
        	assert(null != co);
    		assert(null != key);
    		
        	this.co = co;
            this.key = key;
            this.isSyncKey = isSyncKey;
        }
     
        private ValueType readAndUpdateCache() throws Exception {
        	ValueType value = co.rsdi.readSourceData(key);

            try {
	            if (!co.rwci1.write(key, value)) {
	            	logger.warn("write cache1 error, key={}, value={}", key, value);
	            }
	            if (null != co.rwci2 && !co.rwci2.write(key, value)) {
	            	logger.warn("write cache2 error, key={}, value={}", key, value);
	            }
            } catch (Exception e) {
            	logger.warn("write cache error, key={}, value={}", key, value);
            	logger.warn("", e);
            }

            return value;
        }
        
        @Override
        public ValueType call() throws Exception {
        	if (isSyncKey) {
        		synchronized(key) {
	        		try {
	    	            return co.rwci1.read(key);
	    	        } catch (NoCacheException e) {
	    	        	return readAndUpdateCache();
	    	        }
	        	}
        	} else {
        		return readAndUpdateCache();
        	}
        }
    }
    
    
    /**
     * 缓存管理类
     * */
    public static class CacheManager<KeyType,ValueType> {
    	private final CacheObjects<KeyType,ValueType> co;
    	private final ExecutorService executor;
    	
    	public CacheManager(ReadWriteCacheInterface<KeyType,ValueType> rwci1, 
    			ReadWriteCacheInterface<KeyType,ValueType> rwci2, 
    			ReadSourceDataInterface<KeyType,ValueType> rsdi,
    			ExecutorService executor) {
    		co = new CacheObjects<>(rwci1, rwci2, rsdi);
    		this.executor = executor;
    	}
    	public CacheManager(ReadWriteCacheInterface<KeyType,ValueType> rwci1, 
    			ReadWriteCacheInterface<KeyType,ValueType> rwci2, 
    			ReadSourceDataInterface<KeyType,ValueType> rsdi,
    			int fixedThreadCount) {
    		co = new CacheObjects<>(rwci1, rwci2, rsdi);
    		this.executor = Executors.newFixedThreadPool(fixedThreadCount);
    	}
    	
    	
    	private ValueType updateAndGetCache(KeyType key, boolean isSyncKey) throws Exception {
    		FutureTask<ValueType> futureTask = new FutureTask<ValueType>(new UpdateCacheCallable<KeyType,ValueType>(co, key, isSyncKey));
            
    		executor.submit(futureTask);
    		
            return futureTask.get();
    	}
    	private void updateCache(KeyType key, boolean isSyncKey) throws Exception {
    		FutureTask<ValueType> futureTask = new FutureTask<ValueType>(new UpdateCacheCallable<KeyType,ValueType>(co, key, isSyncKey));
            
    		executor.submit(futureTask);
    	}
    	
    	/**
	     * 获取并更新cache (支持二级缓存, 支持缓存队列)
	     * @param key 缓存的key
	     * @param isSyncKey 是否对key使用同步锁
	     * */
	    public ValueType getAndUpdateCache(KeyType key, boolean isSyncKey) throws Exception {
	        try {
	            return co.rwci1.read(key);
	        } catch (NoCacheException e) {
	        }
	        
	        if (null != co.rwci2) {
		        try {
		        	ValueType v = co.rwci2.read(key);
		        	updateCache(key, isSyncKey);
		        	return v;
		        } catch (NoCacheException e) {
		        }
	        }

	        return updateAndGetCache(key, isSyncKey);
	    }
	    
	    /**
	     * 停止所有的缓存更新任务
	     * */
	    public void stopAllTask() {
	    	try {
				executor.awaitTermination(3, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				executor.shutdown();
			}
	    }
    }

    

    /**
     * 测试 一级缓存
     * **/
    public static void testCache1() {
        final Map<String, String> cacheMap = new HashMap<>();
        final String keyA = "key_a";
        //cacheMap.put(keyA, "testValue1");

        final ReadWriteCacheInterface<String, String> rwci1 = new ReadWriteCacheInterface<String, String>() {
            @Override
            public String read(String key) throws NoCacheException,Exception {
                if (!cacheMap.containsKey(key))
                    throw new NoCacheException();
                System.out.println("rwci1, read data of key["+key+"] from cache");
                return cacheMap.get(key);
            }

            @Override
            public boolean write(String key, String value) throws Exception {
                System.out.println("rwci1, write cache key="+key + ", value="+value);

                //int timeout = 100;
                //if (null == value)  // 空值需要过期更快
                //    timeout >>>= 2;

                cacheMap.put(key, value);
                // TODO 设置过期时间
                return true;
            }
        };

        final ReadSourceDataInterface<String, String> rsdi = new ReadSourceDataInterface<String, String>() {
            private Random random = new Random(System.currentTimeMillis());

            @Override
            public String readSourceData(String key) throws Exception {
                System.out.println("read data of key["+key+"] from source");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return String.valueOf(random.nextDouble());
            }
        };
        
        final CacheManager<String, String> cm = new CacheManager<>(rwci1, null, rsdi, 2);

        
        
        
        Thread[] threads = new Thread[10];
        for (int i=0; i<threads.length; ++i) {
            Thread testThread = new Thread("testThread" + i) {
                @Override
                public void run() {
                    try {
                        String value = cm.getAndUpdateCache(keyA, true);
                        System.out.println(Thread.currentThread().getName() + ", value=" + value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            threads[i] = testThread;
        }

        for (int i=0; i<threads.length; ++i) {
            threads[i].start();
        }

        for (int i=0; i<threads.length; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        
        
        cm.stopAllTask();
    }
    /**
     * 测试 二级缓存
     * **/
    public static void testCache2() {
        final Map<String, String> cacheMap = new HashMap<>();
        final String keyA = "key_a";
        //cacheMap.put(keyA, "testValue2");

        final ReadWriteCacheInterface<String, String> rwci1 = new ReadWriteCacheInterface<String, String>() {
            @Override
            public String read(String key) throws NoCacheException,Exception {
                if (!cacheMap.containsKey(key))
                    throw new NoCacheException();
                System.out.println("rwci1, read data of key["+key+"] from cache");
                return cacheMap.get(key);
            }

            @Override
            public boolean write(String key, String value) throws Exception {
                System.out.println("rwci1, write cache key="+key + ", value="+value);

                //int timeout = 100;
                //if (null == value)  // 空值需要过期更快
                //    timeout >>>= 2;

                cacheMap.put(key, value);
                // TODO 设置过期时间
                return true;
            }
        };

        final ReadWriteCacheInterface<String, String> rwci2 = new ReadWriteCacheInterface<String, String>() {
            @Override
            public String read(String key) throws NoCacheException,Exception {
                System.out.println("rwci2, read data of key["+key+"] from cache");
                return "test2";
            }

            @Override
            public boolean write(String key, String value) throws Exception {
                cacheMap.put(key, value);
                return true;
            }
        };

        final ReadSourceDataInterface<String, String> rsdi = new ReadSourceDataInterface<String, String>() {
            private Random random = new Random(System.currentTimeMillis());

            @Override
            public String readSourceData(String key) throws Exception {
                System.out.println("read data of key["+key+"] from source");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return String.valueOf(random.nextDouble());
            }
        };

        final CacheManager<String, String> cm = new CacheManager<>(rwci1, rwci2, rsdi, 2);




        Thread[] threads = new Thread[10];
        for (int i=0; i<threads.length; ++i) {
            Thread testThread = new Thread("testThread" + i) {
                @Override
                public void run() {
                    try {
                        String value = cm.getAndUpdateCache(keyA, true);
                        System.out.println(Thread.currentThread().getName() + ", value=" + value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            threads[i] = testThread;
        }

        for (int i=0; i<threads.length; ++i) {
            threads[i].start();
        }

        for (int i=0; i<threads.length; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



        cm.stopAllTask();
    }

    public static void main(String[] argv) {
        testCache1();   // 测试 一级缓存
    }
}
