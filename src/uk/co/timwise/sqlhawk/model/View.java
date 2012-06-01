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
package uk.co.timwise.sqlhawk.model;

/**
 * Treat views as tables that have no rows and are represented by the SQL that
 * defined them.
 */
public class View extends Table implements ISqlObject {
	private String definition;

	public View() {}

	public View(String schema, String name, String comments, String definition) {
		super(schema, name, comments);
		super.setNumRows(0); // no rows in views. probably should be null but leaving as zero for now to not break calling code.
		this.definition = definition;
	}

	@Override
	public boolean isView() {
		return true;
	}

	public String getDefinition() {
		return definition;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setDefinition(String definition) {
		this.definition = definition;
	}
}
