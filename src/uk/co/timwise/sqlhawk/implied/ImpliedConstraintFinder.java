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
package uk.co.timwise.sqlhawk.implied;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.co.timwise.sqlhawk.DbAnalyzer;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;

public class ImpliedConstraintFinder {

	public static List<ImpliedForeignKeyConstraint> getImpliedConstraints(Collection<Table> tables) {
		List<TableColumn> columnsWithoutParents = new ArrayList<TableColumn>();
		Map<TableColumn, Table> allPrimaries = new TreeMap<TableColumn, Table>(new Comparator<TableColumn>() {
			public int compare(TableColumn column1, TableColumn column2) {
				int rc = column1.getName().compareToIgnoreCase(column2.getName());
				if (rc == 0)
					rc = column1.getType().compareToIgnoreCase(column2.getType());
				if (rc == 0)
					rc = column1.getLength() - column2.getLength();
				return rc;
			}
		});
	
		int duplicatePrimaries = 0;
	
		// gather all the primary key columns and columns without parents
		for (Table table : tables) {
			List<TableColumn> tablePrimaries = table.getPrimaryColumns();
			if (tablePrimaries.size() == 1) { // can't match up multiples...yet...
				for (TableColumn primary : tablePrimaries) {
					if (primary.allowsImpliedChildren() &&
							allPrimaries.put(primary, table) != null)
						++duplicatePrimaries;
				}
			}
	
			for (TableColumn column : table.getColumns()) {
				if (!column.isForeignKey() && column.allowsImpliedParents())
					columnsWithoutParents.add(column);
			}
		}
	
		// if more than half of the tables have the same primary key then
		// it's most likely a database where primary key names aren't unique
		// (e.g. they all have a primary key named 'ID')
		if (duplicatePrimaries > allPrimaries.size()) // bizarre logic, but it does approximately what we need
			return new ArrayList<ImpliedForeignKeyConstraint>();
	
		DbAnalyzer.sortColumnsByTable(columnsWithoutParents);
	
		List<ImpliedForeignKeyConstraint> impliedConstraints = new ArrayList<ImpliedForeignKeyConstraint>();
		for (TableColumn childColumn : columnsWithoutParents) {
			Table primaryTable = allPrimaries.get(childColumn);
			if (primaryTable != null && primaryTable != childColumn.getTable()) {
				TableColumn parentColumn = primaryTable.getColumn(childColumn.getName());
				// make sure the potential child->parent relationships isn't already a
				// parent->child relationship
				if (parentColumn.getParentConstraint(childColumn) == null) {
					// ok, we've found a potential relationship with a column matches a primary
					// key column in another table and isn't already related to that column
					impliedConstraints.add(new ImpliedForeignKeyConstraint(parentColumn, childColumn));
				}
			}
		}
	
		return impliedConstraints;
	}

}
