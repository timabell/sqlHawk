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

import java.util.regex.Pattern;

/**
 * A remote table (exists in another schema) that was explicitly created via XML metadata.
 */
public class ExplicitRemoteTable extends RemoteTable {
	private static final Pattern excludeNone = Pattern.compile("[^.]");

	public ExplicitRemoteTable(String schema, String name, String baseSchema) {
		super(schema, name, baseSchema);
	}
}