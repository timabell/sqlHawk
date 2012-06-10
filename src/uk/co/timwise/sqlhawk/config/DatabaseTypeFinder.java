/* This file is a part of the sqlHawk project.
 * http://timabell.github.com/sqlHawk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package uk.co.timwise.sqlhawk.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;

public class DatabaseTypeFinder {
	private static final Logger logger = Logger.getLogger(DatabaseTypeFinder.class.getName());

	public static Set<String> getBuiltInDatabaseTypes() {
		Set<String> databaseTypes = new TreeSet<String>();
		JarInputStream jar = null;

		try {
			jar = new JarInputStream(new FileInputStream(Config.getJarName()));
			JarEntry entry;
	
			while ((entry = jar.getNextJarEntry()) != null) {
				String entryName = entry.getName();
				int dotPropsIndex = entryName.indexOf(".properties");
				if (dotPropsIndex != -1)
					databaseTypes.add(entryName.substring(0, dotPropsIndex));
			}
		} catch (Exception exc) {
			logger.severe("Failed to open jar and read properties files:\n" + exc);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException ignore) {
					logger.warning("Failed to close file handle to jar:\n" + ignore.toString());
				}
			}
		}
		return databaseTypes;
	}
}
