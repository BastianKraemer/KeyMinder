package de.akubix.keyminder.core.encryption;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Test;

public class AES_Test {
	@Test
	public void testBASE64Encoding() {
		final String txt = "VGhpcyBpcyBhIEJBU0U2NCBlbmNvZGVkIFN0cmluZw==";
		byte[] data = AES.bytesFromBase64String(txt);
		assertEquals("Testing BASE64 encoding...", txt, AES.bytesToBase64String(data));
	}

	@Test
	public void testHashAlgorithmns() {
		final String md5_str = "qGWn4N2/NfpvaiMuCJO+pA=="; // MD5 checksum of "my_password", BASE64 encoded
		final String sha_str = "9uJI6plPPjQvYRQbi44+3obU3lMleryNBq4Hodpz+zk="; // SHA256 checksum of "my_password", BASE64 encoded

		assertEquals("Testing MD5...", md5_str, AES.bytesToBase64String(AES.getMD5Hash("my_password")));
		assertEquals("Testing SHA256...", sha_str, AES.bytesToBase64String(AES.getSHA256Hash("my_password")));
	}

	@Test
	public void testAESEncryption() throws InvalidKeySpecException, InvalidKeyException {
		byte[] iv = AES.generateIV();
		byte[] salt = AES.generatePasswordSalt(16);

		final String encryption_src = "This text will be encrypted using AES";

		try{
			if(AES.isAES256EncryptionAvailable()){
				String encrypted_text = AES.encryptAES(encryption_src, AES.getPBKDF2Hash("my_password", salt), iv);
				assertEquals("Testing AES-256 encryption...", encryption_src, AES.decryptAES(encrypted_text, AES.getPBKDF2Hash("my_password", salt), iv));
			}
			else{
				String encrypted_text = AES.encryptAES(encryption_src, AES.getMD5Hash("my_password"), iv);
				assertEquals("Testing AES-128 encryption...", encryption_src, AES.decryptAES(encrypted_text, AES.getMD5Hash("my_password"), iv));
			}
		}
		catch(NoSuchAlgorithmException e){
			System.out.println("WARNING: Cannot run AES test. AES is not supported on this System.");
		}
	}

}
