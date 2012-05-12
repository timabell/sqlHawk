/* This file is a part of the sqlHawk project.
 * http://github.com/timabell/sqlHawk
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
package uk.co.timwise.sqlhawk.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.DbType;

/**
 * Configuration of a specific type of database (as specified by --db-type)
 */
public class DbSpecificConfig {
	private final DbType dbType;
	private final List<DbSpecificOption> options = new ArrayList<DbSpecificOption>();

	/**
	 * Construct an instance with configuration options of the specified database type
	 *
	 * @param dbType
	 */
	public DbSpecificConfig(DbType dbType) {
		this.dbType = dbType;
		loadOptions();
	}

	/**
	 * Resolve the options specified by connectionSpec into {@link DbSpecificOption}s.
	 *
	 * @param properties
	 */
	private void loadOptions() {
		Properties properties = dbType.getProps();
		boolean inParam = false;

		StringTokenizer tokenizer = new StringTokenizer(properties.getProperty("connectionSpec"), "<>", true);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals("<")) {
				inParam = true;
			} else if (token.equals(">")) {
				inParam = false;
			} else {
				if (inParam) {
					String desc = properties.getProperty(token);
					options.add(new DbSpecificOption(token, desc));
				}
			}
		}
	}

	/**
	 * Returns a {@link List} of {@link DbSpecificOption}s that are applicable to the
	 * specified database type.
	 *
	 * @return
	 */
	public List<DbSpecificOption> getOptions() {
		return options;
	}

	/**
	 * Dump usage details associated with the associated type of database
	 */
	public void dumpUsage() {
		System.out.println(" " + new File(dbType.getName()).getName() + ":");
		System.out.println("  " + toString());

		for (DbSpecificOption option : getOptions()) {
			System.out.println("   " + option.getName() + ": " + (option.getDescription() != null ? "  \t" + option.getDescription() : ""));
		}
	}

	/**
	 * Return description of the associated type of database
	 */
	@Override
	public String toString() {
		return dbType.getProps().getProperty("description");
	}
}
