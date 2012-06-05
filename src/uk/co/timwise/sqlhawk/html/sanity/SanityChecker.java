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
package uk.co.timwise.sqlhawk.html.sanity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.timwise.sqlhawk.html.TableSorter;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;
import uk.co.timwise.sqlhawk.model.TableIndex;

public class SanityChecker {

	/**
	 * Return a list of <code>TableColumn</code>s that are both nullable
	 * and have an index that specifies that they must be unique (a rather strange combo).
	 */
	public static List<TableColumn> getMustBeUniqueNullableColumns(Collection<Table> tables) {
		List<TableColumn> uniqueNullables = new ArrayList<TableColumn>();
	
		for (Table table : tables) {
			for (TableIndex index : table.getIndexes()) {
				if (index.isUniqueNullable()) {
					uniqueNullables.addAll(index.getColumns());
				}
			}
		}
	
		return TableSorter.sortColumnsByTable(uniqueNullables);
	}

	/**
	 * Return a list of <code>Table</code>s that have neither an index nor a primary key.
	 */
	public static List<Table> getTablesWithoutIndexes(Collection<Table> tables) {
		List<Table> withoutIndexes = new ArrayList<Table>();
	
		for (Table table : tables) {
			if (!table.isView() && table.getIndexes().size() == 0)
				withoutIndexes.add(table);
		}
	
		return TableSorter.sortTablesByName(withoutIndexes);
	}

	public static List<Table> getTablesWithIncrementingColumnNames(Collection<Table> tables) {
		List<Table> denormalizedTables = new ArrayList<Table>();
	
		for (Table table : tables) {
			Map<String, Long> columnPrefixes = new HashMap<String, Long>();
	
			for (TableColumn column : table.getColumns()) {
				// search for columns that start with the same prefix
				// and end in an incrementing number
	
				String columnName = column.getName();
				String numbers = null;
				for (int i = columnName.length() - 1; i > 0; --i) {
					if (Character.isDigit(columnName.charAt(i))) {
						numbers = String.valueOf(columnName.charAt(i)) + (numbers == null ? "" : numbers);
					} else {
						break;
					}
				}
	
				// attempt to detect where they had an existing column
				// and added a "column2" type of column (we'll call this one "1")
				if (numbers == null) {
					numbers = "1";
					columnName = columnName + numbers;
				}
	
				// see if we've already found a column with the same prefix
				// that had a numeric suffix +/- 1.
				String prefix = columnName.substring(0, columnName.length() - numbers.length());
				long numeric = Long.parseLong(numbers);
				Long existing = columnPrefixes.get(prefix);
				if (existing != null && Math.abs(existing.longValue() - numeric) == 1) {
					// found one so add it to our list and stop evaluating this table
					denormalizedTables.add(table);
					break;
				}
				columnPrefixes.put(prefix, new Long(numeric));
			}
		}
	
		return TableSorter.sortTablesByName(denormalizedTables);
	}

	public static List<Table> getTablesWithOneColumn(Collection<Table> tables) {
		List<Table> singleColumnTables = new ArrayList<Table>();
	
		for (Table table : tables) {
			if (table.getColumns().size() == 1)
				singleColumnTables.add(table);
		}
	
		return TableSorter.sortTablesByName(singleColumnTables);
	}

	/**
	 * Returns a list of columns that have the word "NULL" or "null" as their default value
	 * instead of the likely candidate value null.
	 *
	 * @param tables Collection
	 * @return List
	 */
	public static List<TableColumn> getDefaultNullStringColumns(Collection<Table> tables) {
		List<TableColumn> defaultNullStringColumns = new ArrayList<TableColumn>();
	
		for (Table table : tables) {
			for (TableColumn column : table.getColumns()) {
				Object defaultValue = column.getDefaultValue();
				if (defaultValue != null && defaultValue instanceof String) {
					String defaultString = defaultValue.toString();
					if (defaultString.trim().equalsIgnoreCase("null")) {
						defaultNullStringColumns.add(column);
					}
				}
			}
		}
	
		return TableSorter.sortColumnsByTable(defaultNullStringColumns);
	}

}
