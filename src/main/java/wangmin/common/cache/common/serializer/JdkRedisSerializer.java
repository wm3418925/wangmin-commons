package wangmin.common.cache.common.serializer;

import java.io.*;

/**
 * Created by wangmin on 2017/3/22
 */
public class JdkRedisSerializer implements RedisSerializer {
    @Override
    public byte[] serializeObjectAndClass(Object object) {
        if (object == null) {
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

            try {
                ObjectOutputStream ex = new ObjectOutputStream(baos);
                ex.writeObject(object);
                ex.flush();
            } catch (IOException var3) {
                throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), var3);
            }

            return baos.toByteArray();
        }
    }
    @Override
    public <T> T deserializeObject(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        } else {
            try {
                ObjectInputStream ex = new ObjectInputStream(new ByteArrayInputStream(byteArray));
                return (T) ex.readObject();
            } catch (IOException var2) {
                throw new IllegalArgumentException("Failed to deserialize object", var2);
            } catch (ClassNotFoundException var3) {
                throw new IllegalStateException("Failed to deserialize object type", var3);
            }
        }
    }

    @Override
    public byte[] serializeObject(Object object) {
        return serializeObjectAndClass(object);
    }
    @Override
    public <T> T deserializeObjectByClass(byte[] byteArray, Class<T> clazz) {
        return deserializeObject(byteArray);
    }
}
