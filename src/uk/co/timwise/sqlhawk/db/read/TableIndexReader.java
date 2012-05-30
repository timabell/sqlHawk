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
package uk.co.timwise.sqlhawk.db.read;

import java.sql.ResultSet;
import java.sql.SQLException;

import uk.co.timwise.sqlhawk.model.TableIndex;


public class TableIndexReader {

	/**
	 * @param rs
	 * @throws java.sql.SQLException
	 */
	public static TableIndex ReadTableIndex(ResultSet rs) throws SQLException {
		String name = rs.getString("INDEX_NAME");
		boolean isUnique = !rs.getBoolean("NON_UNIQUE");
		return new TableIndex(name, isUnique);
	}
}