/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	AESCore.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.lib;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCore {

	private static String aesCipherInstanceParameter = "AES/CBC/PKCS5Padding";
	public static int DEFAULT_HASH_ITERATION_COUNT = 16384;

	/**
	 * Generates a SHA-256 hash
	 * @param key the string (or the password) you want to hash
	 * @return the SHA-256 hash or {@code null} if something went wrong
	 */
	public static byte[] getSHA256Hash(String key){
		try {
			byte[] bytekey = key.getBytes("UTF-8");
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			return sha.digest(bytekey);
		}
		catch (NoSuchAlgorithmException e) {
			System.err.println("Encoding ERROR (getSHA256Hash()): " + e.getMessage());
			return null;
		}
		catch (UnsupportedEncodingException e) {
			System.err.println("Encoding ERROR (getSHA256Hash()): " + e.getMessage());
			return null;
		}
	}

	/**
	 * Generates a MD5 hash
	 * @param key the string (or the password) you want to hash
	 * @return the MD5 hash or {@code null} if something went wrong
	 */
	public static byte[] getMD5Hash(String key) {
		try {
			byte[] bytekey = key.getBytes("UTF-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return md5.digest(bytekey);
		}
		catch (NoSuchAlgorithmException e) {
			System.err.println("Encoding ERROR (getMD5Hash()): " + e.getMessage());
			return null;
		}
		catch (UnsupportedEncodingException e) {
			System.err.println("Encoding ERROR (getMD5Hash()): " + e.getMessage());
			return null;
		}
	}

	/**
	 * Generates a PBKDF2 hash
	 * @param key the string (or the password) you want to hash
	 * @param salt the salt for this hash
	 * @return the PBKDF2 hash or {@code null} if something went wrong
	 */
	public static byte[] getPBKDF2Hash(String key, byte[] salt) {
		try	{
			return getPBKDF2Hash(key.toCharArray(), salt, 256, 16384);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Encoding ERROR (getPBKDF2Hash()): " + e.getMessage());
			return null;
		} catch (InvalidKeySpecException e) {
			System.err.println("Encoding ERROR (getPBKDF2Hash()): " + e.getMessage());
			return null;
		}
	}

	/**
	 * Generates a PBKDF2 hash
	 * @param key the string (or the password) you want to hash
	 * @param salt the salt for this hash
	 * @param keySizeInBit the size of the hash in bit
	 * @return the PBKDF2 hash
	 * @throws NoSuchAlgorithmException if PBKDF2 cannot be used on this system
	 * @throws InvalidKeySpecException if the key is invalid
	 */
	public static byte[] getPBKDF2Hash(char[] key, byte[] salt, int keySizeInBit) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return getPBKDF2Hash(key, salt, keySizeInBit, DEFAULT_HASH_ITERATION_COUNT);
	}

	/**
	 * Generates a PBKDF2 hash
	 * @param key the string (or the password) you want to hash
	 * @param salt the salt for this hash
	 * @param keySizeInBit the size of the hash in bit
	 * @param hashIterations the number of iterations
	 * @return the PBKDF2 hash
	 * @throws NoSuchAlgorithmException if PBKDF2 cannot be used on this system
	 * @throws InvalidKeySpecException if the key is invalid
	 */
	public static byte[] getPBKDF2Hash(char[] key, byte[] salt, int keySizeInBit, int hashIterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec ks = new PBEKeySpec(key, salt, hashIterations, keySizeInBit);
			SecretKey s = f.generateSecret(ks);
			return s.getEncoded();
	}

	/**
	 * Computes an password salt
	 * @param saltbytesize Size of the password salt in bytes
	 * @return the password salt as byte array
	 */
	public static byte[] generatePasswordSalt(int saltbytesize) {
		SecureRandom r = new SecureRandom();
		byte[] salt = new byte[saltbytesize];
		r.nextBytes(salt);
		return salt;
	}

	/**
	 * Generates an Initial-Vector (IV) for the AES-Encryption
	 * @return the generated IV
	 */
	public static byte[] generateIV() {
		Random r = new Random();
		byte[] iv = new byte[16];
		r.nextBytes(iv);
		return iv;
	}

	/**
	 * Converts a byte array to a BASE64-String
	 * @param byteArr the byte array
	 * @return the equivalent BASE64-String
	 */
	public static String bytesToBase64String(byte[] byteArr) {
		return javax.xml.bind.DatatypeConverter.printBase64Binary(byteArr);
	}

	/**
	 * Converts a BASE64-String to a byte array
	 * @param b64str the BASE64-String
	 * @return the equivalent byte array
	 */
	public static byte[] bytesFromBase64String(String b64str) {
		return javax.xml.bind.DatatypeConverter.parseBase64Binary(b64str);
	}

	/**
	 * Returns {@code true} if AES-256 is available, {@code false} if there will be a fall back to AES-128 because of the US export policy jar file
	 * @return Boolean if AES-256 is supported on this system
	 * @throws NoSuchAlgorithmException if the system (e.g. java cipher) does not support even AES-128 on this system
	 */
	public static boolean isAES256EncryptionAvailable() throws NoSuchAlgorithmException {
			return (Cipher.getMaxAllowedKeyLength(aesCipherInstanceParameter) == Integer.MAX_VALUE);
	}

	/**
	 * This Method is similar to "isAES256EncryptionAvailable()" but never throws a "NoSuchAlgorithmException".
	 * @return Boolean if AES-256 is supported on this system, if the java cipher class even not supports AES-128 the return value will be false too.
	 */
	public static boolean isAES256Supported() {
		try {
			return (Cipher.getMaxAllowedKeyLength(aesCipherInstanceParameter) == Integer.MAX_VALUE);
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}

	/**
	 * Encrypts a string using AES
	 * @param text the text you want to encrypt
	 * @param key the key you want to use
	 * @param iv the initial vector for the encryption
	 * @return the encrypted text (BASE64 encoded)
	 * @throws InvalidKeySpecException if the key is invalid
	 */
	public static String encryptAES(String text, byte[] key, byte[] iv) throws InvalidKeySpecException {
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

			// encrypt
			Cipher cipher = Cipher.getInstance(aesCipherInstanceParameter);

			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

			byte[] encrypted = cipher.doFinal(text.getBytes("UTF8"));

			// convert the bytes to a BASE64-String
			return javax.xml.bind.DatatypeConverter.printBase64Binary(encrypted);
		}
		catch (Exception ex) {
			throw new InvalidKeySpecException(ex.getMessage());
		}
	}

	/**
	 * Decrypts a string using AES
	 * @param text the text you want to decrypt (the text has to be BASE64 encoded)
	 * @param key the key you want to use
	 * @param iv the initial vector for the encryption
	 * @return the decrypted text
	 * @throws InvalidKeyException if the key is wrong or invalid
	 */
	public static String decryptAES(String text, byte[] key, byte[] iv) throws InvalidKeyException {
		try{
			 SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
			// Convert BASE64-String to byte array
			byte[] crypted = javax.xml.bind.DatatypeConverter.parseBase64Binary(text);

			// Decrypt...
			Cipher cipher = Cipher.getInstance(aesCipherInstanceParameter);

			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

			byte[] cipherData = cipher.doFinal(crypted);

			return new String(cipherData, "UTF8");
		}
		catch(Exception ex){
			throw new InvalidKeyException(ex.getMessage());
		}
	}
}
