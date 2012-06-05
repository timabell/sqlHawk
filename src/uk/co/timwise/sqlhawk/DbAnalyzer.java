/* This file is a part of the sqlHawk project.
 * http://timabell.github.com/sqlHawk/
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

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
}
