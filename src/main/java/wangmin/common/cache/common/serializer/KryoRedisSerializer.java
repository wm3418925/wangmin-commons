package wangmin.common.cache.common.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Created by wangmin on 2017/3/22
 */
public class KryoRedisSerializer implements RedisSerializer {
    private static Kryo kryo = null;

    static {
        kryo = new Kryo();
    }

    @Override
    public byte[] serializeObjectAndClass(Object object) {
        if (null == object)
            return null;

        Output output = new Output(256, -1);
        kryo.writeClassAndObject(output, object);
        return output.toBytes();
    }
    @Override
    public <T> T deserializeObject(byte[] byteArray) {
        if (null == byteArray)
            return null;

        Input input = new Input(byteArray);
        return (T) kryo.readClassAndObject(input);
    }


    @Override
    public byte[] serializeObject(Object object) {
        if (null == object)
            return null;

        Output output = new Output(256, -1);
        kryo.writeObject(output, object);
        return output.toBytes();
    }
    @Override
    public <T> T deserializeObjectByClass(byte[] byteArray, Class<T> clazz) {
        if (null == byteArray)
            return null;

        Input input = new Input(byteArray);
        return kryo.readObject(input, clazz);
    }

}
