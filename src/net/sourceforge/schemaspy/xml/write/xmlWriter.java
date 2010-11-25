package net.sourceforge.schemaspy.xml.write;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.DOMUtil;
import net.sourceforge.schemaspy.util.LineWriter;
import net.sourceforge.schemaspy.view.XmlTableFormatter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
