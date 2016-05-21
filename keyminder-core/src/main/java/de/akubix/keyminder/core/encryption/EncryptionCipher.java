/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	EncryptionCipher.java

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
import java.security.spec.InvalidKeySpecException;

public interface EncryptionCipher {
	public String encrypt(String source, char[] password, byte[] iv, byte[] salt) throws InvalidKeySpecException;
	public String decrypt(String enc, char[] password, byte[] iv, byte[] salt) throws InvalidKeyException;
	public boolean areSaltedHashesSupported();
	public String getCipherName();
}
