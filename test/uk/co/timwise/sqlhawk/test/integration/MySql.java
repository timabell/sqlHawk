package uk.co.timwise.sqlhawk.test.integration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;
import uk.co.timwise.sqlhawk.controller.SchemaMapper.ConnectionWithMeta;
import uk.co.timwise.sqlhawk.db.SqlManagement;
import uk.co.timwise.sqlhawk.db.write.DbWriter;
import uk.co.timwise.sqlhawk.util.FileHandling;

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
		setupDbConfig.setDbTypeName("mysql");
		setupDbConfig.setHost("localhost");
		setupDbConfig.setDatabase("mysql");
		setupDbConfig.setUser("root");
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

	private Config mysqlConfig() {
		Config config = new Config();
		config.setLogLevel(Level.FINEST);
		config.setDbTypeName("mysql");
		config.setHost("localhost");
		config.setDatabase("sqlhawktesting");
		config.setUser("sqlhawktesting");
		config.setPassword("sqlhawktesting");
		return config;
	}
}
