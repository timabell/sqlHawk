package uk.co.timwise.sqlhawk.html;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.DbAnalyzer;
import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.ImpliedForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.TableColumn;
import uk.co.timwise.sqlhawk.rails.RailsConstraints;
import uk.co.timwise.sqlhawk.util.LineWriter;

public class HtmlWriter {
	private final Logger logger = Logger.getLogger(getClass().getName());

	public long writeHtml(Config config, long start,
			Database db, boolean fineEnabled) throws IOException,
			UnsupportedEncodingException, FileNotFoundException {
		File outputDir = config.getTargetDir();
		long startSummarizing;
		LineWriter out;
		new File(outputDir, "tables").mkdirs();
		new File(outputDir, "diagrams/summary").mkdirs();
		startSummarizing = System.currentTimeMillis();
		if (!fineEnabled) {
			System.out.println("(" + (startSummarizing - start) / 1000 + "sec)");
		}

		logger.info("Gathered schema details in " + (startSummarizing - start) / 1000 + " seconds");
		logger.info("Writing/graphing summary");
		System.err.flush();
		System.out.flush();
		if (!fineEnabled) {
			System.out.print("Writing/graphing summary");
			System.out.print(".");
		}
		ImageWriter.getInstance().writeImages(outputDir);
		ResourceWriter.getInstance().writeResource("/jquery.js", new File(outputDir, "/jquery.js"));
		ResourceWriter.getInstance().writeResource("/sqlHawk.js", new File(outputDir, "/sqlHawk.js"));
		if (!fineEnabled)
			System.out.print(".");
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

		// generate the compact form of the relationships .dot file
		String dotBaseFilespec = "relationships";
		out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.compact.dot"), Config.DOT_CHARSET);
		EvilStatsStore stats = new EvilStatsStore(tablesAndViews);
		DotFormatter.getInstance().writeRealRelationships(db, tablesAndViews, true, showDetailedTables, stats, out);
		boolean hasRealRelationships = stats.getNumTablesWritten() > 0 || stats.getNumViewsWritten() > 0;
		out.close();

		if (hasRealRelationships) {
			// real relationships exist so generate the 'big' form of the relationships .dot file
			if (!fineEnabled)
				System.out.print(".");
			out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.large.dot"), Config.DOT_CHARSET);
			DotFormatter.getInstance().writeRealRelationships(db, tablesAndViews, false, showDetailedTables, stats, out);
			out.close();
		}

		// getting implied constraints has a side-effect of associating the parent/child tables, so don't do it
		// here unless they want that behavior
		List<ImpliedForeignKeyConstraint> impliedConstraints = null;
		if (includeImpliedConstraints)
			impliedConstraints = DbAnalyzer.getImpliedConstraints(tablesAndViews);
		else
			impliedConstraints = new ArrayList<ImpliedForeignKeyConstraint>();

		List<Table> orphans = DbAnalyzer.getOrphans(tablesAndViews);
		boolean hasOrphans = !orphans.isEmpty() && Dot.getInstance().isValid();

		if (!fineEnabled)
			System.out.print(".");

		File impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.compact.dot");
		out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
		boolean hasImplied = DotFormatter.getInstance().writeAllRelationships(db, tablesAndViews, true, showDetailedTables, stats, out);

		Set<TableColumn> excludedColumns = stats.getExcludedColumns();
		out.close();
		if (hasImplied) {
			impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.large.dot");
			out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
			DotFormatter.getInstance().writeAllRelationships(db, tablesAndViews, false, showDetailedTables, stats, out);
			out.close();
		} else {
			impliedDotFile.delete();
		}

		out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
		HtmlRelationshipsPage.getInstance().write(db, diagramsDir, dotBaseFilespec, hasOrphans, hasRealRelationships, hasImplied, excludedColumns, out);
		out.close();

		if (!fineEnabled)
			System.out.print(".");

		dotBaseFilespec = "utilities";
		out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
		HtmlOrphansPage.getInstance().write(db, orphans, diagramsDir, out);
		out.close();

		if (!fineEnabled)
			System.out.print(".");

		out = new LineWriter(new File(outputDir, "index.html"), 64 * 1024, config.getCharset());
		HtmlMainIndexPage.getInstance().write(db, tablesAndViews, hasOrphans, out);
		out.close();

		if (!fineEnabled)
			System.out.print(".");

		List<ForeignKeyConstraint> constraints = DbAnalyzer.getForeignKeyConstraints(tablesAndViews);
		out = new LineWriter(new File(outputDir, "constraints.html"), 256 * 1024, config.getCharset());
		HtmlConstraintsPage constraintIndexFormatter = HtmlConstraintsPage.getInstance();
		constraintIndexFormatter.write(db, constraints, tablesAndViews, hasOrphans, out);
		out.close();

		if (!fineEnabled)
			System.out.print(".");

		out = new LineWriter(new File(outputDir, "anomalies.html"), 16 * 1024, config.getCharset());
		HtmlAnomaliesPage.getInstance().write(db, tablesAndViews, impliedConstraints, hasOrphans, out);
		out.close();

		if (!fineEnabled)
			System.out.print(".");

		for (HtmlColumnsPage.ColumnInfo columnInfo : HtmlColumnsPage.getInstance().getColumnInfos()) {
			out = new LineWriter(new File(outputDir, columnInfo.getLocation()), 16 * 1024, config.getCharset());
			HtmlColumnsPage.getInstance().write(db, tablesAndViews, columnInfo, hasOrphans, out);
			out.close();
		}

		// create detailed diagrams

		long startDiagrammingDetails = System.currentTimeMillis();
		if (!fineEnabled)
			System.out.println("(" + (startDiagrammingDetails - startSummarizing) / 1000 + "sec)");
		logger.info("Completed summary in " + (startDiagrammingDetails - startSummarizing) / 1000 + " seconds");
		logger.info("Writing/diagramming details");
		if (!fineEnabled) {
			System.out.print("Writing/diagramming details");
		}

		HtmlTablePage tableFormatter = HtmlTablePage.getInstance();
		for (Table table : tablesAndViews) {
			if (!fineEnabled)
				System.out.print('.');
			else
				logger.fine("Writing details of " + table.getName());

			out = new LineWriter(new File(outputDir, "tables/" + table.getName() + ".html"), 24 * 1024, config.getCharset());
			tableFormatter.write(db, table, hasOrphans, outputDir, stats, out);
			out.close();
		}

		out = new LineWriter(new File(outputDir, "sqlHawk.css"), config.getCharset());
		StyleSheet.getInstance().write(out);
		out.close();
		return startDiagrammingDetails;
	}
}
