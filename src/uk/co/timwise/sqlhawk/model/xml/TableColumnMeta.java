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
package uk.co.timwise.sqlhawk.model.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Additional metadata about a column as expressed in XML instead of from
 * the database.
 */
public class TableColumnMeta {
    private final String name;
    private final String comments;
    private final boolean isPrimary;
    private final List<ForeignKeyMeta> foreignKeys = new ArrayList<ForeignKeyMeta>();
    private final boolean isExcluded;
    private final boolean isAllExcluded;
    private final boolean isImpliedParentsDisabled;
    private final boolean isImpliedChildrenDisabled;
    private static final Logger logger = Logger.getLogger(TableColumnMeta.class.getName());

    TableColumnMeta(Node colNode) {
        NamedNodeMap attribs = colNode.getAttributes();
        String tmp;

        name = attribs.getNamedItem("name").getNodeValue();
        Node node = attribs.getNamedItem("comments");
        if (node != null) {
            tmp = node.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }

        node = attribs.getNamedItem("primaryKey");
        if (node != null) {
            isPrimary = evalBoolean(node.getNodeValue());
        } else {
            isPrimary = false;
        }

        node = attribs.getNamedItem("disableImpliedKeys");
        if (node != null) {
            tmp = node.getNodeValue().trim().toLowerCase();
            if (tmp.equals("to")) {
                isImpliedChildrenDisabled = true;
                isImpliedParentsDisabled  = false;
            } else if (tmp.equals("from")) {
                isImpliedParentsDisabled  = true;
                isImpliedChildrenDisabled = false;
            } else if (tmp.equals("all")) {
                isImpliedChildrenDisabled = isImpliedParentsDisabled = true;
            } else {
                isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
            }
        } else {
            isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
        }

        node = attribs.getNamedItem("disableDiagramAssociations");
        if (node != null) {
            tmp = node.getNodeValue().trim().toLowerCase();
            if (tmp.equals("all")) {
                isAllExcluded = true;
                isExcluded = true;
            } else if (tmp.equals("exceptdirect")) {
                isAllExcluded = false;
                isExcluded = true;
            } else {
                isAllExcluded = false;
                isExcluded = false;
            }
        } else {
            isAllExcluded = false;
            isExcluded = false;
        }

        logger.finer("Found XML column metadata for " + name +
                    " isPrimaryKey: " + isPrimary +
                    " comments: " + comments);

        NodeList fkNodes = ((Element)colNode.getChildNodes()).getElementsByTagName("foreignKey");

        for (int i = 0; i < fkNodes.getLength(); ++i) {
            Node fkNode = fkNodes.item(i);
            foreignKeys.add(new ForeignKeyMeta(fkNode));
        }
    }

    private boolean evalBoolean(String exp) {
        if (exp == null)
            return false;

        exp = exp.trim().toLowerCase();
        return exp.equals("true") || exp.equals("yes") || exp.equals("1");
    }

    public String getName() {
        return name;
    }

    public String getComments() {
        return comments;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public List<ForeignKeyMeta> getForeignKeys() {
        return foreignKeys;
    }

    public boolean isExcluded() {
        return isExcluded;
    }

    public boolean isAllExcluded() {
        return isAllExcluded;
    }

    public boolean isImpliedParentsDisabled() {
        return isImpliedParentsDisabled;
    }

    public boolean isImpliedChildrenDisabled() {
        return isImpliedChildrenDisabled;
    }
}