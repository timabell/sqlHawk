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
package uk.co.timwise.sqlhawk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.text.TableOrderer;

public class DbType {
	private String dbPropertiesLoadedFrom;
	private Properties props;
	private String name;

	public String getDbPropertiesLoadedFrom() {
		return dbPropertiesLoadedFrom;
	}


	public static Set<String> getBuiltInDatabaseTypes() {
		return getBuiltInDatabaseTypes(Config.getJarName());
	}

	public static Set<String> getBuiltInDatabaseTypes(String loadedFromJar) {
		Set<String> databaseTypes = new TreeSet<String>();
		JarInputStream jar = null;

		try {
			jar = new JarInputStream(new FileInputStream(loadedFromJar));
			JarEntry entry;

			while ((entry = jar.getNextJarEntry()) != null) {
				String entryName = entry.getName();
				int dotPropsIndex = entryName.indexOf(".properties");
				if (dotPropsIndex != -1)
					databaseTypes.add(entryName.substring(0, dotPropsIndex));
			}
		} catch (IOException exc) {
			System.out.println("Failed to open jar to read properties files:\n" + exc);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException ignore) {}
			}
		}

		return databaseTypes;
	}

	/**
	 * @param type
	 * @return
	 * @throws IOException
	 * @throws InvalidConfigurationException if db properties are incorrectly formed
	 */
	public static DbType getDbType(String type) throws IOException, InvalidConfigurationException {
		ResourceBundle bundle = null;
		String dbPropertiesLoadedFrom;
		try {
			File propertiesFile = new File(type);
			bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
			dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
		} catch (FileNotFoundException notFoundOnFilesystemWithoutExtension) {
			try {
				File propertiesFile = new File(type + ".properties");
				bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
				dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
			} catch (FileNotFoundException notFoundOnFilesystemWithExtensionTackedOn) {
				try {
					bundle = ResourceBundle.getBundle(type);
					dbPropertiesLoadedFrom = "[" + Config.getJarName() + "]" + File.separator + type + ".properties";
				} catch (Exception notInJarWithoutPath) {
					try {
						String path = TableOrderer.class.getPackage().getName() + ".dbTypes." + type;
						path = path.replace('.', '/');
						bundle = ResourceBundle.getBundle(path);
						dbPropertiesLoadedFrom = "[" + Config.getJarName() + "]/" + path + ".properties";
					} catch (Exception notInJar) {
						notInJar.printStackTrace();
						notFoundOnFilesystemWithExtensionTackedOn.printStackTrace();
						throw notFoundOnFilesystemWithoutExtension;
					}
				}
			}
		}

		Properties props = asProperties(bundle);
		bundle = null;
		String saveLoadedFrom = dbPropertiesLoadedFrom; // keep original thru recursion

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

		// done with this level of recursion...restore original
		dbPropertiesLoadedFrom = saveLoadedFrom;
		DbType dbType = new DbType();
		dbType.dbPropertiesLoadedFrom = dbPropertiesLoadedFrom;
		dbType.props = props;
		dbType.name = type;
		return dbType;
}

	/**
	 * Returns a {@link Properties} populated with the contents of <code>bundle</code>
	 *
	 * @param bundle ResourceBundle
	 * @return Properties
	 */
	private static Properties asProperties(ResourceBundle bundle) {
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
}
