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

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.util.DOMUtil;
import uk.co.timwise.sqlhawk.util.LineWriter;
import uk.co.timwise.sqlhawk.view.XmlTableFormatter;

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

		out = new LineWriter(new File(outputDir, xmlName + ".xml"), Config.DOT_CHARSET);
		document.getDocumentElement().normalize();
		DOMUtil.printDOM(document, out);
		out.close();
	}
}
