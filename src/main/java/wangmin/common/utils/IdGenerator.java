package wangmin.common.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;

/**
 * Created by wm on 2017/3/15.
 */
public final class IdGenerator implements ApplicationListener<ApplicationEvent> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IdGenerator.class);

    private static volatile long providerOrder;
    private static volatile long lastGenerateTime;
    private static volatile int millisecondSeralNumber;
    private static volatile boolean isInited = false;
    /**
     * 生成id
     * **/
    public static long generateId() {
        if (!isInited)
            throw new RuntimeException("id generator not inited");

        long ctm = System.currentTimeMillis();
        synchronized (IdGenerator.class) {
            if (lastGenerateTime != ctm) {
                millisecondSeralNumber = 0;
                lastGenerateTime = ctm;
            }

            long id = (providerOrder << (41 + 8)) | (ctm << 8) | millisecondSeralNumber;

            ++ millisecondSeralNumber;

            return id;
        }
    }


    private String zkConnStr;
    public void setZkConnStr(String zkConnStr) {
        this.zkConnStr = zkConnStr;
    }

    private String zkCreateOrderPath;
    public void setZkCreateOrderPath(String zkCreateOrderPath) {
        this.zkCreateOrderPath = zkCreateOrderPath;
    }


    private static String zkNodeData = UUIDGenerator.generateUUID();
    private static String createPath;
    private CuratorFramework client;


    private class MyConnectionStateListener implements ConnectionStateListener {
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if (newState == ConnectionState.LOST) {
                isInited = false;
            } else if (newState == ConnectionState.RECONNECTED) {
                isInited = false;
                if (client != null && createPath != null) {
                    try {
                        String value = client.getData().forPath(createPath).toString();
                        if (zkNodeData.equals(value)) {
                            isInited = true;
                        }
                    } catch (Exception e) {
                        LOGGER.info("get data of " + createPath + " failed", e);
                    }
                }

                if (!isInited) {
                    client.close();
                    generateProviderOrder();
                }
            }
        }
    }

    // 生成服务提供者的编号
    private void generateProviderOrder() {
        try {
            if (client != null) {
                client.close(); // 关闭连接后创建的临时节点将自动删除
                client = null;
            }
            client = CuratorFrameworkFactory.builder()
                    .connectString(zkConnStr).namespace("sunriseId")
                    .retryPolicy(new RetryNTimes(20, 3000))
                    .sessionTimeoutMs(5000)
                    .maxCloseWaitMs(5000)
                    .connectionTimeoutMs(5000)
                    .build();
            ConnectionStateListener connectionStateListener = new MyConnectionStateListener();
            client.getConnectionStateListenable().addListener(connectionStateListener);
            client.start();
            client.blockUntilConnected();

            // 创建根目录
            try {
                client.create().forPath(zkCreateOrderPath, new byte[0]);
            } catch (Exception e) {}

            // 创建临时有序节点
            String childPathPrefix = zkCreateOrderPath + "/id";
            createPath = client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(childPathPrefix, zkNodeData.getBytes());
            LOGGER.info("generateProviderOrder, createPath = {}", createPath);

            // 获取序号
            String orderStr = createPath.substring(childPathPrefix.length());
            providerOrder = Integer.parseInt(orderStr);
            isInited = true;
            LOGGER.info("generateProviderOrder success, providerOrder = {}", providerOrder);
        } catch (Exception e) {
            LOGGER.error("generateProviderOrder failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onApplicationEvent(ApplicationEvent ev) {
        if (ev instanceof ContextRefreshedEvent) {
            generateProviderOrder();
        } else if (ev instanceof ContextStoppedEvent) {
            if (client != null) {
                client.close(); // 关闭连接后创建的临时节点将自动删除
                client = null;
            }
            isInited = false;
        }
    }

}
