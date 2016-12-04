/* KeyMinder
 * Copyright (C) 2015 Bastian Kraemer
 *
 * StorageHandler.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.core.io;

import java.io.File;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.FileConfiguration;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.tree.TreeStore;

public interface StorageHandler {
	/**
	 * This method will be called if the "Storage Handler" is requested to open a file
	 * @param file The file that should be opened
	 * @param filePassword The password of this file (may be "")
	 * @param tree The tree the nodes should be added to
	 * @param instance The application instance, for example to print some output
	 * @return the file configuration of this file
	 * @throws StorageException Throw this exception if something went wrong
	 */
	public FileConfiguration open(File file, String filePassword, Object tree, ApplicationInstance instance) throws StorageException;

	/**
	 * This method will be called if the "Storage Handler" is requested to save the tree to a file
	 * @param file The data should be stored in this file
	 * @param tree The tree that contains the data that should be stored
	 * @param instance The application instance, for example to print some output
	 * @throws StorageException Throw this exception if something went wrong
	 */
	public void save(FileConfiguration file, TreeStore tree, ApplicationInstance instance) throws StorageException;
}
