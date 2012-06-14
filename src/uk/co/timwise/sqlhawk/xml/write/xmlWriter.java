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
package uk.co.timwise.sqlhawk.xml.write;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.util.LineWriter;

public class xmlWriter {
	public static void writeXml(File outputDir, Database db)
			throws ParserConfigurationException, UnsupportedEncodingException,
			FileNotFoundException, TransformerException, IOException {
		LineWriter out;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		Element rootNode = document.createElement("database");
		document.appendChild(rootNode);
		DOMUtil.appendAttribute(rootNode, "name", db.getName());
		if (db.getSchema() != null)
			DOMUtil.appendAttribute(rootNode, "schema", db.getSchema());
		DOMUtil.appendAttribute(rootNode, "type", db.getDbms());

		Collection<Table> tablesAndViews = db.getTablesAndViews();
		XmlTableFormatter.getInstance().appendTables(rootNode, tablesAndViews);

		String xmlName = db.getName();

		// some dbNames have path info in the name...strip it
		xmlName = new File(xmlName).getName();

		if (db.getSchema() != null)
			xmlName += '.' + db.getSchema();

		out = new LineWriter(new File(outputDir, xmlName + ".xml"), "UTF-8");
		document.getDocumentElement().normalize();
		DOMUtil.printDOM(document, out);
		out.close();
	}
}
