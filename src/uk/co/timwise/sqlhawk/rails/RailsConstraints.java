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
package uk.co.timwise.sqlhawk.rails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;

public class RailsConstraints {

	/**
	 * Ruby on Rails-based databases typically have no real referential integrity
	 * constraints.  Instead they have a somewhat unusual way of associating
	 * columns to primary keys.<p>
	 *
	 * Basically all tables have a primary key named <code>ID</code>.
	 * All tables are named plural names.
	 * The columns that logically reference that <code>ID</code> are the singular
	 * form of the table name suffixed with <code>_ID</code>.<p>
	 *
	 * A side-effect of calling this method is that the returned collection of
	 * constraints will be "tied into" the associated tables.
	 *
	 * @param tables
	 * @return List of {@link RailsForeignKeyConstraint}s
	 */
	public static List<RailsForeignKeyConstraint> getRailsConstraints(Map<String, Table> tables) {
		List<RailsForeignKeyConstraint> railsConstraints = new ArrayList<RailsForeignKeyConstraint>(tables.size());
	
		// iterate thru each column in each table looking for columns that
		// match Rails naming conventions
		for (Table table : tables.values()) {
			for (TableColumn column : table.getColumns()) {
				String columnName = column.getName().toLowerCase();
				if (!column.isForeignKey() && column.allowsImpliedParents() && columnName.endsWith("_id")) {
					String singular = columnName.substring(0, columnName.length() - 3);
					String primaryTableName = Inflection.pluralize(singular);
					Table primaryTable = tables.get(primaryTableName);
					if (primaryTable != null) {
						TableColumn primaryColumn = primaryTable.getColumn("ID");
						if (primaryColumn != null) {
							railsConstraints.add(new RailsForeignKeyConstraint(primaryColumn, column));
						}
					}
				}
			}
		}
	
		return railsConstraints;
	}

}
