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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.DbAnalyzer;
import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.implied.ImpliedConstraintFinder;
import uk.co.timwise.sqlhawk.implied.ImpliedForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;
import uk.co.timwise.sqlhawk.rails.RailsConstraints;
import uk.co.timwise.sqlhawk.util.LineWriter;

public class HtmlWriter {
	private final Logger logger = Logger.getLogger(getClass().getName());

	public void writeHtml(Config config, Database db) throws Exception {
		if (db == null){
			throw new Exception("Database input missing, can't write html.");
		}
		File outputDir = config.getTargetDir();
		LineWriter out;
		new File(outputDir, "tables").mkdirs();
		new File(outputDir, "diagrams/summary").mkdirs();

		logger.info("Gathered schema details");
		logger.info("Writing/graphing summary...");
		ImageWriter.getInstance().writeImages(outputDir);
		ResourceWriter.getInstance().writeResource("/jquery.js", new File(outputDir, "/jquery.js"));
		ResourceWriter.getInstance().writeResource("/sqlHawk.js", new File(outputDir, "/sqlHawk.js"));
		Collection<Table> tablesAndViews = db.getTablesAndViews();
		boolean showDetailedTables = config.isShowDetailedTablesEnabled();
		final boolean includeImpliedConstraints = config.isImpliedConstraintsEnabled();

		// if evaluating a 'ruby on rails-based' database then connect the columns
		// based on RoR conventions
		// note that this is done before 'hasRealRelationships' gets evaluated so
		// we get a relationships ER diagram
		if (config.isRailsEnabled())
			RailsConstraints.getRailsConstraints(db.getTablesByName());

		File diagramsDir = new File(outputDir, "diagrams/summary");

		// set up column exclude list
		Set<TableColumn> excludedColumns = getExcludedColumns(tablesAndViews);
		boolean hasRealRelationships = true; // TODO: cacluate whether this should be set;
		boolean hasImplied = false;  // TODO: cacluate whether this should be set;

		// generate the compact form of the relationships .dot file
		String dotBaseFilespec = "relationships";
		out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.compact.dot"), Config.DOT_CHARSET);
		DotFormatter.getInstance().writeRealRelationships(db, tablesAndViews, true, showDetailedTables, excludedColumns, out);
		out.close();

		if (hasRealRelationships) {
			// real relationships exist so generate the 'big' form of the relationships .dot file
			out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.large.dot"), Config.DOT_CHARSET);
			DotFormatter.getInstance().writeRealRelationships(db, tablesAndViews, false, showDetailedTables, excludedColumns, out);
			out.close();
		}

		// getting implied constraints has a side-effect of associating the parent/child tables, so don't do it
		// here unless they want that behavior
		List<ImpliedForeignKeyConstraint> impliedConstraints = null;
		if (includeImpliedConstraints)
			impliedConstraints = ImpliedConstraintFinder.getImpliedConstraints(tablesAndViews);
		else
			impliedConstraints = new ArrayList<ImpliedForeignKeyConstraint>();

		List<Table> orphans = DbAnalyzer.getOrphans(tablesAndViews);
		boolean hasOrphans = !orphans.isEmpty() && Dot.getInstance().isValid();

		File impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.compact.dot");
		out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
		DotFormatter.getInstance().writeAllRelationships(db, tablesAndViews, true, showDetailedTables, excludedColumns, out);
		out.close();

		if (hasImplied) {
			impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.large.dot");
			out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
			DotFormatter.getInstance().writeAllRelationships(db, tablesAndViews, false, showDetailedTables, excludedColumns, out);
			out.close();
		} else {
			impliedDotFile.delete();
		}

		out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
		HtmlRelationshipsPage.getInstance().write(db, diagramsDir, dotBaseFilespec, hasOrphans, hasRealRelationships, hasImplied, excludedColumns, out);
		out.close();

		dotBaseFilespec = "utilities";
		out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
		HtmlOrphansPage.getInstance().write(db, orphans, diagramsDir, out);
		out.close();

		out = new LineWriter(new File(outputDir, "index.html"), 64 * 1024, config.getCharset());
		HtmlMainIndexPage.getInstance().write(db, tablesAndViews, hasOrphans, out);
		out.close();

		List<ForeignKeyConstraint> constraints = getForeignKeyConstraints(tablesAndViews);
		out = new LineWriter(new File(outputDir, "constraints.html"), 256 * 1024, config.getCharset());
		HtmlConstraintsPage constraintIndexFormatter = HtmlConstraintsPage.getInstance();
		constraintIndexFormatter.write(db, constraints, tablesAndViews, hasOrphans, out);
		out.close();

		out = new LineWriter(new File(outputDir, "anomalies.html"), 16 * 1024, config.getCharset());
		HtmlAnomaliesPage.getInstance().write(db, tablesAndViews, impliedConstraints, hasOrphans, out);
		out.close();

		for (HtmlColumnsPage.ColumnInfo columnInfo : HtmlColumnsPage.getInstance().getColumnInfos()) {
			out = new LineWriter(new File(outputDir, columnInfo.getLocation()), 16 * 1024, config.getCharset());
			HtmlColumnsPage.getInstance().write(db, tablesAndViews, columnInfo, hasOrphans, out);
			out.close();
		}

		// create detailed diagrams

		logger.info("Completed summary");
		logger.info("Writing/diagramming details...");

		HtmlTablePage tableFormatter = HtmlTablePage.getInstance();
		for (Table table : tablesAndViews) {
			logger.fine("Writing details of " + table.getName());

			out = new LineWriter(new File(outputDir, "tables/" + table.getName() + ".html"), 24 * 1024, config.getCharset());
			tableFormatter.write(db, table, hasOrphans, hasImplied, outputDir, excludedColumns, impliedConstraints, out);
			out.close();
		}

		out = new LineWriter(new File(outputDir, "sqlHawk.css"), config.getCharset());
		StyleSheet.getInstance().write(out);
		out.close();
	}

	/**
	 * Returns a <code>List</code> of all of the <code>ForeignKeyConstraint</code>s
	 * used by the specified tables.
	 *
	 * @param tables Collection
	 * @return List
	 */
	private static List<ForeignKeyConstraint> getForeignKeyConstraints(Collection<Table> tables) {
		List<ForeignKeyConstraint> constraints = new ArrayList<ForeignKeyConstraint>();
	
		for (Table table : tables) {
			constraints.addAll(table.getForeignKeys());
		}
	
		return constraints;
	}

	private static Set<TableColumn> getExcludedColumns(Collection<Table> tables) {
		Set<TableColumn> excludedColumns = new HashSet<TableColumn>();
	
		for (Table table : tables) {
			for (TableColumn column : table.getColumns()) {
				if (column.isExcluded()) { // supplied extra xml meta data says ignore this column
					excludedColumns.add(column);
				}
			}
		}
		return excludedColumns;
	}
}
