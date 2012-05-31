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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.util.LineWriter;

/**
 * The page that contains the all tables that aren't related to others (orphans)
 */
public class HtmlOrphansPage extends HtmlDiagramFormatter {
	private static HtmlOrphansPage instance = new HtmlOrphansPage();
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Singleton: Don't allow instantiation
	 */
	private HtmlOrphansPage() {
	}

	/**
	 * Singleton accessor
	 *
	 * @return the singleton instance
	 */
	public static HtmlOrphansPage getInstance() {
		return instance;
	}

	public void write(Database db, List<Table> orphanTables, File diagramDir, LineWriter html) throws IOException {
		Dot dot = getDot();
		if (dot == null) {
			return; // getDot() will already have warned user so just pass
		}

		Set<Table> orphansWithImpliedRelationships = new HashSet<Table>();

		for (Table table : orphanTables) {
			if (!table.isOrphan(true)){
				orphansWithImpliedRelationships.add(table);
			}
		}

		writeHeader(db, "Utility Tables", !orphansWithImpliedRelationships.isEmpty(), html);

		html.writeln("<a name='diagram'>");
		try {
			StringBuilder maps = new StringBuilder(64 * 1024);

			for (Table table : orphanTables) {
				String dotBaseFilespec = table.getName();

				File dotFile = new File(diagramDir, dotBaseFilespec + ".1degree.dot");
				File imgFile = new File(diagramDir, dotBaseFilespec + ".1degree.png");

				LineWriter dotOut = new LineWriter(dotFile, Config.DOT_CHARSET);
				DotFormatter.getInstance().writeOrphan(table, dotOut);
				dotOut.close();
				try {
					maps.append(dot.generateDiagram(dotFile, imgFile));
				} catch (Dot.DotFailure dotFailure) {
					logger.warning("Error generating diagram:\n  " + dotFailure);
					return;
				}

				html.write("  <img src='diagrams/summary/" + imgFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
				if (orphansWithImpliedRelationships.contains(table))
					html.write(" class='impliedNotOrphan'");
				html.writeln(">");
			}

			html.write(maps.toString());
		} finally {
			html.writeln("</a>");
			writeFooter(html);
		}
	}

	private void writeHeader(Database db, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
		writeHeader(db, null, title, true, html);
		html.writeln("<table class='container' width='100%'>");
		html.writeln("<tr><td class='container'>");
		writeGeneratedBy(db.getGeneratedDate(), html);
		html.writeln("</td>");
		html.writeln("<td class='container' align='right' valign='top' rowspan='2'>");
		writeLegend(false, html);
		html.writeln("</td></tr>");
		html.writeln("<tr><td class='container' align='left' valign='top'>");
		if (hasImpliedRelationships) {
			html.writeln("<form action=''>");
			html.writeln(" <label for='removeImpliedOrphans'><input type=checkbox id='removeImpliedOrphans'>");
			html.writeln("  Hide tables with implied relationships</label>");
			html.writeln("</form>");
		}
		html.writeln("</td></tr></table>");
	}

	@Override
	protected boolean isOrphansPage() {
		return true;
	}
}
