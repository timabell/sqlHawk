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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;


public class DbType {
	private String dbPropertiesLoadedFrom;
	private Properties props;
	private String name;
	private final Logger logger = Logger.getLogger(getClass().getName());
	private boolean alterSupported;

	public String getDbPropertiesLoadedFrom() {
		return dbPropertiesLoadedFrom;
	}


	/**
	 * @param type
	 * @return
	 * @throws IOException
	 * @throws InvalidConfigurationException if db properties are incorrectly formed
	 */
	public DbType getDbType(String type) throws IOException, InvalidConfigurationException {
		ResourceBundle bundle = null;
		String dbPropertiesLoadedFrom = null;

		// Try loading type as the full filename of a properties file
		File propertiesFile = new File(type);

		if (!propertiesFile.exists()) {
			// Not found, so try loading with a fixed filename extension instead
			propertiesFile = new File(type + ".properties");
		}

		if (propertiesFile.exists()) {
			// The specified is a filename, which has been found.
			// Load the file's contents into a properties bundle ready for parsing
			bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
			dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
		} else {

			// The type wasn't a valid file, so load the properties from within the running jar
			try {
				bundle = ResourceBundle.getBundle(type);
				dbPropertiesLoadedFrom = "[" + Config.getJarName() + "]" + File.separator + type + ".properties";
			} catch (Exception notInJarWithoutPath) {

				// Failed to load the type from the jar, likely debugging raw .class files so find it in the source path for database type properties instead
				try {
					String path = "dbTypes/" + type;
					bundle = ResourceBundle.getBundle(path);
					dbPropertiesLoadedFrom = path + ".properties";
				} catch (Exception notInJar) {
					// This database type has no matching properties that we can find, log the failure.
					logger.severe("Failed to find properties for db type '"+type+"' in file '"+type+"' or '"
							+type+".properties', and no bundled version found:\n"
							+ notInJar.toString() + "\n" + notInJarWithoutPath.toString());
					throw new InvalidConfigurationException("Unable to find database properties for specified type '" + type + "'");
				}
			}
		}

		Properties props = asProperties(bundle);
		bundle = null;

		// bring in key/values pointed to by the include directive
		// example: include.1=mysql::selectRowCountSql
		for (int i = 1; true; ++i) {
			String include = (String)props.remove("include." + i);
			if (include == null)
				break;

			int separator = include.indexOf("::");
			if (separator == -1)
				throw new InvalidConfigurationException("include directive in " + dbPropertiesLoadedFrom + " must have '::' between dbType and key");

			String refdType = include.substring(0, separator).trim();
			String refdKey = include.substring(separator + 2).trim();

			// recursively resolve the ref'd properties file and the ref'd key
			Properties refdProps = getDbType(refdType).getProps();
			props.put(refdKey, refdProps.getProperty(refdKey));
		}

		// bring in base properties files pointed to by the extends directive
		String baseDbType = (String)props.remove("extends");
		if (baseDbType != null) {
			baseDbType = baseDbType.trim();
			Properties baseProps = getDbType(baseDbType).getProps();

			// overlay our properties on top of the base's
			baseProps.putAll(props);
			props = baseProps;
		}

		DbType dbType = new DbType();
		dbType.dbPropertiesLoadedFrom = dbPropertiesLoadedFrom;
		dbType.props = props;
		dbType.name = type;
		dbType.alterSupported = Boolean.parseBoolean(props.getProperty("supportsAlterProc"));
		return dbType;
}

	/**
	 * Returns a {@link Properties} populated with the contents of <code>bundle</code>
	 *
	 * @param bundle ResourceBundle
	 * @return Properties
	 */
	public static Properties asProperties(ResourceBundle bundle) {
		Properties props = new Properties();
		Enumeration<String> iter = bundle.getKeys();
		while (iter.hasMoreElements()) {
			String key = iter.nextElement();
			props.put(key, bundle.getObject(key));
		}

		return props;
	}

	public Properties getProps() {
		return props;
	}


	public String getName() {
		return name;
	}


	public boolean isAlterSupported() {
		return alterSupported;
	}
}
