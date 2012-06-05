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
package uk.co.timwise.sqlhawk.html;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;
import uk.co.timwise.sqlhawk.util.LineWriter;

public class HtmlFormatter {
	protected final boolean encodeComments       = Config.getInstance().isEncodeCommentsEnabled();
	protected final boolean displayNumRows       = Config.getInstance().isNumRowsEnabled();

	protected HtmlFormatter() {
	}

	protected void writeHeader(Database db, Table table, String text, boolean showOrphans, List<String> javascript, LineWriter out) throws IOException {
		out.writeln("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>");
		out.writeln("<html>");
		out.writeln("<head>");
		out.writeln("  <!-- sqlHawk version " + HtmlFormatter.class.getPackage().getImplementationVersion() + " -->");
		out.write("  <title>sqlHawk - ");
		out.write(getDescription(db, table, text, false));
		out.writeln("</title>");
		out.write("  <link rel=stylesheet href='");
		if (table != null)
			out.write("../");
		out.writeln("sqlHawk.css' type='text/css'>");
		out.writeln("  <meta HTTP-EQUIV='Content-Type' CONTENT='text/html; charset=" + Config.getInstance().getCharset() + "'>");
		out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "jquery.js'></SCRIPT>");
		out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "sqlHawk.js'></SCRIPT>");
		if (table != null) {
			out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
			out.writeln("    table='" + table + "';");
			out.writeln("  </SCRIPT>");
		}
		if (javascript != null) {
			out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
			for (String line : javascript)
				out.writeln("    " + line);
			out.writeln("  </SCRIPT>");
		}
		out.writeln("</head>");
		out.writeln("<body>");
		writeTableOfContents(showOrphans, out);
		out.writeln("<div class='content' style='clear:both;'>");
		out.writeln("<table width='100%' border='0' cellpadding='0'>");
		out.writeln(" <tr>");
		out.write("  <td class='heading' valign='middle'>");
		out.write("<span class='header'>");
		if (table == null)
			out.write("sqlHawk Analysis of ");
		out.write(getDescription(db, table, text, true));
		out.write("</span>");
		if (table == null && db.getDescription() != null)
			out.write("<span class='description'>" + db.getDescription().replace("\\=", "=") + "</span>");

		String comments = table == null ? null : table.getComments();
		if (comments != null) {
			out.write("<div style='padding: 0px 4px;'>");
			if (encodeComments)
				for (int i = 0; i < comments.length(); ++i)
					out.write(HtmlEncoder.encodeToken(comments.charAt(i)));
			else
				out.write(comments);
			out.writeln("</div><p>");
		}
		out.writeln("</td>");;
		out.writeln("  <td class='heading' align='right' valign='top'><span class='indent'>Generated by</span><br><span class='indent'><span class='signature'><a href='http://timabell.github.com/sqlHawk/' target='_blank'>sqlHawk</a></span></span></td>");
		out.writeln(" </tr>");
		out.writeln("</table>");
	}

	/**
	 * Convenience method for all those formatters that don't deal with JavaScript
	 */
	protected void writeHeader(Database db, Table table, String text, boolean showOrphans, LineWriter out) throws IOException {
		writeHeader(db, table, text, showOrphans, null, out);
	}

	protected void writeGeneratedBy(Date generatedDate, LineWriter html) throws IOException {
		html.write("<span class='container'>");
		html.write("Generated by <span class='signature'><a href='http://timabell.github.com/sqlHawk/' target='_blank'>sqlHawk</a></span> on ");
		html.write(formatDate(generatedDate));
		html.writeln("</span>");
	}

	public String formatDate(Date date) {
		return new SimpleDateFormat("EEE MMM dd HH:mm z yyyy").format(date);
	}

	protected void writeTableOfContents(boolean showOrphans, LineWriter html) throws IOException {
		// don't forget to modify HtmlMultipleSchemasIndexPage with any changes to 'header' or 'headerHolder'
		String path = getPathToRoot();
		// have to use a table to deal with a horizontal scrollbar showing up inappropriately
		html.writeln("<table id='headerHolder' cellspacing='0' cellpadding='0'><tr><td>");
		html.writeln("<div id='header'>");
		html.writeln(" <ul>");
		if (Config.getInstance().isOneOfMultipleSchemas())
			html.writeln("  <li><a href='" + path + "../index.html' title='All Schemas Evaluated'>Schemas</a></li>");
		html.writeln("  <li" + (isMainIndex() ? " id='current'" : "") + "><a href='" + path + "index.html' title='All tables and views in the schema'>Tables</a></li>");
		html.writeln("  <li" + (isRelationshipsPage() ? " id='current'" : "") + "><a href='" + path + "relationships.html' title='Diagram of table relationships'>Relationships</a></li>");
		if (showOrphans)
			html.writeln("  <li" + (isOrphansPage() ? " id='current'" : "") + "><a href='" + path + "utilities.html' title='View of tables with neither parents nor children'>Utility&nbsp;Tables</a></li>");
		html.writeln("  <li" + (isConstraintsPage() ? " id='current'" : "") + "><a href='" + path + "constraints.html' title='Useful for diagnosing error messages that just give constraint name or number'>Constraints</a></li>");
		html.writeln("  <li" + (isAnomaliesPage() ? " id='current'" : "") + "><a href='" + path + "anomalies.html' title=\"Things that might not be quite right\">Anomalies</a></li>");
		html.writeln("  <li" + (isColumnsPage() ? " id='current'" : "") + "><a href='" + path + HtmlColumnsPage.getInstance().getColumnInfos().get(0) + "' title=\"All of the columns in the schema\">Columns</a></li>");
		html.writeln(" </ul>");
		html.writeln("</div>");
		html.writeln("</td></tr></table>");
	}

	protected String getDescription(Database db, Table table, String text, boolean hoverHelp) {
		StringBuilder description = new StringBuilder();
		if (table != null) {
			if (table.isView())
				description.append("View ");
			else
				description.append("Table ");
		}
		if (hoverHelp)
			description.append("<span title='Database'>");
		description.append(db.getName());
		if (hoverHelp)
			description.append("</span>");
		if (db.getSchema() != null) {
			description.append('.');
			if (hoverHelp)
				description.append("<span title='Schema'>");
			description.append(db.getSchema());
			if (hoverHelp)
				description.append("</span>");
		}
		if (table != null) {
			description.append('.');
			if (hoverHelp)
				description.append("<span title='Table'>");
			description.append(table.getName());
			if (hoverHelp)
				description.append("</span>");
		}
		if (text != null) {
			description.append(" - ");
			description.append(text);
		}

		return description.toString();
	}

	protected void writeLegend(boolean tableDetails, LineWriter out) throws IOException {
		writeLegend(tableDetails, true, out);
	}

	protected void writeLegend(boolean tableDetails, boolean diagramDetails, LineWriter out) throws IOException {
		out.writeln(" <table class='legend' border='0'>");
		out.writeln("  <tr>");
		out.writeln("   <td class='dataTable' valign='bottom'>Legend:</td>");
		out.writeln("  </tr>");
		out.writeln("  <tr><td class='container' colspan='2'>");
		out.writeln("   <table class='dataTable' border='1'>");
		out.writeln("    <tbody>");
		out.writeln("    <tr><td class='primaryKey'>Primary key columns</td></tr>");
		out.writeln("    <tr><td class='indexedColumn'>Columns with indexes</td></tr>");
		if (tableDetails)
			out.writeln("    <tr class='impliedRelationship'><td class='detail'><span class='impliedRelationship'>Implied relationships</span></td></tr>");
		// comment this out until I can figure out a clean way to embed image references
		//out.writeln("    <tr><td class='container'>Arrows go from children (foreign keys)" + (tableDetails ? "<br>" : " ") + "to parents (primary keys)</td></tr>");
		if (diagramDetails) {
			out.writeln("    <tr><td class='excludedColumn'>Excluded column relationships</td></tr>");
			if (!tableDetails)
				out.writeln("    <tr class='impliedRelationship'><td class='legendDetail'>Dashed lines show implied relationships</td></tr>");
			out.writeln("    <tr><td class='legendDetail'>&lt; <em>n</em> &gt; number of related tables</td></tr>");
		}
		out.writeln("    </tbody>");
		out.writeln("   </table>");
		out.writeln("  </td></tr>");
		out.writeln(" </table>");
		out.writeln("&nbsp;");
	}

	protected void writeExcludedColumns(Set<TableColumn> excludedColumns, Table table, LineWriter html) throws IOException {
		Set<TableColumn> notInDiagram;

		// diagram INCLUDES relationships directly connected to THIS table's excluded columns
		if (table == null) {
			notInDiagram = excludedColumns;
		} else {
			notInDiagram = new HashSet<TableColumn>();
			for (TableColumn column : excludedColumns) {
				if (column.isAllExcluded() || !column.getTable().equals(table)) {
					notInDiagram.add(column);
				}
			}
		}

		if (notInDiagram.size() > 0) {
			html.writeln("<span class='excludedRelationship'>");
			html.writeln("<br>Excluded from diagram's relationships: ");
			for (TableColumn column : notInDiagram) {
				if (!column.getTable().equals(table)) {
					html.write("<a href=\"" + getPathToRoot() + "tables/");
					html.write(column.getTable().getName());
					html.write(".html\">");
					html.write(column.getTable().getName());
					html.write(".");
					html.write(column.getName());
					html.writeln("</a>&nbsp;");
				}
			}
			html.writeln("</span>");
		}
	}

	protected void writeInvalidGraphvizInstallation(LineWriter html) throws IOException {
		html.writeln("<br>sqlHawk was unable to generate a diagram of table relationships.");
		html.writeln("<br>sqlHawk requires Graphviz " + Dot.getInstance().getSupportedVersions().substring(4) + " from <a href='http://www.graphviz.org' target='_blank'>www.graphviz.org</a>.");
	}

	protected void writeFooter(LineWriter html) throws IOException {
		html.writeln("</div>");
		html.writeln("</body>");
		html.writeln("</html>");
	}

	/**
	 * Override if your output doesn't live in the root directory.
	 * If non blank must end with a trailing slash.
	 *
	 * @return String
	 */
	protected String getPathToRoot() {
		return "";
	}

	/**
	 * Override and return true if you're the main index page.
	 *
	 * @return boolean
	 */
	protected boolean isMainIndex() {
		return false;
	}

	/**
	 * Override and return true if you're the relationships page.
	 *
	 * @return boolean
	 */
	protected boolean isRelationshipsPage() {
		return false;
	}

	/**
	 * Override and return true if you're the orphans page.
	 *
	 * @return boolean
	 */
	protected boolean isOrphansPage() {
		return false;
	}

	/**
	 * Override and return true if you're the constraints page
	 *
	 * @return boolean
	 */
	protected boolean isConstraintsPage() {
		return false;
	}

	/**
	 * Override and return true if you're the anomalies page
	 *
	 * @return boolean
	 */
	protected boolean isAnomaliesPage() {
		return false;
	}

	/**
	 * Override and return true if you're the columns page
	 *
	 * @return boolean
	 */
	protected boolean isColumnsPage() {
		return false;
	}
}