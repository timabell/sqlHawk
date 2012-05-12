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

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import uk.co.timwise.sqlhawk.util.DbSpecificConfig;

public class DbConfigPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JComboBox databaseTypeSelector;
    private final DbConfigTableModel model = new DbConfigTableModel();
    private JTable table;

    public DbConfigPanel() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            {
                setDefaultRenderer(Boolean.TYPE, getDefaultRenderer(Boolean.class));
                setDefaultEditor(Boolean.TYPE, getDefaultEditor(Boolean.class));
                setDefaultRenderer(Number.class, getDefaultRenderer(String.class));
                setDefaultEditor(Number.class, getDefaultEditor(String.class));

                DirectoryCellEditor fileEditor = new DirectoryCellEditor(model, new File("/"));
                setDefaultRenderer(File.class, fileEditor);
                setDefaultEditor(File.class, fileEditor);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                TableCellRenderer renderer;

                if (column == 0)
                    renderer = super.getCellRenderer(row, column);
                else
                    renderer = getDefaultRenderer(model.getClass(row));
                if (renderer instanceof JComponent)
                    ((JComponent)renderer).setToolTipText(model.getDescription(row));
                return renderer;
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                return getDefaultEditor(model.getClass(row));
            }
        };

        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                TableColumn paramColumn = table.getColumnModel().getColumn(0);
                paramColumn.setPreferredWidth(UiUtils.getPreferredColumnWidth(table, paramColumn) + 4);
                paramColumn.setMaxWidth(paramColumn.getPreferredWidth());
                table.sizeColumnsToFit(0);
            }
        });

        setLayout(new BorderLayout());
        JScrollPane scroller = new JScrollPane(table);
        scroller.setViewportBorder(null);
        add(scroller, BorderLayout.CENTER);

        add(getDatabaseTypeSelector(), BorderLayout.NORTH);
    }

    /**
     * This method initializes databaseTypeSelector
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox getDatabaseTypeSelector() {
        if (databaseTypeSelector == null) {
            DbTypeSelectorModel selectorModel = new DbTypeSelectorModel("ora");
            databaseTypeSelector = new JComboBox(selectorModel);
            databaseTypeSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt) {
                    if (evt.getStateChange() == ItemEvent.SELECTED)
                        model.setDbSpecificConfig((DbSpecificConfig)evt.getItem());
                }
            });

            DbSpecificConfig selected = (DbSpecificConfig)selectorModel.getSelectedItem();
            if (selected != null)
                model.setDbSpecificConfig(selected);
        }
        return databaseTypeSelector;
    }
}