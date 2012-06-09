package uk.co.timwise.sqlhawk.test.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MySql {
	private static final String TARGET_TYPE = "mysql";

	@Before
	public void resetTestDatabase() throws Exception {
		IntegrationTester.setupTestDatbase(TARGET_TYPE);
	}

	@Test
	public void testMySql() throws Exception {
		IntegrationTester.testDatabase(TARGET_TYPE);
	}

	@After
	public void cleanTestDatabase() throws Exception {
		IntegrationTester.cleanTestDatabase(TARGET_TYPE);
	}
}
