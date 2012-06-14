package uk.co.timwise.sqlhawk.test.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MySql extends BaseIntegrationTest {
	private static final String TARGET_TYPE = "mysql";

	@Before
	public void resetTestDatabase() throws Exception {
		IntegrationTester.setupTestDatbase(TARGET_TYPE);
	}

	@Test
	public void MySql_ScmToDatabase() throws Exception {
		IntegrationTester.testScmToDatabase(TARGET_TYPE);
	}

	@Test
	public void MySql_DatabaseToHtml() throws Exception {
		IntegrationTester.testDatabaseToHtml(TARGET_TYPE, tempOutput);
	}

	@After
	public void cleanTestDatabase() throws Exception {
		IntegrationTester.cleanTestDatabase(TARGET_TYPE);
	}
}
