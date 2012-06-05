package uk.co.timwise.sqlhawk.test.unit;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.co.timwise.sqlhawk.db.SqlManagement;

public class SqlManagementTests {

	@Test
	public void testConvertCreateToAlter() {
		// arrange
		String sqlText = "-- alter me\nALTER proc1 AS\n foo";

		// act
		String actual = SqlManagement.ConvertAlterToCreate(sqlText);

		// assert
		String expected = "-- alter me\nCREATE proc1 AS\n foo";
		assertEquals(expected, actual);
	}

	@Test
	public void testConvertAlterToCreate() {
		// arrange
		String sqlText = "-- alter me\nCREATE proc1 AS\n foo";

		// act
		String actual = SqlManagement.ConvertCreateToAlter(sqlText);

		// assert
		String expected = "-- alter me\nALTER proc1 AS\n foo";
		assertEquals(expected, actual);
	}

	@Test
	public void testSplitBatches() {
		// arrange
		String sqlText = "select 'batch1';\nGO\nselect 'batch2';";

		// act
		String[] actual = SqlManagement.SplitBatches(sqlText);

		// assert
		String[] expected = { "select 'batch1';\n", "\nselect 'batch2';" };
		assertArrayEquals(expected, actual);
	}
}
