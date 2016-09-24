/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	EncryptionManager.java

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
package de.akubix.keyminder.core.encryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;

/**
 * This class is used to handle the encryption of a password file
 * @see EncryptionCipher
 */
public class EncryptionManager {
	private static Map<String, EncryptionCipher> algorithms = new HashMap<>();
	private static String defaultCipher;
	private static byte defaultSaltLengthInByte = 16;

	private char[] password;
	private EncryptionCipher cipher;

	private byte[] salt;
	private byte[] iv;

	/**
	 * Create a encryption manager
	 * Note: This constructor does not set the password. You have to call {@link #requestPasswordInputWithConfirm(ApplicationInstance, String, String, String)} after this manually.
	 * @param useDefaultCipher use {@code true} if the default cipher should be used, otherwise the encryption is disabled.
	 */
	public EncryptionManager(boolean useDefaultCipher){
		this.password = new char[0];
		this.cipher = useDefaultCipher ? algorithms.get(defaultCipher) : algorithms.get("None");
		this.iv = new byte[0];
		this.salt = new byte[0];
	}

	/**
	 * Create a encryption manager using the default encryption cipher
	 * @param key the file password
	 * @throws NoSuchAlgorithmException if there is no cipher with this name available
	 */
	public EncryptionManager(char[] key) throws NoSuchAlgorithmException{
		this(defaultCipher, key);
	}

	/**
	 * Create a encryption manager using a custom encryption cipher and an empty IV as well as an empty password salt
	 * @param cipherName the name of the encryption cipher
	 * @param key the file password
	 * @throws NoSuchAlgorithmException if there is no cipher with this name available
	 */
	public EncryptionManager(String cipherName, char[] key) throws NoSuchAlgorithmException{
		this(cipherName, key, new byte[8], new byte[8]);
	}

	/**
	 * Create a encryption manager using a custom encryption cipher and an empty password salt
	 * @param cipherName the name of the encryption cipher
	 * @param key the file password
	 * @param iv the initial vector for the encryption
	 * @throws NoSuchAlgorithmException if there is no cipher with this name available
	 */
	public EncryptionManager(String cipherName, char[] key, byte[] iv) throws NoSuchAlgorithmException{
		this(cipherName, key, iv, new byte[8]);
	}

	/**
	 * Create a encryption manager using a custom encryption cipher
	 * @param cipherName the name of the encryption cipher
	 * @param key the file password
	 * @param iv the initial vector for the encryption
	 * @param salt the password salt for the encryption
	 * @throws NoSuchAlgorithmException if there is no cipher with this name available
	 */
	public EncryptionManager(String cipherName, char[] key, byte[] iv, byte[] salt) throws NoSuchAlgorithmException{
		if(algorithms.containsKey(cipherName)){
			this.password = key;
			this.cipher = algorithms.get(cipherName);
			this.iv = iv;
			this.salt = salt;
		}
		else{
			throw new NoSuchAlgorithmException("Cipher not available: '" + cipherName + "'");
		}
	}

	/**
	 * Get the salt that was used for encryption last time, by default for each encryption a new salt will be generated
	 * Note: If the cipher does not support password salts, this method may return {@code null}
	 * @return The used salt as Byte-Array
	 */
	public byte[] getPasswordSalt(){
		return salt;
	}

	/**
	 * Get the salt that was used for encryption last time, by default for each encryption a new salt will be generated
	 * If the cipher does not support password salts, the returned data is not used for encryption
	 * @return The used salt as BASE64 String
	 */
	public String getPasswordSaltAsBase64(){
		if(salt == null){
			return "";
		}
		else{
			return AES.bytesToBase64String(salt);
		}
	}

	/**
	 * Get the IV (Initial Vector) that has been used for encryption last time, by default a new IV is generated each time you want to encrypt
	 * @return The IV that has been used last time as Byte-Array
	 */
	public byte[] getIV(){
		return iv;
	}

	/**
	 * Get the IV (Initial Vector) that has been used for encryption last time, by default a new IV is generated each time you want to encrypt
	 * @return The IV that has been used last time as BASE64-String
	 */
	public String getIVasBase64(){
		return AES.bytesToBase64String(iv);
	}

	/**
	 * Encrypt a string using the assigned EncryptionCipher and generate a new IV and a new password salt
	 * @param source The data that should be encrypted
	 * @return the encrypted data
	 * @throws InvalidKeySpecException if the key can't be used for encryption
	 */
	public String encrypt(String source) throws InvalidKeySpecException{
		return encrypt(source, true, true);
	}

	/**
	 * Encrypt a String using the assigned {@link EncryptionCipher}
	 * @param source The data that should be encrypted
	 * @param generateNewSalt use {@code true} if you want to generate a new password salt
	 * @param generateNewIV use {@code true} if you want to generate a new IV
	 * @return the encrypted data
	 * @throws InvalidKeySpecException if the key can't be used for encryption
	 */
	public String encrypt(String source, boolean generateNewSalt, boolean generateNewIV) throws InvalidKeySpecException{
		if(generateNewIV){iv = AES.generateIV();}

		if(cipher.areSaltedHashesSupported()){
			if(generateNewSalt){salt = AES.generatePasswordSalt(defaultSaltLengthInByte);}
			return cipher.encrypt(source, password, iv, salt);
		}
		else
		{
			return cipher.encrypt(source, password, iv, null);
		}
	}

	/**
	 * Decrypt a string using the assigned {@link EncryptionCipher}
	 * @param source the data that to decrypt
	 * @return The decrypted data
	 * @throws InvalidKeyException if the given password (respectively the key) is not correct
	 */
	public String decrypt(String source) throws InvalidKeyException{
		if(cipher.areSaltedHashesSupported()){
			return cipher.decrypt(source, password, iv, salt);
		}
		else{
			return cipher.decrypt(source, password, iv, null);
		}
	}

	/**
	 * Initializes an update of the password which has been assigned to the encryption manager using the {@link ApplicationInstance#requestStringInput(String, String, String)} method of the application instance.
	 * Furthermore the user has to input the password twice to avoid typing errors.
	 * @param instance the application instance
	 * @param windowTitle the window title
	 * @param labelText the label text
	 * @param labelTextConfirm the label text for confirm
	 * @return {@code true} if the password has been changed, {@code false} if not
	 * @throws UserCanceledOperationException if the user canceled the operation
	 */
	public boolean requestPasswordInputWithConfirm(ApplicationInstance instance, String windowTitle, String labelText, String labelTextConfirm) throws UserCanceledOperationException {
		char[] pw = null;
		try{
			pw = instance.requestPasswordInput(windowTitle, labelText, "");
			if(pw.length > 0){
				char[] pw_confirm = instance.requestPasswordInput(windowTitle, labelTextConfirm, "");
				if(comparePasswords(pw, pw_confirm)){
					clearArray(pw_confirm);
					clearArray(this.password); // clear the current password...
					this.password = pw; // ...and set the new one

					if(cipher.getCipherName().equals("None")){cipher = algorithms.get(defaultCipher);}
					return true;
				}
				else{
					clearArray(pw);
					clearArray(pw_confirm);
					return false;
				}
			}
			else{
				return false;
			}
		}
		catch(UserCanceledOperationException e){
			if(pw != null){clearArray(pw);}
			throw e;
		}
	}

	private boolean comparePasswords(char[] pw1, char[] pw2)
	{
		if(pw1.length != pw2.length){return false;}
		for(int i = 0; i < pw1.length; i++){
			if(pw1[i] != pw2[i]){return false;}
		}
		return true;
	}

	/**
	 * Compare a password with the file password
	 * @param pw the password that will be compared with the file password
	 * @return {@code true} if the passwords are equal, {@code false} if not
	 */
	public boolean checkPassword(char[] pw){
		return comparePasswords(this.password, pw);
	}

	/**
	 * Replace the used {@link EncryptionCipher} by another one
	 * @param cipherName the name of the new cipher
	 * @throws NoSuchAlgorithmException if there is no cipher with this name available
	 */
	public void setCipher(String cipherName) throws NoSuchAlgorithmException {
		if(algorithms.containsKey(cipherName)){
			cipher = algorithms.get(cipherName);
		}
		else{
			throw new NoSuchAlgorithmException("Cipher-Algorithm not available: '" + cipherName + "'");
		}
	}

	/**
	 * Get the cipher that is currently in use
	 * @return the currently used {@link EncryptionCipher}
	 */
	public EncryptionCipher getCipher(){
		return cipher;
	}

	/**
	 * Set the cipher to "none" and clears the password, iv and salt
	 */
	public void destroy(){
		clearArray(this.password);
		clearArray(this.iv);
		clearArray(this.salt);
		cipher = algorithms.get("None");
	}

	/* ===================================== STATIC METHODS ===================================== */

	/**
	 * Add an new cipher algorithm which can be used for any encryption
	 * @param ec the new cipher algorithm
	 */
	public static void addCipherAlgorithm(EncryptionCipher ec){
		algorithms.put(ec.getCipherName(), ec);
	}

	/**
	 * Get a list (respectively a {@link Set}) of all supported encryption ciphers
	 * @return the list of encryption ciphers
	 */
	public static Set<String> getCipherAlgorithms(){
		return algorithms.keySet();
	}

	/**
	 * This method must be called during the startup to provide some default cipher algorithms
	 */
	public static void loadDefaultCiphers(){
		if(AES.isAES256Supported()){
			defaultCipher = "AES-256/PBKDF2";
			addCipherAlgorithm(new EncryptionCipher() {
				@Override
				public String getCipherName() {
					return "AES-256/PBKDF2";
				}

				@Override
				public String encrypt(String source, char[] password, byte[] iv, byte[] salt) throws InvalidKeySpecException {
					try {
						byte[] key = AES.getPBKDF2Hash(password, salt, 256);
						String enc = AES.encryptAES(source, key, iv);
						clearArray(key);
						return enc;
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
						throw new InvalidKeySpecException("Algorithm not available: " + e.getMessage());
					} catch (InvalidKeySpecException e) {
						e.printStackTrace();
						throw new InvalidKeySpecException("The given key cannot be used for encryption: " + e.getMessage());
					}
				}

				@Override
				public String decrypt(String enc, char[] password, byte[] iv, byte[] salt) throws InvalidKeyException {
					try {
						byte[] key = AES.getPBKDF2Hash(password, salt, 256);
						String src = AES.decryptAES(enc, key, iv);
						clearArray(key);
						return src;
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
						throw new InvalidKeyException("Algorithm not available: " + e.getMessage());
					} catch (InvalidKeySpecException e) {
						throw new InvalidKeyException("Wrong password.");
					}
				}

				@Override
				public boolean areSaltedHashesSupported() {
					return true;
				}
			});


			addCipherAlgorithm(new EncryptionCipher() {

				@Override
				public String getCipherName() {
					return "AES-256/SHA-256";
				}

				@Override
				public String encrypt(String source, char[] password, byte[] iv, byte[] salt) throws InvalidKeySpecException {
					byte[] key = AES.getSHA256Hash(new String(password));
					String enc = AES.encryptAES(source, key, iv);
					clearArray(key);
					return enc;
				}

				@Override
				public String decrypt(String enc, char[] password, byte[] iv, byte[] salt) throws InvalidKeyException {
					byte[] key = AES.getSHA256Hash(new String(password));
					String src = AES.decryptAES(enc, key, iv);
					clearArray(key);
					return src;
				}

				@Override
				public boolean areSaltedHashesSupported() {
					return false;
				}
			});
		}
		else
		{
			defaultCipher = "AES-128/MD5";
		}

		addCipherAlgorithm(new EncryptionCipher() {
			@Override
			public String getCipherName() {
				return "AES-128/MD5";
			}

			@Override
			public String encrypt(String source, char[] password, byte[] iv, byte[] salt) throws InvalidKeySpecException {
				byte[] key = AES.getMD5Hash(new String(password));
				String enc = AES.encryptAES(source, key, iv);
				clearArray(key);
				return enc;
			}

			@Override
			public String decrypt(String enc, char[] password, byte[] iv, byte[] salt) throws InvalidKeyException {
				byte[] key = AES.getMD5Hash(new String(password));
				String src = AES.decryptAES(enc, key, iv);
				clearArray(key);
				return src;
			}

			@Override
			public boolean areSaltedHashesSupported() {
				return false;
			}
		});

		addCipherAlgorithm(new EncryptionCipher() {
			@Override
			public String getCipherName() {return "None";}

			@Override
			public String encrypt(String source, char[] password, byte[] iv, byte[] salt) throws InvalidKeySpecException{return source;}

			@Override
			public String decrypt(String enc, char[] password, byte[] iv, byte[] salt) throws InvalidKeyException{return enc;}

			@Override
			public boolean areSaltedHashesSupported(){return false;}
		});
	}

	private static void clearArray(byte[] b){
		for(int i = 0; i < b.length; i++){b[i] = 0;}
	}

	private static void clearArray(char[] c){
		for(int i = 0; i < c.length; i++){c[i] = 0;	}
	}
}
