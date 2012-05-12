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
package uk.co.timwise.sqlhawk.view;

import java.util.Set;

import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Table;

/**
 * Implementations of this interface know how to take SQL and format it
 * into (hopefully) readable HTML.
 */
public interface SqlFormatter {
    /**
     * Return a HTML-formatted representation of the specified SQL.
     *
     * @param sql SQL to be formatted
     * @param db Database
     * @param references set of tables referenced by this SQL
     *      (populated by the formatter) or left empty if the formatter already
     *      provides references to those tables
     * @return HTML-formatted representation of the specified SQL
     */
    String format(String sql, Database db, Set<Table> references);
}
