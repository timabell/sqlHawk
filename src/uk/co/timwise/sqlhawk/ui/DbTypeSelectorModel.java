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
package uk.co.timwise.sqlhawk.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.util.DbSpecificConfig;

/**
 * @author John Currier
 */
public class DbTypeSelectorModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;
    private final List<DbSpecificConfig> dbConfigs = new ArrayList<DbSpecificConfig>();
    private Object selected;

    public DbTypeSelectorModel(String defaultType) {
        Pattern pattern = Pattern.compile(".*/" + defaultType);
        Set<String> dbTypes = new TreeSet<String>(Config.getBuiltInDatabaseTypes(Config.getLoadedFromJar()));
        for (String dbType : dbTypes) {
            DbSpecificConfig config = new DbSpecificConfig(dbType);
            dbConfigs.add(config);

            if (pattern.matcher(dbType).matches()) {
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

