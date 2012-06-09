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
package uk.co.timwise.sqlhawk.test.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.Level;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.config.DbType;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;
import uk.co.timwise.sqlhawk.controller.SchemaMapper.ConnectionWithMeta;
import uk.co.timwise.sqlhawk.db.write.DbWriter;

/**
 * The Class IntegrationTester.
 * Provides generic routines for putting the implementation
 * of sqlhawk against each dbms through its paces.
 * Details of connections and scripts to test are on the filesystem
 * under test/test-data/[database-type]/.
 */
public class IntegrationTester {

	static void setupTestDatbase(String type) throws Exception {
		System.out.println("Integration test: setting up " + type);
		runSetupSql(type, "setup");
	}

	static void testDatabase(String type) throws Exception {
		Config config = getTestConfig(type);

		config.setScmInputEnabled(true);
		config.setTargetDir("test/test-data/" + type + "/scm-input");

		config.setIntializeLogEnabled(true);
		config.setDatabaseOutputEnabled(true);

		new SchemaMapper().RunMapping(config);

		validateDatabase(type);
	}

	static void cleanTestDatabase(String type) throws Exception {
		System.out.println("Integration test: cleaning up " + type);
		runSetupSql(type, "clean");
	}

	private static void runSetupSql(String type, String target) throws Exception {
		Config setupDbConfig = getSetupConfig(type);
		runSqlFile(type, target, setupDbConfig);
	}

	private static void validateDatabase(String type) throws Exception {
		System.out.println("validate");
		Config config = getTestConfig(type);
		runSqlFile(type, "validate", config);
	}

	private static Config getSetupConfig(String type) throws IOException, FileNotFoundException {
		Config setupDbConfig = new Config();
		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/" + type + "/setup.properties")));
		Properties properties = DbType.asProperties(bundle);

		setupDbConfig.setDbTypeName(type);
		setupDbConfig.setHost(properties.getProperty("host"));
		setupDbConfig.setDatabase(properties.getProperty("database"));
		setupDbConfig.setUser(properties.getProperty("user"));
		return setupDbConfig;
	}

	private static Config getTestConfig(String type) throws FileNotFoundException, IOException {
		Config config = new Config();
		config.setLogLevel(Level.FINEST);
		config.setDbTypeName(type);

		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/" + type + "/test.properties")));
		Properties properties = DbType.asProperties(bundle);
		config.setHost(properties.getProperty("host"));
		config.setDatabase(properties.getProperty("database"));
		config.setUser(properties.getProperty("user"));
		config.setPassword(properties.getProperty("password"));
		return config;
	}

	private static void runSqlFile(String type, String target, Config setupDbConfig) throws Exception, IOException, SQLException {
		ConnectionWithMeta connection = new SchemaMapper().getConnection(setupDbConfig);
		DbWriter.runSqlScriptFile(connection.Connection, new File("."), "test/test-data/" + type + "/" + target + ".sql", false);
	}
}
