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
package uk.co.timwise.sqlhawk.html;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;

/**
 * Ugly hack that provides details of what was written.
 * Also stores data to be used around processing,
 *  - number of items processed to see if two degree was different
 *  - whether there are implied relationships
 * This class must die. Terrible stuff.
 * Set aside several hours for disentangling it!
 */
@Deprecated
public class EvilStatsStore {
	private int numTables;
	private int numViews;
	private final Set<TableColumn> excludedColumns;

	public EvilStatsStore(Collection<Table> tables) {
		excludedColumns = new HashSet<TableColumn>();

		for (Table table : tables) {
			for (TableColumn column : table.getColumns()) {
				if (column.isExcluded()) {
					excludedColumns.add(column);
				}
			}
		}
	}

	public EvilStatsStore(EvilStatsStore stats) {
		excludedColumns = stats.excludedColumns;
	}

	public void wroteTable(Table table) {
		if (table.isView())
			++numViews;
		else
			++numTables;
	}

	public int getNumTablesWritten() {
		return numTables;
	}

	public int getNumViewsWritten() {
		return numViews;
	}

	public Set<TableColumn> getExcludedColumns() {
		return excludedColumns;
	}
}