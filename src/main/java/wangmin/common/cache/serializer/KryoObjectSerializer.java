package wangmin.common.cache.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Created by wangmin on 2017/3/22
 */
public class KryoObjectSerializer<T> implements ObjectSerializer<T> {

    private static Kryo kryo = null;

    static {
        kryo = new Kryo();
    }


    @Override
    public byte[] serialize(T transaction) {
        Output output = new Output(256, -1);
        kryo.writeClassAndObject(output, transaction);
        return output.toBytes();
    }

    @Override
    public T deserialize(byte[] bytes) {
        Input input = new Input(bytes);
        return (T) kryo.readClassAndObject(input);
    }
}
