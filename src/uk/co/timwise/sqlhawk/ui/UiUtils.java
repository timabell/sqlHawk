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

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class UiUtils {
    public static int getPreferredColumnWidth(JTable table, TableColumn col) {
        return Math.max(getPreferredColumnHeaderWidth(table, col), getWidestCellInColumn(table, col));
    }

    public static int getPreferredColumnHeaderWidth(JTable table, TableColumn col) {
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null)
            return 0;
        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        return comp.getPreferredSize().width;
    }

    public static int getWidestCellInColumn(JTable table, TableColumn col) {
        int column = col.getModelIndex();
        int max = 0;

        for (int row = 0; row < table.getRowCount(); ++row) {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
            max = Math.max(comp.getPreferredSize().width, max);
        }

        return max;
    }
}
