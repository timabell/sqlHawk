package uk.co.timwise.sqlhawk.test.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MsSqlJtds {
	private static final String TARGET_TYPE = "mssql-jtds"; // MicroSoft Sql server

	@Before
	public void resetTestDatabase() throws Exception {
		IntegrationTester.setupTestDatbase(TARGET_TYPE);
	}

	@Test
	public void testMsSqlJtds() throws Exception {
		IntegrationTester.testDatabase(TARGET_TYPE);
	}

	@After
	public void cleanTestDatabase() throws Exception {
		IntegrationTester.cleanTestDatabase(TARGET_TYPE);
	}
}
