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
package uk.co.timwise.sqlhawk.html.implied;

import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.TableColumn;

public class ImpliedForeignKeyConstraint extends ForeignKeyConstraint {
	/**
	 * @param parentColumn
	 * @param childColumn
	 */
	public ImpliedForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) {
		super(parentColumn, childColumn);
	}

	/**
	 * @return
	 */
	@Override
	public String getName() {
		return "Implied Constraint";
	}

	/**
	 * @return
	 */
	@Override
	public boolean isImplied() {
		return true;
	}

	/**
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(getChildTable());
		buf.append(".");
		buf.append(toString(getChildColumns()));
		buf.append("'s name implies that it's a child of ");
		buf.append(getParentTable());
		buf.append(".");
		buf.append(toString(getParentColumns()));
		buf.append(", but it doesn't reference that column.");
		return buf.toString();
	}
}
