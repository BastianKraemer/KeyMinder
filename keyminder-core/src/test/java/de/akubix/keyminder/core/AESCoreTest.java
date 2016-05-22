package de.akubix.keyminder.core;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Test;

import de.akubix.keyminder.lib.AESCore;

public class AESCoreTest {
	@Test
	public void testBASE64Encoding() {
		final String txt = "VGhpcyBpcyBhIEJBU0U2NCBlbmNvZGVkIFN0cmluZw==";
		byte[] data = AESCore.bytesFromBase64String(txt);
		assertEquals("Testing BASE64 encoding...", txt, AESCore.bytesToBase64String(data));
	}

	@Test
	public void testHashAlgorithmns() {
		final String md5_str = "qGWn4N2/NfpvaiMuCJO+pA=="; // MD5 checksum of "my_password", BASE64 encoded
		final String sha_str = "9uJI6plPPjQvYRQbi44+3obU3lMleryNBq4Hodpz+zk="; // SHA256 checksum of "my_password", BASE64 encoded

		assertEquals("Testing MD5...", md5_str, AESCore.bytesToBase64String(AESCore.getMD5Hash("my_password")));
		assertEquals("Testing SHA256...", sha_str, AESCore.bytesToBase64String(AESCore.getSHA256Hash("my_password")));
	}

	@Test
	public void testAESEncryption() throws InvalidKeySpecException, InvalidKeyException {
		byte[] iv = AESCore.generateIV();
		byte[] salt = AESCore.generatePasswordSalt(16);

		final String encryption_src = "This text will be encrypted using AES";

		try{
			if(AESCore.isAES256EncryptionAvailable()){
				String encrypted_text = AESCore.encryptAES(encryption_src, AESCore.getPBKDF2Hash("my_password", salt), iv);
				assertEquals("Testing AES-256 encryption...", encryption_src, AESCore.decryptAES(encrypted_text, AESCore.getPBKDF2Hash("my_password", salt), iv));
			}
			else{
				String encrypted_text = AESCore.encryptAES(encryption_src, AESCore.getMD5Hash("my_password"), iv);
				assertEquals("Testing AES-128 encryption...", encryption_src, AESCore.decryptAES(encrypted_text, AESCore.getMD5Hash("my_password"), iv));
			}
		}
		catch(NoSuchAlgorithmException e){
			System.out.println("WARNING: Cannot run AES test. AES is not supported on this System.");
		}
	}

}
