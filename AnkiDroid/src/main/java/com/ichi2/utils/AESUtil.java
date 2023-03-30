package com.ichi2.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    // 共通鍵
//    private static final String ENCRYPTION_KEY = "1234567890qwertyuioplkjhgfdsazxc";
    private static final String ENCRYPTION_IV = "";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";


    public static String encrypt(String src, String key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, makeKey(key), makeIv());
            return Base64.encodeBytes(cipher.doFinal(src.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String decrypt(String src, String key) {
//        String decrypted = "";
//        try {
//            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
//            cipher.init(Cipher.DECRYPT_MODE, makeKey(key), makeIv());
//            decrypted = new String(cipher.doFinal(Base64.decode(src)));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        String decrypted = "";
        try {
            decrypted = new String(decrypt(Base64.decode(src), key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypted;
    }


    public static byte[] decrypt(byte[] data, String key) throws Exception {
        //实例化
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        //使用密钥初始化，设置为解密模式
        cipher.init(Cipher.DECRYPT_MODE, makeKey(key), makeIv());
        //执行操作
        return cipher.doFinal(data);
    }


    public static void encryptionFile(String src, String dest) {
        int len = 0;
        byte[] buffer = new byte[1024];
        byte[] cipherbuffer;
        // 使用会话密钥对文件加密。
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, makeKey("aj1jcaoqjv6fydhei8ajvkfie8gu41o9"), makeIv());

            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dest);

            // 读取原文，加密并写密文到输出文件。
            while ((len = fis.read(buffer)) != -1) {
                cipherbuffer = cipher.update(buffer, 0, len);
                fos.write(cipherbuffer);
                fos.flush();
            }
            cipherbuffer = cipher.doFinal();
            fos.write(cipherbuffer);
            fos.flush();

            fis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void decryptionFile(String src, String dest) {
        int len = 0;
        byte[] buffer = new byte[5 * 1024];
        byte[] plainbuffer;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, makeKey("aj1jcaoqjv6fydhei8ajvkfie8gu41o9"), makeIv());

            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dest);

            while ((len = fis.read(buffer)) != -1) {
                plainbuffer = cipher.update(buffer, 0, len);
                fos.write(plainbuffer);
                fos.flush();
            }

            plainbuffer = cipher.doFinal();
            fos.write(plainbuffer);
            fos.flush();

            fis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static AlgorithmParameterSpec makeIv() {
        return new IvParameterSpec(ENCRYPTION_IV.getBytes(StandardCharsets.UTF_8));
    }


    static Key makeKey(String encryptionKey) {
        return new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    }
}
