package com.crocs.oms.order;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.ibm.sterling.afc.jsonutil.PLTJSONUtils;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.core.YFSSystem;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Base64;

/**
 * EOMS-2596 This class is responsible for decrypt the ExtnForterStorage
 * attribute from SFCC. It also includes an encrypt method, intended for
 * developers to test the encryption and decrypt of ExtnForterStorage.
 */

public class CrocsDecryption {

	private static final String SECRET_KEY = YFSSystem.getProperty(CrocsConstant.A_CROCS_DECRYPT_KEY);

	// The salt is used for key generation. In OMS, we do not generate the key; the
	// salt is stored for future reference.
	private static final String SALT = YFSSystem.getProperty(CrocsConstant.VAL_FORTER_SALT);

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsDecryption.class);

	
	public static String decrypt(String encryptedData, SecretKey secretKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		@SuppressWarnings("unused")
		// Decode Base64 data
		byte[] encryptedText = Base64.getDecoder().decode(encryptedData);

		// Extract IV (first 16 bytes)
		byte[] iv = Base64.getDecoder().decode(SALT);

		// Extract the actual encrypted content
		byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		// Initialize Cipher
		// As per EOMS - 875 , This padding is used in SFCC , the same padding to
		// decrypt in OMS
		Cipher cipher = Cipher.getInstance(YFSSystem.getProperty(CrocsConstant.A_AES_TRANSFORMATION));
		cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

		// Perform decryption
		byte[] decryptedData = cipher.doFinal(encryptedBytes);

		return new String(decryptedData, StandardCharsets.UTF_8);

	}

	// Method to encrypt data using AES
	// As per EOMS - 875 , This padding is used in SFCC , the same padding to
	// encrypt in OMS
	public static String encrypt(String plainText, String secretKey)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		byte[] keyBytes = secretKey.getBytes(CrocsConstant.UTF_8);
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, YFSSystem.getProperty(CrocsConstant.AES));

		byte[] iv = new byte[16];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(iv);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		Cipher cipher = Cipher.getInstance(YFSSystem.getProperty(CrocsConstant.A_AES_TRANSFORMATION));
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
		byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(CrocsConstant.UTF_8));

		byte[] combined = new byte[16 + encryptedBytes.length];
		System.arraycopy(iv, 0, combined, 0, 16);
		System.arraycopy(encryptedBytes, 0, combined, 16, encryptedBytes.length);

		return Base64.getEncoder().encodeToString(combined);
	}
	
	public static String encryptGiftCard(String plainText, SecretKey secretKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

		logger.verbose("plainText to be Encrypted : " );
		
		byte[] encodedBytes = Base64.getEncoder().encode(plainText.getBytes(StandardCharsets.UTF_8));

		byte[] iv = Base64.getDecoder().decode(SALT); // This is salt
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		Cipher cipher = Cipher.getInstance(YFSSystem.getProperty(CrocsConstant.A_AES_TRANSFORMATION));
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
		byte[] encryptedBytes = cipher.doFinal(encodedBytes);

		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	public static JSONObject parseBase64ToJson(String base64String) {
		if (base64String == null || base64String.isEmpty()) {
			return new JSONObject(); // Return an empty JSON object for an empty input
		}

		try {
			byte[] decodedBytes = Base64.getDecoder().decode(base64String);
			String decodedString = new String(decodedBytes);

			if (decodedString.isEmpty()) {
				return new JSONObject(); // Return an empty JSON object if the decoded string is empty
			}

			return new JSONObject(decodedString);
		} catch (IllegalArgumentException e) {
			// Handle invalid Base64 string
			logger.error("Invalid Base64 string: " + e.getMessage());
			return new JSONObject(); // Return an empty JSON object or handle appropriately
		} catch (JSONException e) {
			// Handle invalid JSON string
			logger.error("Invalid JSON string: " + e.getMessage());
			return new JSONObject(); // Return an empty JSON object or handle appropriately
		}
	}

	public static String decode(String encodedString) {
		byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
		return new String(decodedBytes);
	}

	public static Document convertJSONtoXML(String obj, String strRootElementName) throws JSONException {
		String response = obj.replace(":null", ":''");
		response = response.replaceAll("\\s", "");
		JSONObject objResponseMessage = new JSONObject(response);
		Document outDoc = PLTJSONUtils.getXmlFromJSON(objResponseMessage.toString(), strRootElementName);
		return outDoc;
	}

	public Document processEncryptedText(String encryptedText)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

		Document docForter = null;

		try {

			// Decode the key from base64
			byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);

			// Create a SecretKey object from the byte array
			SecretKey secretKey = new SecretKeySpec(keyBytes, YFSSystem.getProperty(CrocsConstant.AES));

			// Decrypt the text
			String decryptedCode = decrypt(encryptedText, secretKey);
		
			JSONObject validJsonObject = parseBase64ToJson(decryptedCode); // sma propertoes

			docForter = convertJSONtoXML(validJsonObject.toString(), null);


		} catch (Exception e) {
			logger.verbose(" CrocsDecryption : processEncryptedText " + e.getMessage());

		}

		return docForter;

	}

	
	
	public String getDecryptedDataForGivex(String encryptedText)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

		String decodedVal = null;

		try {

			// Decode the key from base64
			byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);

			// Create a SecretKey object from the byte array
			SecretKey secretKey = new SecretKeySpec(keyBytes, YFSSystem.getProperty(CrocsConstant.AES));

			// Decrypt the text
			String decryptedCode = decrypt(encryptedText, secretKey);
			
			String deCode = decode(decryptedCode);
			decodedVal = deCode;

		} catch (Exception e) {
			logger.verbose(" CrocsDecryption : processEncryptedText " + e.getMessage());
			throw new YFCException(e.getMessage());
		}

		return decodedVal;

	}
	
	public String getEncryptedDataForGivex(String encryptedText)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

		String decodedVal = null;

		try {
			
			// Decode the key from base64
			byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);

			// Create a SecretKey object from the byte array
			SecretKey secretKey = new SecretKeySpec(keyBytes, YFSSystem.getProperty(CrocsConstant.AES));

			// Decrypt the text
			String encryptedCode = encryptGiftCard(encryptedText, secretKey);
			
			decodedVal = encryptedCode;

		} catch (Exception e) {
			logger.verbose(" CrocsDecryption : processEncryptedText " + e.getMessage());
			throw new YFCException(e.getMessage());
		}

		return decodedVal;

	}
}