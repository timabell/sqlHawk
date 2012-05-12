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
package uk.co.timwise.sqlhawk.db.read;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;
import uk.co.timwise.sqlhawk.model.TableIndex;
import uk.co.timwise.sqlhawk.model.xml.ForeignKeyMeta;
import uk.co.timwise.sqlhawk.model.xml.TableColumnMeta;
import uk.co.timwise.sqlhawk.model.xml.TableMeta;


public class TableReader {
	protected Database db;
	private Table table;
	protected Properties properties;
	private DatabaseMetaData meta;
	private final static Logger logger = Logger.getLogger(TableReader.class.getName());
	private Pattern invalidIdentifierPattern;
	private final boolean fineEnabled = logger.isLoggable(Level.FINE);

	/**
	 * Construct a table that knows everything about the database table's metadata
	 *
	 * @param db
	 * @param schema
	 * @param name
	 * @param comments
	 * @param properties
	 * @param excludeIndirectColumns
	 * @param excludeColumns
	 * @param meta 
	 * @throws SQLException
	 */
	public Table ReadTable(Database db, String schema, String name, String comments, Properties properties, Pattern excludeIndirectColumns, Pattern excludeColumns, DatabaseMetaData meta, DbReader dbReader) throws SQLException {
		this.table = new Table(schema, name, comments);
		this.db = db;
		this.properties = properties;
		this.meta = meta;
		logger.fine("Creating " + getClass().getSimpleName().toLowerCase() + " " +
				schema == null ? name : (schema + '.' + name));
		setComments(comments);
		initColumns(excludeIndirectColumns, excludeColumns);
		initIndexes(dbReader);
		initPrimaryKeys(meta);
		return table;
	}

	public void setMeta(DatabaseMetaData meta){
		this.meta = meta;
	}

	/**
	 * "Connect" all of this table's foreign keys to their referenced primary keys
	 * (and, in some cases, do the reverse as well).
	 *
	 * @param tables
	 * @param excludeIndirectColumns
	 * @param excludeColumns
	 * @throws SQLException
	 */
	public void connectForeignKeys(Table table, Map<String, Table> tables, Pattern excludeIndirectColumns, Pattern excludeColumns, DbReader dbReader) throws SQLException {
		ResultSet rs = null;
		this.table = table;
		try {
			rs = meta.getImportedKeys(null, table.getSchema(), table.getName());

			while (rs.next()) {
				if (fineEnabled)
					logger.finest("Adding foreign key " + rs.getString("FK_NAME"));
				addForeignKey(rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
						rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"),
						rs.getString("PKCOLUMN_NAME"),
						rs.getInt("UPDATE_RULE"), rs.getInt("DELETE_RULE"),
						tables, excludeIndirectColumns, excludeColumns, dbReader);
			}
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * "Connect" this table's exported keys to their referenced primary keys.
	 * Will load tables from other schema.
	 *
	 * @param tables
	 * @param excludeIndirectColumns
	 * @param excludeColumns
	 * @throws SQLException
	 */
	public void connectExportedKeys(Table table, Map<String, Table> tables, Pattern excludeIndirectColumns, Pattern excludeColumns, DbReader reader) throws SQLException {
		ResultSet rs = null;

		// also try to find all of the 'remote' tables in other schemas that
		// point to our primary keys (not necessary in the normal case
		// as we infer this from the opposite direction)
		if (table.getSchema() != null) {
			try {
				rs = meta.getExportedKeys(null, table.getSchema(), table.getName());

				while (rs.next()) {
					String otherSchema = rs.getString("FKTABLE_SCHEM");
					if (!table.getSchema().equals(otherSchema))
						reader.addRemoteTable(otherSchema, rs.getString("FKTABLE_NAME"), table.getSchema(), properties, excludeIndirectColumns, excludeColumns);
				}
			} finally {
				if (rs != null)
					rs.close();
			}
		}
	}

	/**
	 * @param rs ResultSet from {@link DatabaseMetaData#getImportedKeys(String, String, String)}
	 * rs.getString("FK_NAME");
	 * rs.getString("FKCOLUMN_NAME");
	 * rs.getString("PKTABLE_SCHEM");
	 * rs.getString("PKTABLE_NAME");
	 * rs.getString("PKCOLUMN_NAME");
	 * @param tables Map
	 * @param db
	 * @throws SQLException
	 */
	protected void addForeignKey(String fkName, String fkColName,
			String pkTableSchema, String pkTableName, String pkColName,
			int updateRule, int deleteRule,
			Map<String, Table> tables,
			Pattern excludeIndirectColumns, Pattern excludeColumns, DbReader dbReader) throws SQLException {
		if (fkName == null)
			return;

		ForeignKeyConstraint foreignKey = table.getForeignKey(fkName);

		if (foreignKey == null) {
			foreignKey = new ForeignKeyConstraint(table, fkName, updateRule, deleteRule);
			table.addForeignKey(fkName, foreignKey);
		}

		TableColumn childColumn = table.getColumn(fkColName);
		if (childColumn != null) {
			foreignKey.addChildColumn(childColumn);

			Table parentTable = tables.get(pkTableName);
			String parentSchema = pkTableSchema;
			String baseSchema = Config.getInstance().getSchema();

			// if named table doesn't exist in this schema
			// or exists here but really referencing same named table in another schema
			if (parentTable == null ||
					(baseSchema != null && parentSchema != null &&
					!baseSchema.equals(parentSchema))) {
				parentTable = dbReader.addRemoteTable(parentSchema, pkTableName, baseSchema,
						properties, excludeIndirectColumns, excludeColumns);
			}

			if (parentTable != null) {
				TableColumn parentColumn = parentTable.getColumn(pkColName);
				if (parentColumn != null) {
					foreignKey.addParentColumn(parentColumn);

					childColumn.addParent(parentColumn, foreignKey);
					parentColumn.addChild(childColumn, foreignKey);
				} else {
					logger.warning("Couldn't add FK '" + foreignKey.getName() + "' to table '" + this +
							"' - Column '" + pkColName + "' doesn't exist in table '" + parentTable + "'");
				}
			} else {
				logger.warning("Couldn't add FK '" + foreignKey.getName() + "' to table '" + this +
						"' - Unknown Referenced Table '" + pkTableName + "'");
			}
		} else {
			logger.warning("Couldn't add FK '" + foreignKey.getName() + "' to table '" + this +
					"' - Column '" + fkColName + "' doesn't exist");
		}
	}

	/**
	 * @param meta
	 * @throws SQLException
	 */
	private void initPrimaryKeys(DatabaseMetaData meta) throws SQLException {
		if (properties == null)
			return;

		ResultSet rs = null;

		try {
			rs = meta.getPrimaryKeys(null, table.getSchema(), table.getName());

			while (rs.next())
				setPrimaryColumn(rs);
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * @param rs
	 * @throws SQLException
	 */
	private void setPrimaryColumn(ResultSet rs) throws SQLException {
		String pkName = rs.getString("PK_NAME");
		if (pkName == null)
			return;

		TableIndex index = table.getIndex(pkName);
		if (index != null) {
			index.setIsPrimaryKey(true);
		}

		String columnName = rs.getString("COLUMN_NAME");

		table.setPrimaryColumn(table.getColumn(columnName));
	}

	/**
	 * @param excludeIndirectColumns
	 * @param excludeColumns
	 * @throws SQLException
	 */
	private void initColumns(Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
		ResultSet rs = null;

		synchronized (Table.class) {
			try {
				rs = meta.getColumns(null, table.getSchema(), table.getName(), "%");

				while (rs.next())
					addColumn(rs, excludeIndirectColumns, excludeColumns);
			} catch (SQLException exc) {
				class ColumnInitializationFailure extends SQLException {
					private static final long serialVersionUID = 1L;

					public ColumnInitializationFailure(SQLException failure) {
						super("Failed to collect column details for " + (table.isView() ? "view" : "table") + " '" + table.getName() + "' in schema '" + table.getSchema() + "'");
						initCause(failure);
					}
				}

				throw new ColumnInitializationFailure(exc);
			} finally {
				if (rs != null)
					rs.close();
			}
		}

		if (!table.isView() && !table.isRemote())
			initColumnAutoUpdate(false);
	}

	/**
	 * @param forceQuotes
	 * @throws SQLException
	 */
	private void initColumnAutoUpdate(boolean forceQuotes) throws SQLException {
		ResultSet rs = null;
		PreparedStatement stmt = null;

		// we've got to get a result set with all the columns in it
		// so we can ask if the columns are auto updated
		// Ugh!!!  Should have been in DatabaseMetaData instead!!!
		StringBuilder sql = new StringBuilder("select * from ");
		if (table.getSchema() != null) {
			sql.append(table.getSchema());
			sql.append('.');
		}

		if (forceQuotes) {
			String quote = meta.getIdentifierQuoteString().trim();
			sql.append(quote + table.getName() + quote);
		} else
			sql.append(getQuotedIdentifier(table.getName()));

		sql.append(" where 0 = 1");

		try {
			stmt = meta.getConnection().prepareStatement(sql.toString());
			rs = stmt.executeQuery();

			ResultSetMetaData rsMeta = rs.getMetaData();
			for (int i = rsMeta.getColumnCount(); i > 0; --i) {
				TableColumn column = table.getColumn(rsMeta.getColumnName(i));
				column.setIsAutoUpdated(rsMeta.isAutoIncrement(i));
			}
		} catch (SQLException exc) {
			if (forceQuotes) {
				// don't completely choke just because we couldn't do this....
				logger.warning("Failed to determine auto increment status: " + exc);
				logger.warning("SQL: " + sql.toString());
			} else {
				initColumnAutoUpdate(true);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
		}
	}

	/**
	 * @param rs - from {@link DatabaseMetaData#getColumns(String, String, String, String)}
	 * @param excludeIndirectColumns
	 * @param excludeColumns
	 * @throws SQLException
	 */
	protected void addColumn(ResultSet rs, Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
		String columnName = rs.getString("COLUMN_NAME");

		if (columnName == null)
			return;

		if (table.getColumn(columnName) == null) {
			TableColumn column = TableColumnReader.ReadTableColumn(table, rs, excludeIndirectColumns, excludeColumns);
			table.getColumnMap().put(column.getName(), column);
		}
	}

	/**
	 * Add a column that's defined in xml metadata.
	 * Assumes that a column named colMeta.getName() doesn't already exist in <code>columns</code>.
	 * @param colMeta
	 * @return
	 */
	protected TableColumn addColumn(TableColumnMeta colMeta) {
		TableColumn column = new TableColumn();
		TableColumnReader columnReader = new TableColumnReader();
		columnReader.update(column, colMeta);
		table.getColumnMap().put(column.getName(), column);
		return column;
	}

	/**
	 * Initialize index information
	 *
	 * @throws SQLException
	 */
	private void initIndexes(DbReader dbReader) throws SQLException {
		if (table.isView() || table.isRemote())
			return;

		// first try to initialize using the index query spec'd in the .properties
		// do this first because some DB's (e.g. Oracle) do 'bad' things with getIndexInfo()
		// (they try to do a DDL analyze command that has some bad side-effects)
		if (initIndexes(properties.getProperty("selectIndexesSql"), dbReader))
			return;

		// couldn't, so try the old fashioned approach
		ResultSet rs = null;

		try {
			rs = meta.getIndexInfo(null, table.getSchema(), table.getName(), false, true);

			while (rs.next()) {
				if (rs.getShort("TYPE") != DatabaseMetaData.tableIndexStatistic)
					addIndex(rs);
			}
		} catch (SQLException exc) {
			logger.warning("Unable to extract index info for table '" + table.getName() + "' in schema '" + table.getSchema() + "': " + exc);
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Try to initialize index information based on the specified SQL
	 *
	 * @return boolean <code>true</code> if it worked, otherwise <code>false</code>
	 */
	private boolean initIndexes(String selectIndexesSql, DbReader dbReader) {
		if (selectIndexesSql == null)
			return false;

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = dbReader.prepareStatement(selectIndexesSql, table.getName());
			rs = stmt.executeQuery();

			while (rs.next()) {
				if (rs.getShort("TYPE") != DatabaseMetaData.tableIndexStatistic)
					addIndex(rs);
			}
		} catch (SQLException sqlException) {
			logger.warning("Failed to query index information with SQL: " + selectIndexesSql);
			logger.warning(sqlException.toString());
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
			if (stmt != null)  {
				try {
					stmt.close();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}

		return true;
	}

	/**
	 * @param rs
	 * @throws SQLException
	 */
	private void addIndex(ResultSet rs) throws SQLException {
		String indexName = rs.getString("INDEX_NAME");

		if (indexName == null)
			return;

		TableIndex index = table.getIndex(indexName);

		if (index == null) {
			index = TableIndexReader.ReadTableIndex(rs);
			table.addIndex(index.getName(), index);
		}

		index.addColumn(table.getColumn(rs.getString("COLUMN_NAME")), rs.getString("ASC_OR_DESC"));
	}

	/**
	 * Sets the comments that are associated with this table
	 *
	 * @param comments
	 */
	public void setComments(String comments) {
		String cmts = (comments == null || comments.trim().length() == 0) ? null : comments.trim();

		// MySQL's InnoDB engine does some insane crap of storing erroneous details in
		// with table comments.  Here I attempt to strip the "crap" out without impacting
		// other databases.  Ideally this should happen in selectColumnCommentsSql (and
		// therefore isolate it to MySQL), but it's a bit too complex to do cleanly.
		if (cmts != null) {
			int crapIndex = cmts.indexOf("; InnoDB free: ");
			if (crapIndex == -1)
				crapIndex = cmts.startsWith("InnoDB free: ") ? 0 : -1;
			if (crapIndex != -1) {
				cmts = cmts.substring(0, crapIndex).trim();
				cmts = cmts.length() == 0 ? null : cmts;
			}
		}

		table.setComments(cmts);
	}

	/**
	 * Fetch the number of rows contained in this table.
	 *
	 * returns -1 if unable to successfully fetch the row count
	 *
	 * @param db Database
	 * @return int
	 * @throws SQLException
	 */
	protected long fetchNumRows(DbReader dbReader) {
		if (properties == null) // some "meta" tables don't have associated properties
			return 0;

		SQLException originalFailure = null;

		String sql = properties.getProperty("selectRowCountSql");
		if (sql != null) {
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try {
				stmt = dbReader.prepareStatement(sql, table.getName());
				rs = stmt.executeQuery();

				while (rs.next()) {
					return rs.getLong("row_count");
				}
			} catch (SQLException sqlException) {
				// don't die just because this failed
				originalFailure = sqlException;
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException exc) {}
				}
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException exc) {}
				}
			}
		}

		// if we get here then we either didn't have custom SQL or it didn't work
		try {
			// '*' should work best for the majority of cases
			return fetchNumRows("count(*)", false, dbReader);
		} catch (SQLException try2Exception) {
			try {
				// except nested tables...try using '1' instead
				return fetchNumRows("count(1)", false, dbReader);
			} catch (SQLException try3Exception) {
				logger.warning("Unable to extract the number of rows for table " + table.getName() + ", using '-1'");
				if (originalFailure != null)
					logger.warning(originalFailure.toString());
				logger.warning(try2Exception.toString());
				logger.warning(try3Exception.toString());
				return -1;
			}
		}
	}

	protected long fetchNumRows(String clause, boolean forceQuotes, DbReader dbReader) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		StringBuilder sql = new StringBuilder("select ");
		sql.append(clause);
		sql.append(" from ");
		if (table.getSchema() != null) {
			sql.append(table.getSchema());
			sql.append('.');
		}

		if (forceQuotes) {
			String quote = meta.getIdentifierQuoteString().trim();
			sql.append(quote + table.getName() + quote);
		} else
			sql.append(getQuotedIdentifier(table.getName()));

		try {
			stmt = dbReader.prepareStatement(sql.toString(), null);
			rs = stmt.executeQuery();
			while (rs.next()) {
				return rs.getLong(1);
			}
			return -1;
		} catch (SQLException exc) {
			if (forceQuotes) // we tried with and w/o quotes...fail this attempt
				throw exc;

			return fetchNumRows(clause, true, dbReader);
		} finally {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
		}
	}

	/**
	 * Update the table with the specified XML-derived metadata
	 *
	 * @param tableMeta
	 */
	public void update(TableMeta tableMeta) {
		String newComments = tableMeta.getComments();
		if (newComments != null) {
			table.setComments(newComments);
		}

		for (TableColumnMeta colMeta : tableMeta.getColumns()) {
			TableColumn col = table.getColumn(colMeta.getName());
			if (col == null) {
				if (tableMeta.getRemoteSchema() == null) {
					logger.warning("Unrecognized column '" + colMeta.getName() + "' for table '" + table.getName() + '\'');
					continue;
				}

				col = addColumn(colMeta);
			}

			// update the column with the changes
			TableColumnReader columnReader = new TableColumnReader();
			columnReader.update(col, colMeta);
		}
	}

	/**
	 * Same as {@link #connectForeignKeys(Map, Database, Properties, Pattern, Pattern)},
	 * but uses XML-based metadata
	 *
	 * @param tableMeta
	 * @param tables
	 * @param remoteTables
	 */
	public void connect(TableMeta tableMeta, Map<String, Table> tables, Map<String, Table> remoteTables) {
		for (TableColumnMeta colMeta : tableMeta.getColumns()) {
			TableColumn col = table.getColumn(colMeta.getName());

			// go thru the new foreign key defs and associate them with our columns
			for (ForeignKeyMeta fk : colMeta.getForeignKeys()) {
				Table parent = fk.getRemoteSchema() == null ? tables.get(fk.getTableName())
						: remoteTables.get(fk.getRemoteSchema() + '.' + fk.getTableName());
				if (parent != null) {
					TableColumn parentColumn = parent.getColumn(fk.getColumnName());

					if (parentColumn == null) {
						logger.warning(parent.getName() + '.' + fk.getColumnName() + " doesn't exist");
					} else {
						/**
						 * Merely instantiating a foreign key constraint ties it
						 * into its parent and child columns (& therefore their tables)
						 */
						new ForeignKeyConstraint(parentColumn, col) {
							@Override
							public String getName() {
								return "Defined in XML";
							}
						};
					}
				} else {
					logger.warning("Undefined table '" + fk.getTableName() + "' referenced by '" + table.getName() + '.' + col.getName() + '\'');
				}
			}
		}
	}
	/**
	 * Return <code>id</code> quoted if required, otherwise return <code>id</code>
	 *
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	private String getQuotedIdentifier(String id) throws SQLException {
		// look for any character that isn't valid (then matcher.find() returns true)
		Matcher matcher = getInvalidIdentifierPattern().matcher(id);

		boolean quotesRequired = matcher.find() || db.getKeywords().contains(id.toUpperCase());

		if (quotesRequired) {
			// name contains something that must be quoted
			String quote = meta.getIdentifierQuoteString().trim();
			return quote + id + quote;
		}

		// no quoting necessary
		return id;
	}

	/**
	 * Return a <code>Pattern</code> whose matcher will return <code>true</code>
	 * when run against an identifier that contains a character that is not
	 * acceptable by the database without being quoted.
	 */
	private Pattern getInvalidIdentifierPattern() throws SQLException {
		if (invalidIdentifierPattern == null) {
			String validChars = "a-zA-Z0-9_";
			String reservedRegexChars = "-&^";
			String extraValidChars = meta.getExtraNameCharacters();
			for (int i = 0; i < extraValidChars.length(); ++i) {
				char ch = extraValidChars.charAt(i);
				if (reservedRegexChars.indexOf(ch) >= 0)
					validChars += "\\";
				validChars += ch;
			}

			invalidIdentifierPattern = Pattern.compile("[^" + validChars + "]");
		}

		return invalidIdentifierPattern;
	}

}