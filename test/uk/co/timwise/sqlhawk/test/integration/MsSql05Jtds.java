package uk.co.timwise.sqlhawk.test.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MsSql05Jtds {
	private static final String TARGET_TYPE = "mssql05-jtds"; // MicroSoft Sql server

	@Before
	public void resetTestDatabase() throws Exception {
		IntegrationTester.setupTestDatbase(TARGET_TYPE);
	}

	@Test
	public void MsSql05Jtds_ScmToDatabase() throws Exception {
		IntegrationTester.testScmToDatabase(TARGET_TYPE);
	}

	@Test
	public void MsSql05Jtds_DatabaseToHtml() throws Exception {
		IntegrationTester.testDatabaseToHtml(TARGET_TYPE);
	}

	@After
	public void cleanTestDatabase() throws Exception {
		IntegrationTester.cleanTestDatabase(TARGET_TYPE);
	}
}
