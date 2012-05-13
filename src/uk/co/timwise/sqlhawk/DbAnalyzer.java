/* This file is a part of the sqlHawk project.
 * http://github.com/timabell/sqlHawk
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package uk.co.timwise.sqlhawk;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;

public class DbAnalyzer {
	/**
	 * Returns a <code>List</code> of all of the <code>ForeignKeyConstraint</code>s
	 * used by the specified tables.
	 *
	 * @param tables Collection
	 * @return List
	 */
	public static List<ForeignKeyConstraint> getForeignKeyConstraints(Collection<Table> tables) {
		List<ForeignKeyConstraint> constraints = new ArrayList<ForeignKeyConstraint>();

		for (Table table : tables) {
			constraints.addAll(table.getForeignKeys());
		}

		return constraints;
	}

	public static List<Table> getOrphans(Collection<Table> tables) {
		List<Table> orphans = new ArrayList<Table>();

		for (Table table : tables) {
			if (table.isOrphan(false)) {
				orphans.add(table);
			}
		}

		return sortTablesByName(orphans);
	}

	public static List<Table> sortTablesByName(List<Table> tables) {
		Collections.sort(tables, new Comparator<Table>() {
			public int compare(Table table1, Table table2) {
				return table1.compareTo(table2);
			}
		});

		return tables;
	}

	public static List<TableColumn> sortColumnsByTable(List<TableColumn> columns) {
		Collections.sort(columns, new Comparator<TableColumn>() {
			public int compare(TableColumn column1, TableColumn column2) {
				int rc = column1.getTable().compareTo(column2.getTable());
				if (rc == 0)
					rc = column1.getName().compareToIgnoreCase(column2.getName());
				return rc;
			}
		});

		return columns;
	}

	/**
	 * getSchemas - returns a List of schema names (Strings)
	 *
	 * @param meta DatabaseMetaData
	 */
	public static List<String> getSchemas(DatabaseMetaData meta) throws SQLException {
		List<String> schemas = new ArrayList<String>();

		ResultSet rs = meta.getSchemas();
		while (rs.next()) {
			schemas.add(rs.getString("TABLE_SCHEM"));
		}
		rs.close();

		return schemas;
	}

	/**
	 * getSchemas - returns a List of schema names (Strings) that contain tables
	 *
	 * @param meta DatabaseMetaData
	 */
	public static List<String> getPopulatedSchemas(DatabaseMetaData meta) throws SQLException {
		return getPopulatedSchemas(meta, ".*");
	}

	/**
	 * getSchemas - returns a List of schema names (Strings) that contain tables and
	 * match the <code>schemaSpec</code> regular expression
	 *
	 * @param meta DatabaseMetaData
	 */
	public static List<String> getPopulatedSchemas(DatabaseMetaData meta, String schemaSpec) throws SQLException {
		Set<String> schemas = new TreeSet<String>(); // alpha sorted
		Pattern schemaRegex = Pattern.compile(schemaSpec);
		Logger logger = Logger.getLogger(DbAnalyzer.class.getName());
		boolean logging = logger.isLoggable(Level.FINE);

		Iterator<String> iter = getSchemas(meta).iterator();
		while (iter.hasNext()) {
			String schema = iter.next().toString();
			if (schemaRegex.matcher(schema).matches()) {
				ResultSet rs = null;
				try {
					rs = meta.getTables(null, schema, "%", null);
					if (rs.next()) {
						if (logging)
							logger.fine("Including schema " + schema +
									": matches + \"" + schemaRegex + "\" and contains tables");
						schemas.add(schema);
					} else {
						if (logging)
							logger.fine("Excluding schema " + schema +
									": matches \"" + schemaRegex + "\" but contains no tables");
					}
				} catch (SQLException ignore) {
				} finally {
					if (rs != null)
						rs.close();
				}
			} else {
				if (logging)
					logger.fine("Excluding schema " + schema +
							": doesn't match \"" + schemaRegex + '"');
			}
		}

		return new ArrayList<String>(schemas);
	}

	/**
	 * For debugging/analyzing result sets
	 * @param rs ResultSet
	 * @throws SQLException
	 */
	public static void dumpResultSetRow(ResultSet rs, String description) throws SQLException {
		ResultSetMetaData meta = rs.getMetaData();
		int numColumns = meta.getColumnCount();
		System.out.println(numColumns + " columns of " + description + ":");
		for (int i = 1; i <= numColumns; ++i) {
			System.out.print(meta.getColumnLabel(i));
			System.out.print(": ");
			System.out.print(String.valueOf(rs.getString(i)));
			System.out.print("\t");
		}
		System.out.println();
	}
}
