package wangmin.common.utils.password;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-22
 * <p>Version: 1.0
 * 封装 RSA 加密
 * 需要认证jdk
 * */
public abstract class RSAUtil {
	private static final Logger logger = LoggerFactory.getLogger(RSAUtil.class);
	
	private static String RSAKeyStore;
	private static KeyPair keyPair;
	static {
		RSAKeyStore = RSAUtil.class.getResource("/").getPath();
		RSAKeyStore = RSAKeyStore.substring(0,  RSAKeyStore.length()-1);
		int li = RSAKeyStore.lastIndexOf('/');
		if (li >= 0)
			RSAKeyStore = RSAKeyStore.substring(0, li+1);
		
		RSAKeyStore += "rsa_key.txt";
		
		try {
			keyPair = loadOrGenerateKeyPair();
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * * 从原有文件加载或者生成新的
	 * 
	 * @return KeyPair *
	 * @throws Exception
	 */
	private static KeyPair loadOrGenerateKeyPair() throws Exception {
		KeyPair keyPair = loadKeyPair();
		if (keyPair != null)
			return keyPair;
		
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA",
					new org.bouncycastle.jce.provider.BouncyCastleProvider());
			final int KEY_SIZE = 1024;// 没什么好说的了，这个值关系到块加密的大小，可以更改，但是不要太大，否则效率会低
			keyPairGen.initialize(KEY_SIZE, new SecureRandom());
			keyPair = keyPairGen.generateKeyPair();

			saveKeyPair(keyPair);
			return keyPair;
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
	}
	
	public static RSAPublicKey getPublicKey() {
		return (RSAPublicKey) keyPair.getPublic();
	}
	
	/**
	 * * 生成新的
	 * 
	 * @return KeyPair *
	 * @throws Exception
	 */
	/*private static KeyPair generateKeyPair() throws Exception {
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA",
					new org.bouncycastle.jce.provider.BouncyCastleProvider());
			final int KEY_SIZE = 1024;// 没什么好说的了，这个值关系到块加密的大小，可以更改，但是不要太大，否则效率会低
			keyPairGen.initialize(KEY_SIZE, new SecureRandom());
			KeyPair keyPair = keyPairGen.generateKeyPair();
			// System.out.println(keyPair.getPrivate());
			// System.out.println(keyPair.getPublic());
			saveKeyPair(keyPair);
			return keyPair;
		} catch (Exception e) {
			throw e;
		}
	}*/

	
	
	private static KeyPair loadKeyPair() throws Exception {
		try {
			FileInputStream fis = new FileInputStream(RSAKeyStore);
			ObjectInputStream oos = new ObjectInputStream(fis);
			KeyPair kp = (KeyPair) oos.readObject();
			oos.close();
			fis.close();
			return kp;
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	private static void saveKeyPair(KeyPair kp) throws Exception {
		FileOutputStream fos = new FileOutputStream(RSAKeyStore);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(kp);
		oos.close();
		fos.close();
	}
	 

	/**
	 * * 生成公钥 *
	 * 
	 * @param modulus
	 *            *
	 * @param publicExponent
	 *            *
	 * @return RSAPublicKey *
	 * @throws Exception
	 */
	/*public static RSAPublicKey generateRSAPublicKey(byte[] modulus,
			byte[] publicExponent) throws Exception {
		KeyFactory keyFac = null;
		try {
			keyFac = KeyFactory.getInstance("RSA",
					new org.bouncycastle.jce.provider.BouncyCastleProvider());
		} catch (NoSuchAlgorithmException ex) {
			throw new Exception(ex.getMessage());
		}

		RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(new BigInteger(
				modulus), new BigInteger(publicExponent));
		try {
			return (RSAPublicKey) keyFac.generatePublic(pubKeySpec);
		} catch (InvalidKeySpecException ex) {
			throw e;
		}
	}*/

	/**
	 * * 生成私钥 *
	 * 
	 * @param modulus
	 *            *
	 * @param privateExponent
	 *            *
	 * @return RSAPrivateKey *
	 * @throws Exception
	 */
	/*public static RSAPrivateKey generateRSAPrivateKey(byte[] modulus,
			byte[] privateExponent) throws Exception {
		KeyFactory keyFac = null;
		try {
			keyFac = KeyFactory.getInstance("RSA",
					new org.bouncycastle.jce.provider.BouncyCastleProvider());
		} catch (NoSuchAlgorithmException ex) {
			throw new Exception(ex.getMessage());
		}

		RSAPrivateKeySpec priKeySpec = new RSAPrivateKeySpec(new BigInteger(
				modulus), new BigInteger(privateExponent));
		try {
			return (RSAPrivateKey) keyFac.generatePrivate(priKeySpec);
		} catch (InvalidKeySpecException ex) {
			throw e;
		}
	}*/

	/**
	 * * 加密 *
	 *
	 * @param data
	 *            待加密的明文数据 *
	 * @return 加密后的数据 *
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] data) throws Exception {
		try {
			PublicKey pk = keyPair.getPublic();
			
			Cipher cipher = Cipher.getInstance("RSA", new org.bouncycastle.jce.provider.BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, pk);
			int blockSize = cipher.getBlockSize();// 获得加密块大小，如：加密前数据为128个byte，而key_size=1024
			// 加密块大小为127
			// byte,加密后为128个byte;因此共有2个加密块，第一个127
			// byte第二个为1个byte
			int outputSize = cipher.getOutputSize(data.length);// 获得加密块加密后块大小
			int leavedSize = data.length % blockSize;
			int blocksSize = leavedSize != 0 ? data.length / blockSize + 1
					: data.length / blockSize;
			byte[] raw = new byte[outputSize * blocksSize];
			int i = 0;
			while (data.length - i * blockSize > 0) {
				if (data.length - i * blockSize > blockSize)
					cipher.doFinal(data, i * blockSize, blockSize, raw, i * outputSize);
				else
					cipher.doFinal(data, i * blockSize, data.length - i * blockSize, raw, i * outputSize);

				i++;
			}
			return raw;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * * 解密 *
	 *
	 * @param raw
	 *            已经加密的数据 *
	 * @return 解密后的明文 *
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] raw) throws Exception {
		try {
			PrivateKey pk = keyPair.getPrivate();
			
			Cipher cipher = Cipher.getInstance("RSA", new org.bouncycastle.jce.provider.BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, pk);
			int blockSize = cipher.getBlockSize();
			ByteArrayOutputStream bout = new ByteArrayOutputStream(64);
			int j = 0;

			while (raw.length - j * blockSize > 0) {
				bout.write(cipher.doFinal(raw, j * blockSize, blockSize));
				j++;
			}
			return bout.toByteArray();
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static String decryptStr(String token) throws Exception {
		byte[] en_result = Hex.decode(token);
		
		byte[] bs = RSAUtil.decrypt(en_result);
		String de_orig = new String(bs, "UTF-8");
		StringBuffer sb = new StringBuffer();
		sb.append(de_orig);
		return sb.reverse().toString();
	}

	
	public static void main(String[] args) throws Exception {
		String test = "hello world";
		byte[] en_test = encrypt(test.getBytes());
		byte[] de_test = decrypt(en_test);
		System.out.println(new String(de_test));
	}
}
