package com.crocs.oms.common.util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.yantra.ycp.japi.util.YCPEncrypter;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;

/**
 * EOMS - 2596 This class implements the encrypt and decrypt methods of the
 * YCPEncrypter. These methods are used to encrypt and decrypt the customer
 * related attributes for Forter.
 */
public class CrocsPropertyEncrypterImpl implements YCPEncrypter {
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsPropertyEncrypterImpl.class);
	private static final String AES_ALGORITHM =YFSSystem.getProperty(CrocsConstant.AES); 
	private static final String AES_TRANSFORMATION = YFSSystem.getProperty(CrocsConstant.A_AES_TRANSFORMATION); 

	static String keySize = YFSSystem.getProperty(CrocsConstant.AES_KEY_SIZE);
	static String ivSize = YFSSystem.getProperty(CrocsConstant.IV_SIZE);
	private static final int AES_KEY_SIZE = Integer.parseInt(keySize);
	private static final int IV_SIZE = Integer.parseInt(ivSize); 
	private final SecretKey secretKey;
	
	// Replace your generateKey() and constructor with this:
		private static final String ENCRYPTION_KEY = YFSSystem.getProperty(CrocsConstant.SECRET_KEY); 
				

	public CrocsPropertyEncrypterImpl() throws NoSuchAlgorithmException {
		this.secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
	}


	// This method is used to encrypt the customer fields in OMS
	public String encrypt(String data) throws Exception {
		
		logger.verbose("PropertyEncrypterImpl : encrypt");
		
		byte[] iv = new byte[IV_SIZE];
		new SecureRandom().nextBytes(iv);
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
		byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

		// Combine IV + encrypted data
		byte[] combined = new byte[iv.length + encrypted.length];
		System.arraycopy(iv, 0, combined, 0, iv.length);
		System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

		return Base64.getEncoder().encodeToString(combined);
	}

	// This method is used to decrypt the customer fields in OMS
	public String decrypt(String encryptedData) throws Exception {
		
		logger.verbose("PropertyEncrypterImpl : decrypt" );
		
		byte[] combined = Base64.getDecoder().decode(encryptedData);

		byte[] iv = new byte[IV_SIZE];
		byte[] encryptedBytes = new byte[combined.length - IV_SIZE];

		System.arraycopy(combined, 0, iv, 0, IV_SIZE);
		System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

		Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

		byte[] decrypted = cipher.doFinal(encryptedBytes);
		return new String(decrypted, StandardCharsets.UTF_8);
	}
}
