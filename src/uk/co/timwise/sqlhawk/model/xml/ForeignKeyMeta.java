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
package uk.co.timwise.sqlhawk.model.xml;

import java.util.logging.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Additional metadata about a foreign key relationship as expressed in XML
 * instead of from the database.
 */
public class ForeignKeyMeta {
	private final String tableName;
	private final String columnName;
	private final String remoteSchema;
	private final static Logger logger = Logger.getLogger(ForeignKeyMeta.class.getName());

	ForeignKeyMeta(Node foreignKeyNode) {
		NamedNodeMap attribs = foreignKeyNode.getAttributes();
		Node node = attribs.getNamedItem("table");
		if (node == null)
			throw new IllegalStateException("XML foreignKey definition requires 'table' attribute");
		tableName = node.getNodeValue();
		node = attribs.getNamedItem("column");
		if (node == null)
			throw new IllegalStateException("XML foreignKey definition requires 'column' attribute");
		columnName = node.getNodeValue();
		node = attribs.getNamedItem("remoteSchema");
		if (node != null) {
			remoteSchema = node.getNodeValue();
		} else {
			remoteSchema = null;
		}

		logger.finer("Found XML FK metadata for " + tableName + "." + columnName +
				" remoteSchema: " + remoteSchema);
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public String getRemoteSchema() {
		return remoteSchema;
	}

	@Override
	public String toString() {
		return tableName + '.' + columnName;
	}
}