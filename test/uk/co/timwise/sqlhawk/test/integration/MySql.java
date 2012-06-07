package uk.co.timwise.sqlhawk.test.integration;

import static org.junit.Assert.*;

import java.util.logging.Level;

import org.junit.Test;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;

public class MySql {

	@Test
	public void testMySqlGetProc() throws Exception {
		// arrange
		SchemaMapper mapper = new SchemaMapper();
		Config config = new Config();
		config.setLogLevel(Level.FINEST);

		config.setScmInputEnabled(true);
		config.setTargetDir("test/test-data/mysql/scm-input");

		config.setDatabaseOutputEnabled(true);
		config.setDbTypeName("mysql");
		config.setHost("localhost");
		config.setDatabase("sqlhawktesting");
		config.setUser("sqlhawktesting");
		config.setPassword("sqlhawktesting");

		// act
		mapper.RunMapping(config);

		//assert
	}

}
