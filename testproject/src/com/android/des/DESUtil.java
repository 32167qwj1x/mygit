package com.android.des;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DESUtil {
	
	public static byte[] CBCEncrypt(byte[] data, byte[] key, byte[] iv) {
		try {
			// 从原始密钥数据树立DESKeySpec对象
			DESKeySpec dks = new DESKeySpec(key);

			// 树立一个密匙工厂，然后用它把DESKeySpec转换成
			// 一个SecretKey对象
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey secretKey = keyFactory.generateSecret(dks);

			// Cipher对象实践完成加密操作"
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			// 若采用NoPadding方式，data长度必需是8的倍数
			// Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
			// 用密匙原始化Cipher对象
			IvParameterSpec param = new IvParameterSpec(iv);

			cipher.init(Cipher.ENCRYPT_MODE, secretKey, param);
			// 执行加密操作

			byte encryptedData[] = cipher.doFinal(data);

			return encryptedData;
		} catch (Exception e) {

			System.err.println("DES算法，加密数据出错!");

			e.printStackTrace();
		}
		return null;

	}

	public static byte[] CBCDecrypt(byte[] data, byte[] key, byte[] iv) {
		try {
			// 从原始密匙数据树立一个DESKeySpec对象
			DESKeySpec dks = new DESKeySpec(key);
			// 树立一个密匙工厂，然后用它把DESKeySpec对象转换成一个SecretKey对象
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey secretKey = keyFactory.generateSecret(dks);
			// using DES in CBC mode
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			// 若采用NoPadding方式，data长度必需是8的倍数
			// Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
			// 用密匙原始化Cipher对象
			IvParameterSpec param = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, param);
			// 正式执行解密操作
			byte decryptedData[] = cipher.doFinal(data);
			return decryptedData;
		} catch (Exception e) {
			System.err.println("DES算法，解密出错。");
			e.printStackTrace();
		}
		return null;
	}
}