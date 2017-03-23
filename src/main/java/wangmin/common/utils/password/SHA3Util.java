package wangmin.common.utils.password;

import java.io.UnsupportedEncodingException;

import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-22
 * <p>Version: 1.0
 * 封装 SHA3 编码
 * */
public abstract class SHA3Util {

    private static Size DEFAULT_SIZE = Size.S224;
    
    public static String digest(String string) {
        return digest(string, DEFAULT_SIZE);
    }
    
    public static String digest(String string, Size s) {
        return digest(string, s, ResultByteEncode.hex);
    }
    
    public static String digest(String string, Size s, ResultByteEncode resultByteEncode) {
        Size size = s == null ? DEFAULT_SIZE : s;
        
        DigestSHA3 md = new DigestSHA3(size.getValue());
        String text = string != null ? string : "null";
        try {
            md.update(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            // most unlikely
            md.update(text.getBytes());
        }
        byte[] digest = md.digest();
        return transferBytesToString(digest, resultByteEncode);
    }
    
    protected static String transferBytesToString(byte [] bytes, ResultByteEncode resultByteEncode) {
        if (ResultByteEncode.hex == resultByteEncode)
            return Hex.toHexString(bytes);
        else
            return new String(Base64.encode(bytes));
    }
    
    protected enum Size {
        S224(224),
        S256(256),
        S384(384),
        S512(512);
        
        int bits = 0;
        
        Size(int bits) {
            this.bits = bits;
        }
        
        public int getValue() {
            return this.bits;
        }
    }

    protected enum ResultByteEncode {
        hex,
        base64
    }


    public static String userPasswordHash(String org, String salt) {
        return SHA3Util.digest(org + salt, SHA3Util.Size.S384, SHA3Util.ResultByteEncode.base64);
    }


    public static void main(String[] argvs) {
        //lo66AFfLtFj1IHg33pT3qJsfdGrW6w8SaR2QjWPMpqudXIRTCMCfYw0KK1yFhOrj
        System.out.println(userPasswordHash("admin", "2dsd"));
    }
}
