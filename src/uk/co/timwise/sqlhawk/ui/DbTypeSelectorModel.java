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
package uk.co.timwise.sqlhawk.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import uk.co.timwise.sqlhawk.config.DatabaseTypeFinder;
import uk.co.timwise.sqlhawk.config.DbType;

public class DbTypeSelectorModel extends AbstractListModel implements ComboBoxModel {
	private static final long serialVersionUID = 1L;
	private final List<DbType> dbConfigs = new ArrayList<DbType>();
	private Object selected;
	private final Logger logger = Logger.getLogger(getClass().getName());

	public DbTypeSelectorModel(String defaultType) {
		Pattern pattern = Pattern.compile(".*/" + defaultType);
		Set<String> dbTypes = new TreeSet<String>(DatabaseTypeFinder.getBuiltInDatabaseTypes());
		for (String typeName : dbTypes) {
			DbType config = null;
			try {
				config = new DbType(typeName);
			} catch (Exception e) {
				logger.severe("Error loading properties for db type '" + typeName + "':\n" + e.toString());
				System.exit(1);
			}
			dbConfigs.add(config);

			if (pattern.matcher(typeName).matches()) {
				setSelectedItem(config);
			}
		}

		if (getSelectedItem() == null && dbConfigs.size() > 0)
			setSelectedItem(dbConfigs.get(0));
	}

	/* (non-Javadoc)
	 * @see javax.swing.ComboBoxModel#getSelectedItem()
	 */
	public Object getSelectedItem() {
		return selected;
	}

	/* (non-Javadoc)
	 * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
	 */
	public void setSelectedItem(Object anItem) {
		selected = anItem;
	}

	/* (non-Javadoc)
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	public Object getElementAt(int index) {
		return dbConfigs.get(index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.ListModel#getSize()
	 */
	public int getSize() {
		return dbConfigs.size();
	}
}

