package uk.co.timwise.sqlhawk.test.integration;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class BaseIntegrationTest {

	@Rule
	public TemporaryFolder tempOutput = new TemporaryFolder();

	public BaseIntegrationTest() {
		super();
	}

}
