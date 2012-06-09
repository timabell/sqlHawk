package uk.co.timwise.sqlhawk.test.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.config.DbType;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;
import uk.co.timwise.sqlhawk.controller.SchemaMapper.ConnectionWithMeta;
import uk.co.timwise.sqlhawk.db.write.DbWriter;

public class MySql {

	@Before
	public void resetMysql() throws Exception {
		System.out.println("setup");
		runSetupSql("setup");
	}

	@After
	public void cleanMysql() throws Exception {
		System.out.println("clean");
		runSetupSql("clean");
	}

	private void runSetupSql(String target) throws Exception, IOException, SQLException {
		Config setupDbConfig = new Config();
		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/mysql/setup.properties")));
		Properties properties = DbType.asProperties(bundle);

		setupDbConfig.setDbTypeName("mysql");
		setupDbConfig.setHost(properties.getProperty("host"));
		setupDbConfig.setDatabase(properties.getProperty("database"));
		setupDbConfig.setUser(properties.getProperty("user"));
		runSqlFile(target, setupDbConfig);
	}

	private void runSqlFile(String target, Config setupDbConfig) throws Exception, IOException, SQLException {
		ConnectionWithMeta connection = new SchemaMapper().getConnection(setupDbConfig);
		DbWriter.runSqlScriptFile(connection.Connection, new File("."), "test/test-data/mysql/" + target + ".sql", false);
	}

	@Test
	public void testMySql() throws Exception {
		Config config = mysqlConfig();

		config.setScmInputEnabled(true);
		config.setTargetDir("test/test-data/mysql/scm-input");

		config.setIntializeLogEnabled(true);
		config.setDatabaseOutputEnabled(true);

		new SchemaMapper().RunMapping(config);

		validate();
	}

	private void validate() throws Exception {
		System.out.println("validate");
		Config config = mysqlConfig();
		runSqlFile("validate", config);
	}

	private Config mysqlConfig() throws FileNotFoundException, IOException {
		Config config = new Config();
		config.setLogLevel(Level.FINEST);
		config.setDbTypeName("mysql");

		PropertyResourceBundle bundle = new PropertyResourceBundle(
				new FileInputStream(new File("test/test-data/mysql/test.properties")));
		Properties properties = DbType.asProperties(bundle);
		config.setHost(properties.getProperty("host"));
		config.setDatabase(properties.getProperty("database"));
		config.setUser(properties.getProperty("user"));
		config.setPassword(properties.getProperty("password"));
		return config;
	}
}
