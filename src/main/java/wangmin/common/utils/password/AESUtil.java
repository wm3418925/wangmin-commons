package wangmin.common.utils.password;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/*******************************************************************************
 * AES加解密算法
 * 
 * @author wangmin
 * 
 * 
 *         加密用的Key 可以用26个字母和数字组成，最好不要用保留字符，虽然不会错，至于怎么裁决，个人看情况而定
 *         此处使用AES-128-CBC加密模式，key需要为16位。 也是使用0102030405060708
 */
public abstract class AESUtil {
	// 加密
	public static String encrypt(String sSrc, String sKey, String ivStr) throws Exception {
		if (sKey == null) {
			System.out.print("Key为空null");
			return null;
		}
		// 判断Key是否为16位
		if (sKey.length() != 16) {
			System.out.print("Key长度不是16位");
			return null;
		}
		byte[] raw = sKey.getBytes();
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "算法/模式/补码方式"
		IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		byte[] encrypted = cipher.doFinal(sSrc.getBytes());

		return Base64.encodeBase64String(encrypted);// 此处使用BAES64做转码功能，同时能起到2次加密的作用。
	}

	// 解密
	public static String decrypt(String sSrc, String sKey, String ivStr) throws Exception {
		try {
			// 判断Key是否正确
			if (sKey == null) {
				System.out.print("Key为空null");
				return null;
			}
			// 判断Key是否为16位
			if (sKey.length() != 16) {
				System.out.print("Key长度不是16位");
				return null;
			}
			byte[] raw = sKey.getBytes("ASCII");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] encrypted1 = Base64.decodeBase64(sSrc);// 先用bAES64解密
			try {
				byte[] original = cipher.doFinal(encrypted1);
				String originalString = new String(original);
				return originalString;
			} catch (Exception e) {
				System.out.println(e.toString());
				return null;
			}
		} catch (Exception ex) {
			System.out.println(ex.toString());
			return null;
		}
	}
	
	public static void main(String[] argvs) throws Exception {
		String s = "ntvrOMQDW2K9ZGDpG50a/d5dWdUXj5kzhX34uOR4/RPrWnZNw1gTKEK/l+5hIHFnDkG2b1YPgH0SoZrUT9wejrafx6DOQ6vJGR7xExWq9r2rf5rsVNeayCJKK13sXUMz";
		System.out.println(decrypt(s, "0123456789012345", "0123456789012345"));//dRO4e3wOq8iqVk49KtRoFTm/98Qcsnvg1FBZx3RHbmp7ABZaomIalzd0gXYWCLwWqxMuXJ906OYCfW2Xw/vCrg==
	}

}
