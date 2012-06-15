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
import java.util.logging.Logger;

import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;
import uk.co.timwise.sqlhawk.controller.SchemaMapper.ConnectionWithMeta;
import uk.co.timwise.sqlhawk.db.write.DbWriter;
import uk.co.timwise.sqlhawk.logging.LogConfig;
import uk.co.timwise.sqlhawk.util.PropertyHandler;

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

	static void testScmToDatabase(String type) throws Exception {
		System.out.println("Integration test: running ScmToDatabase for " + type);
		Config config = getTestConfig(type);

		config.setScmInputEnabled(true);
		config.setTargetDir(new File("test/test-data/" + type + "/scm-input"));

		config.setIntializeLogEnabled(true);
		config.setDatabaseOutputEnabled(true);

		new SchemaMapper().RunMapping(config);

		System.out.println("Integration test: validating ScmToDatabase for " + type);
		runSqlFile(type, "validate", config);
	}

	static void testDatabaseToHtml(String type, TemporaryFolder tempOutput) throws Exception {
		System.out.println("Integration test: running DatabaseToHtml for " + type);
		Config config = getTestConfig(type);

		File targetDir = tempOutput.newFolder();
		config.setTargetDir(targetDir);

		config.setDatabaseInputEnabled(true);
		config.setHtmlGenerationEnabled(true);

		new SchemaMapper().RunMapping(config);

		System.out.println("Integration test: validating DatabaseToHtml for " + type);
		boolean generated = new File(targetDir,"index.html").exists();
		Assert.assertEquals("index.html not found in Html output", true, generated);
	}

	static void cleanTestDatabase(String type) throws Exception {
		System.out.println("Integration test: cleaning up " + type);
		runSetupSql(type, "clean");
	}

	private static void runSetupSql(String type, String target) throws Exception {
		Config setupDbConfig = getSetupConfig(type);
		LogConfig.setupLogger(setupDbConfig);
		runSqlFile(type, target, setupDbConfig);
	}

	private static Config getSetupConfig(String type) throws IOException, FileNotFoundException {
		Config setupDbConfig = new Config();
		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/" + type + "/test.properties")));
		Properties properties = PropertyHandler.bundleAsProperties(bundle);

		// TODO: convert to some kind of automatic mapping
		setupDbConfig.setLogLevel(Level.FINEST);
		setupDbConfig.setDbTypeName(type);
		setupDbConfig.setHost(properties.getProperty("host"));
		setupDbConfig.setDatabase(properties.getProperty("setup-database"));
		setupDbConfig.setUser(properties.getProperty("setup-user"));
		setupDbConfig.setPassword(properties.getProperty("setup-password"));
		return setupDbConfig;
	}

	private static Config getTestConfig(String type) throws FileNotFoundException, IOException {
		Config config = new Config();
		config.setLogLevel(Level.FINEST);
		config.setDbTypeName(type);

		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/" + type + "/test.properties")));
		Properties properties = PropertyHandler.bundleAsProperties(bundle);
		config.setHost(properties.getProperty("host"));
		config.setDatabase(properties.getProperty("test-database"));
		config.setUser(properties.getProperty("test-user"));
		config.setPassword(properties.getProperty("test-password"));
		return config;
	}

	private static void runSqlFile(String type, String target, Config setupDbConfig) throws Exception, IOException, SQLException {
		ConnectionWithMeta connection = new SchemaMapper().getConnection(setupDbConfig);
		DbWriter.runSqlScriptFile(connection.Connection, new File("."), "test/test-data/" + type + "/" + target + ".sql", false);
	}
}
