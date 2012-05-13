package uk.co.timwise.sqlhawk.rails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.timwise.sqlhawk.model.RailsForeignKeyConstraint;
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
