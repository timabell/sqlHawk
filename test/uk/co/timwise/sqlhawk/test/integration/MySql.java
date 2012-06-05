package uk.co.timwise.sqlhawk.test.integration;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.co.timwise.sqlhawk.SchemaMapper;
import uk.co.timwise.sqlhawk.config.Config;

public class MySql {

	@Test
	public void testMySqlGetProc() throws Exception {
		// arrange
		SchemaMapper mapper = new SchemaMapper();
		Config config = new Config();

		// act
		mapper.RunMapping(config);

		//assert
	}

}
