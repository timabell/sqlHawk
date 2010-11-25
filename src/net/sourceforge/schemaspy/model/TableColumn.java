/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy.model;

import java.sql.DatabaseMetaData;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class TableColumn {
    private Table table;
    private String name;
    private Object id;
    private String type;
    private int length;
    private int decimalDigits;
    private String detailedSize;
    private boolean isNullable;
    private       boolean isAutoUpdated;
    private       Boolean isUnique;
    private Object defaultValue;
    private       String comments;
    private final Map<TableColumn, ForeignKeyConstraint> parents = new HashMap<TableColumn, ForeignKeyConstraint>();
    private final Map<TableColumn, ForeignKeyConstraint> children = new TreeMap<TableColumn, ForeignKeyConstraint>(new ColumnComparator());
    private boolean allowImpliedParents = true;
    private boolean allowImpliedChildren = true;
    private boolean isExcluded = false;
    private boolean isAllExcluded = false;

    public TableColumn() {
    }
    
    /**
     * Create a column associated with a table.
     *
     * @param table
     * @param colMeta
     */
    public TableColumn(Table table, String name, String comments) {
        this.table = table;
        this.name = name;
        id = null;
        type = "Unknown";
        length = 0;
        decimalDigits = 0;
        detailedSize = "";
        isNullable = false;
        isAutoUpdated = false;
        defaultValue = null;
        this.comments = comments;
    }

    /**
     * Returns the {@link Table} that this column belongs to.
     *
     * @return
     */
    public Table getTable() {
        return table;
    }

    /**
     * Returns the column's name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ID of the column or <code>null</code> if the database doesn't support the concept.
     *
     * @return
     */
    public Object getId() {
        return id;
    }

    /**
     * Type of the column.
     * See {@link DatabaseMetaData#getColumns(String, String, String, String)}'s <code>TYPE_NAME</code>.
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Length of the column.
     * See {@link DatabaseMetaData#getColumns(String, String, String, String)}'s <code>BUFFER_LENGTH</code>,
     * or if that's <code>null</code>, <code>COLUMN_SIZE</code>.
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     * Decimal digits of the column.
     * See {@link DatabaseMetaData#getColumns(String, String, String, String)}'s <code>DECIMAL_DIGITS</code>.
     *
     * @return
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }

    /**
     * String representation of length with optional decimal digits (if decimal digits &gt; 0).
     *
     * @return
     */
    public String getDetailedSize() {
        return detailedSize;
    }

    /**
     * Returns <code>true</code> if null values are allowed
     *
     * @return
     */
    public boolean isNullable() {
        return isNullable;
    }

    /**
     * See {@link java.sql.ResultSetMetaData#isAutoIncrement(int)}
     *
     * @return
     */
    public boolean isAutoUpdated() {
        return isAutoUpdated;
    }

    /**
     * setIsAutoUpdated
     *
     * @param isAutoUpdated boolean
     */
    public void setIsAutoUpdated(boolean isAutoUpdated) {
        this.isAutoUpdated = isAutoUpdated;
    }

    /**
     * Returns <code>true</code> if this column can only contain unique values
     *
     * @return
     */
    public boolean isUnique() {
        if (isUnique == null) {
            // see if there's a unique index on this column by itself
            for (TableIndex index : table.getIndexes()) {
                if (index.isUnique()) {
                    List<TableColumn> indexColumns = index.getColumns();
                    if (indexColumns.size() == 1 && indexColumns.contains(this)) {
                        isUnique = true;
                        break;
                    }
                }
            }

            if (isUnique == null) {
                // if it's a single PK column then it's unique
                isUnique = table.getPrimaryColumns().size() == 1 && isPrimary();
            }
        }

        return isUnique;
    }

    /**
     * Returns <code>true</code> if this column is a primary key
     *
     * @return
     */
    public boolean isPrimary() {
        return table.getPrimaryColumns().contains(this);
    }

    /**
     * Returns <code>true</code> if this column points to another table's primary key.
     *
     * @return
     */
    public boolean isForeignKey() {
        return !parents.isEmpty();
    }

    /**
     * Returns the value that the database uses for this column if one isn't provided.
     *
     * @return
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return Comments associated with this column, or <code>null</code> if none.
     */
    public String getComments() {
        return comments;
    }

    /**
     * See {@link #getComments()}
     * @param comments
     */
    public void setComments(String comments) {
        this.comments = (comments == null || comments.trim().length() == 0) ? null : comments.trim();
    }

    /**
     * Returns <code>true</code> if this column is to be excluded from relationship diagrams.
     * Unless {@link #isAllExcluded()} is true this column will be included in the detailed
     * diagrams of the containing table.
     *
     * <p>This is typically an attempt to reduce clutter that can be introduced when many tables
     * reference a given column.
     *
     * @return
     */
    public boolean isExcluded() {
        return isExcluded;
    }

    /**
     * Returns <code>true</code> if this column is to be excluded from all relationships in
     * relationship diagrams.  This includes the detailed diagrams of the containing table.
     *
     * <p>This is typically an attempt to reduce clutter that can be introduced when many tables
     * reference a given column.
     *
     * @return
     */
    public boolean isAllExcluded() {
        return isAllExcluded;
    }

    /**
     * Add a parent column (PK) to this column (FK) via the associated constraint
     *
     * @param parent
     * @param constraint
     */
    public void addParent(TableColumn parent, ForeignKeyConstraint constraint) {
        parents.put(parent, constraint);
        table.addedParent();
    }

    /**
     * Remove the specified parent column from this column
     *
     * @param parent
     */
    public void removeParent(TableColumn parent) {
        parents.remove(parent);
    }

    /**
     * Disassociate all parents from this column
     */
    public void unlinkParents() {
        for (TableColumn parent : parents.keySet()) {
            parent.removeChild(this);
        }
        parents.clear();
    }

    /**
     * Returns the {@link Set} of all {@link TableColumn parents} associated with this column
     *
     * @return
     */
    public Set<TableColumn> getParents() {
        return parents.keySet();
    }

    /**
     * Returns the constraint that connects this column to the specified column (this 'child' column to specified 'parent' column)
     */
    public ForeignKeyConstraint getParentConstraint(TableColumn parent) {
        return parents.get(parent);
    }

    /**
     * Removes a parent constraint and returns it, or null if there are no parent constraints
     *
     * @return the removed {@link ForeignKeyConstraint}
     */
    public ForeignKeyConstraint removeAParentFKConstraint() {
        for (TableColumn relatedColumn : parents.keySet()) {
            ForeignKeyConstraint constraint = parents.remove(relatedColumn);
            relatedColumn.removeChild(this);
            return constraint;
        }

        return null;
    }

    /**
     * Remove one child {@link ForeignKeyConstraint} that points to this column.
     *
     * @return the removed constraint, or <code>null</code> if none were available to be removed
     */
    public ForeignKeyConstraint removeAChildFKConstraint() {
        for (TableColumn relatedColumn : children.keySet()) {
            ForeignKeyConstraint constraint = children.remove(relatedColumn);
            relatedColumn.removeParent(this);
            return constraint;
        }

        return null;
    }

    /**
     * Add a child column (FK) to this column (PK) via the associated constraint
     *
     * @param child
     * @param constraint
     */
    public void addChild(TableColumn child, ForeignKeyConstraint constraint) {
        children.put(child, constraint);
        table.addedChild();
    }

    /**
     * Remove the specified child column from this column
     *
     * @param child
     */
    public void removeChild(TableColumn child) {
        children.remove(child);
    }

    /**
     * Disassociate all children from this column
     */
    public void unlinkChildren() {
        for (TableColumn child : children.keySet())
            child.removeParent(this);
        children.clear();
    }

    /**
     * Returns <code>Set</code> of <code>TableColumn</code>s that have a real (or implied) foreign key that
     * references this <code>TableColumn</code>.
     * @return Set
     */
    public Set<TableColumn> getChildren() {
        return children.keySet();
    }

    /**
     * returns the constraint that connects the specified column to this column
     * (specified 'child' to this 'parent' column)
     */
    public ForeignKeyConstraint getChildConstraint(TableColumn child) {
        return children.get(child);
    }

    /**
     * Returns <code>true</code> if tableName.columnName matches the supplied
     * regular expression.
     *
     * @param regex
     * @return
     */
    public boolean matches(Pattern regex) {
        return regex.matcher(getTable().getName() + '.' + getName()).matches();
    }

    /**
     * Returns the name of this column.
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Two {@link TableColumn}s are considered equal if their tables and names match.
     */
    private class ColumnComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getTable().compareTo(column2.getTable());
            if (rc == 0)
                rc = column1.getName().compareToIgnoreCase(column2.getName());
            return rc;
        }
    }

    /**
     * Returns <code>true</code> if this column is permitted to be an implied FK
     * (based on name/type/size matches to PKs).
     *
     * @return
     */
    public boolean allowsImpliedParents() {
        return allowImpliedParents;
    }

    /**
     * Returns <code>true</code> if this column is permitted to be a PK to an implied FK
     * (based on name/type/size matches to PKs).
     *
     * @return
     */
    public boolean allowsImpliedChildren() {
        return allowImpliedChildren;
    }

	public void setType(String type) {
		this.type = type;
	}

	public void setDecimalDigits(int decimalDigits) {
		this.decimalDigits = decimalDigits;
	}

	public void setDetailedSize(String detailedSize) {
		this.detailedSize = detailedSize;
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setAllExcluded(boolean isAllExcluded) {
		this.isAllExcluded = isAllExcluded;
	}

	public void setExcluded(boolean isExcluded) {
		this.isExcluded = isExcluded; 
	}

	public void setAllowsImpliedParents(boolean allowImpliedParents) {
		this.allowImpliedParents = allowImpliedParents;
	}

	public void setAllowsImpliedChildren(boolean allowImpliedChildren) {
		this.allowImpliedChildren = allowImpliedChildren;
	}
}