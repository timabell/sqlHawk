/*
 * This file is a part of the sqlHawk project.
 * http://timabell.github.com/sqlHawk/
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.timwise.sqlhawk.scm.read;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpgradeScriptReader {

	public static List<String> getUpgradeScripts(File scriptFolder) throws IOException, Exception {
		int strip = scriptFolder.toString().length() + 1; // remove base path + trailing slash
		return recurseUpgradeScriptFolder(scriptFolder, strip);
	}

	private static List<String> recurseUpgradeScriptFolder(File scriptFolder, int strip) throws IOException, Exception {
		List<String> results = new ArrayList<String>();
		File[] files = scriptFolder.listFiles();
		Arrays.sort(files, new NumericStringComparator());
		for (File file : files) {
			if (file.isDirectory()) {
				results.addAll(recurseUpgradeScriptFolder(file, strip));
			}
			if (!file.getName().endsWith(".sql")) // skip non sql files
				continue;
		// add relative path of the sql file to the list:
			results.add(file.toString().substring(strip));
		}
		return results;
	}
}
