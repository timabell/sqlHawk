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
package uk.co.timwise.sqlhawk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.db.read.ProcessExecutionException;
import uk.co.timwise.sqlhawk.html.HtmlMultipleSchemasIndexPage;
import uk.co.timwise.sqlhawk.util.LineWriter;


/**
 * Writes an index page of schemas in a database and fires up new instances
 * of sqlHawk to analyse each one. This should be rewritten to avoid java
 * runtime calls and work from within the java code.
 */
public final class MultipleSchemaAnalyzer {
	private static MultipleSchemaAnalyzer instance = new MultipleSchemaAnalyzer();
	private final Logger logger = Logger.getLogger(getClass().getName());

	private MultipleSchemaAnalyzer() {
	}

	public static MultipleSchemaAnalyzer getInstance() {
		return instance;
	}

	public void analyze(String dbName, DatabaseMetaData meta, String schemaSpec, List<String> schemas, List<String> args, String user, File outputDir, String charset, String loadedFrom) throws SQLException, IOException {
		List<String> genericCommand = new ArrayList<String>();
		genericCommand.add("java");
		genericCommand.add("-Doneofmultipleschemas=true");
		if (new File(loadedFrom).isDirectory()) {
			genericCommand.add("-cp");
			genericCommand.add(loadedFrom);
			genericCommand.add(Main.class.getName());
		} else {
			genericCommand.add("-jar");
			genericCommand.add(loadedFrom);
		}

		for (String next : args) {
			if (next.startsWith("-"))
				genericCommand.add(next);
			else
				genericCommand.add("\"" + next + "\"");
		}

		List<String> populatedSchemas;
		if (schemas == null) {
			logger.info("Analyzing schemas that match regular expression '" + schemaSpec + "':");
			logger.info("(use -schemaSpec on command line or in .properties to exclude other schemas)");
			populatedSchemas = getPopulatedSchemas(meta, schemaSpec, user);
		} else {
			logger.info("Analyzing schemas:");
			populatedSchemas = schemas;
		}

		for (String populatedSchema : populatedSchemas)
			logger.info(" " + populatedSchema);

		writeIndexPage(dbName, populatedSchemas, meta, outputDir, charset);

		for (String schema : populatedSchemas) {
			List<String> command = new ArrayList<String>(genericCommand);
			if (dbName == null)
				command.add("-db");
			else
				command.add("-s");
			command.add(schema);
			command.add("-o");
			command.add(new File(outputDir, schema).toString());
			logger.info("Analyzing " + schema);
			System.out.flush();
			logger.fine("Analyzing schema with: " + command);
			Process java = Runtime.getRuntime().exec(command.toArray(new String[]{}));
			new ProcessOutputReader(java.getInputStream(), System.out).start();
			new ProcessOutputReader(java.getErrorStream(), System.err).start();

			try {
				int rc = java.waitFor();
				if (rc != 0) {
					StringBuilder err = new StringBuilder("Failed to execute this process (rc " + rc + "):");
					for (String chunk : command) {
						err.append(" ");
						err.append(chunk);
					}
					throw new ProcessExecutionException(err.toString());
				}
			} catch (InterruptedException exc) {
			}
		}

		logger.info("Wrote relationship details of " + populatedSchemas.size() + " schema" + (populatedSchemas.size() == 1 ? "" : "s") + ".");
		logger.info("Start with " + new File(outputDir, "index.html"));
	}

	public void analyze(String dbName, List<String> schemas, List<String> args,
			String user, File outputDir, String charset, String loadedFromJar) throws SQLException, IOException {
		analyze(dbName, null, null, schemas, args, user, outputDir, charset, loadedFromJar);
	}

	private void writeIndexPage(String dbName, List<String> populatedSchemas, DatabaseMetaData meta, File outputDir, String charset) throws IOException {
		if (populatedSchemas.size() > 0) {
			LineWriter index = new LineWriter(new File(outputDir, "index.html"), charset);
			HtmlMultipleSchemasIndexPage.getInstance().write(dbName, populatedSchemas, meta, index);
			index.close();
		}
	}

	private List<String> getPopulatedSchemas(DatabaseMetaData meta, String schemaSpec, String user) throws SQLException {
		List<String> populatedSchemas;

		if (meta.supportsSchemasInTableDefinitions()) {
			Pattern schemaRegex = Pattern.compile(schemaSpec);

			populatedSchemas = DbAnalyzer.getPopulatedSchemas(meta, schemaSpec);
			Iterator<String> iter = populatedSchemas.iterator();
			while (iter.hasNext()) {
				String schema = iter.next();
				if (!schemaRegex.matcher(schema).matches()) {
					logger.fine("Excluding schema " + schema +
							": doesn't match + \"" + schemaRegex + '"');
					iter.remove(); // remove those that we're not supposed to analyze
				} else {
						logger.fine("Including schema " + schema +
								": matches + \"" + schemaRegex + '"');
				}
			}
		} else {
			populatedSchemas = Arrays.asList(new String[] {user});
		}

		return populatedSchemas;
	}

	private static class ProcessOutputReader extends Thread {
		private final Reader processReader;
		private final PrintStream out;

		ProcessOutputReader(InputStream processStream, PrintStream out) {
			processReader = new InputStreamReader(processStream);
			this.out = out;
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				int ch;
				while ((ch = processReader.read()) != -1) {
					out.print((char)ch);
					out.flush();
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} finally {
				try {
					processReader.close();
				} catch (Exception exc) {
					exc.printStackTrace(); // shouldn't ever get here...but...
				}
			}
		}
	}
}