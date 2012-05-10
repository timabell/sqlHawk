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
package uk.co.timwise.sqlhawk.model;

import uk.co.timwise.sqlhawk.DbAnalyzer;

/**
 * See {@link DbAnalyzer#getRailsConstraints(java.util.Map)} for
 * details on Rails naming conventions.
 *
 * @author John Currier
 */
public class RailsForeignKeyConstraint extends ForeignKeyConstraint {
    /**
     * @param parentColumn
     * @param childColumn
     */
    public RailsForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) {
        super(parentColumn, childColumn);
    }

    /**
     * Normally the name of the constraint, but this one is implied by
     * Rails naming conventions.
     *
     * @return
     */
    @Override
    public String getName() {
        return "ByRailsConventionConstraint";
    }
}