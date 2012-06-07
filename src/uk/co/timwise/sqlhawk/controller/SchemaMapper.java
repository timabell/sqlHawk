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
package uk.co.timwise.sqlhawk.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.config.InvalidConfigurationException;
import uk.co.timwise.sqlhawk.db.ConnectionURLBuilder;
import uk.co.timwise.sqlhawk.db.read.ConnectionFailure;
import uk.co.timwise.sqlhawk.db.read.DbReader;
import uk.co.timwise.sqlhawk.db.write.DbWriter;
import uk.co.timwise.sqlhawk.html.HtmlWriter;
import uk.co.timwise.sqlhawk.logging.LogFormatter;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.ForeignKeyConstraint;
import uk.co.timwise.sqlhawk.model.Table;
import uk.co.timwise.sqlhawk.model.xml.SchemaMeta;
import uk.co.timwise.sqlhawk.scm.read.ScmDbReader;
import uk.co.timwise.sqlhawk.scm.write.ScmDbWriter;
import uk.co.timwise.sqlhawk.text.TableOrderer;
import uk.co.timwise.sqlhawk.text.TextFormatter;
import uk.co.timwise.sqlhawk.util.LineWriter;
import uk.co.timwise.sqlhawk.xml.write.xmlWriter;


/**
 * The Class SchemaMapper.
 * This is the class that orchestrates the actions taken
 */
public class SchemaMapper {
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Performs whatever mappings are requested by the config.
	 * @param config
	 * @return true if ran without issue. Use to set exit code.
	 * @throws Exception
	 */
	public void RunMapping(Config config) throws Exception {
		setupLogger(config);
		logger.fine("Working directory: " + new File(".").getAbsolutePath());
		//========= schema reading code ============
		//TODO: check for any conflict in request options (read vs write?)
		if (config.isSourceControlOutputEnabled()
				|| config.isXmlOutputEnabled()
				|| config.isOrderingOutputEnabled()
				|| config.isHtmlGenerationEnabled()) { //one or more output type enabled so need a target directory
			setupOuputDir(config.getTargetDir());
		}
		//processMultipleSchemas(config, outputDir); // TODO: multischema support temporarily disabled.
		Database db = null;
		if (config.isDatabaseInputEnabled())
			db = analyze(config);
		if (config.isScmInputEnabled()) {
			if (db != null) {
				throw new Exception("Multiple inputs specified");
			}
			db = new ScmDbReader().Load(config);
		}
		if (db==null && !config.isIntializeLogEnabled())
			logger.warning("No database information has been read. Set a read flag in the command line arguments if required.");
		//========= schema writing code ============
		if (config.isHtmlGenerationEnabled()) {
			new HtmlWriter().writeHtml(config, db);
		}
		if (config.isSourceControlOutputEnabled())
			new ScmDbWriter().writeForSourceControl(config.getTargetDir(), db);
		if (config.isXmlOutputEnabled())
			xmlWriter.writeXml(config.getTargetDir(), db);
		if (config.isOrderingOutputEnabled())
			writeOrderingFiles(config.getTargetDir(), db);
		if (config.isIntializeLogEnabled()) {
			initializeLog(config);
		}
		if (config.isDatabaseOutputEnabled()) {
			db.setSchema(config.getSchema());
			writeDb(config, db);
		}
		logger.info("Done.");
	}

	/**
	 * Connect to a database, load schema information into memory,
	 * return an in-memory representation of the database.
	 * @param config
	 * @return
	 * @throws Exception
	 */
	private Database analyze(Config config) throws Exception {
		return readDb(config);
	}

	private void processMultipleSchemas(Config config, File outputDir) throws Exception {
		List<String> schemas = config.getSchemas();
		if (schemas != null || config.isEvaluateAllEnabled()) {
			if (schemas != null){
				//MultipleSchemaAnalyzer.getInstance().analyze(dbName, schemas, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
				throw new UnsupportedOperationException("Multi schema support awaiting re-write");
			} else { //EvaluateAllEnabled
				String schemaSpec = config.getSchemaSpec();
				if (schemaSpec == null)
					schemaSpec = config.getDbType().getProps().getProperty("schemaSpec", ".*");
				// ConnectionWithMeta connection = getConnection(config);
				// String dbName = config.getDb();
				// MultipleSchemaAnalyzer.getInstance().analyze(dbName, meta, schemaSpec, null, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
				throw new UnsupportedOperationException("Multi schema support awaiting re-write");
			}
		}
	}

	private Database readDb(Config config)
			throws Exception {
		ConnectionWithMeta connection = getConnection(config);

		SchemaMeta schemaMeta = config.getMetaDataPath() == null ? null : new SchemaMeta(config.getMetaDataPath(), config.getDatabase(), config.getSchema());
		if (schemaMeta != null && schemaMeta.getFile() != null) {
			logger.info("Using additional metadata from " + schemaMeta.getFile());
		}

		// create our representation of the database
		logger.info("Gathering schema details...");
		DbReader reader = new DbReader();
		return reader.Read(config, connection.Connection, connection.Metadata, schemaMeta);
	}

	/**
	 * Sets the schema if supported.
	 * If no schema specified then user is used for the schema.
	 * Throws exception if both missing.
	 *
	 * @throws InvalidConfigurationException
	 * @throws SQLException
	 */
	private void setSchema(Config config, DatabaseMetaData meta)
			throws InvalidConfigurationException, SQLException {
		if (config.getSchema() == null && meta.supportsSchemasInTableDefinitions() &&
				!config.isSchemaDisabled()) {
			if (config.getSchema() == null && config.getUser() == null) {
				throw new InvalidConfigurationException("Either a schema ('-s') or a user ('-u') must be specified");
			}
			config.setSchema(config.getUser()); // set the schema to the be the selected user
		}
	}

	private void initializeLog(Config config)
			throws Exception {
		ConnectionWithMeta connection = getConnection(config);
		logger.info("Initializing tracking log table...");
		DbWriter writer = new DbWriter();
		writer.initializeLog(connection.Connection, config);
	}

	private void writeDb(Config config, Database db)
			throws Exception {
		ConnectionWithMeta connection = getConnection(config);

		DbWriter writer = new DbWriter();
		writer.write(config, connection.Connection, connection.Metadata, db);
	}

	private ConnectionWithMeta getConnection(Config config)
			throws Exception {
		Connection connection;
		String connectionUrl = new ConnectionURLBuilder().buildUrl(config);
		if (config.getDatabase() == null)
			config.setDatabase(connectionUrl);

		Properties properties = config.getDbType().getProps();
		String driverClass = properties.getProperty("driver");
		String driverPath = properties.getProperty("driverPath");
		if (driverPath == null)
			driverPath = "";
		if (config.getDriverPath() != null)
			driverPath = config.getDriverPath() + File.pathSeparator + driverPath;

		connection = getConnection(config, connectionUrl, driverClass, driverPath);
		DatabaseMetaData meta = connection.getMetaData();
		logger.info("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());
		setSchema(config, meta);
		return new ConnectionWithMeta(connection, meta);
	}

	private File setupOuputDir(File outputDir) throws IOException {
		if (!outputDir.isDirectory()) {
			if (!outputDir.mkdirs()) {
				throw new IOException("Failed to create directory '" + outputDir + "'");
			}
		}
		return outputDir;
	}

	private void setupLogger(Config config) {
		// set the log level for the root logger
		Logger.getLogger("").setLevel(config.getLogLevel());

		// clean-up console output a bit
		for (Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				((ConsoleHandler)handler).setFormatter(new LogFormatter());
				handler.setLevel(config.getLogLevel());
			}
		}
	}

	private void writeOrderingFiles(File outputDir, Database db)
			throws UnsupportedEncodingException, IOException {
		LineWriter out;
		List<ForeignKeyConstraint> recursiveConstraints = new ArrayList<ForeignKeyConstraint>();

		// create an orderer to be able to determine insertion and deletion ordering of tables
		TableOrderer orderer = new TableOrderer();

		// side effect is that the RI relationships get trashed
		// also populates the recursiveConstraints collection
		List<Table> orderedTables = orderer.getTablesOrderedByRI(db.getTables(), recursiveConstraints);

		out = new LineWriter(new File(outputDir, "insertionOrder.txt"), 16 * 1024, Config.DOT_CHARSET);
		TextFormatter.getInstance().write(orderedTables, false, out);
		out.close();

		out = new LineWriter(new File(outputDir, "deletionOrder.txt"), 16 * 1024, Config.DOT_CHARSET);
		Collections.reverse(orderedTables);
		TextFormatter.getInstance().write(orderedTables, false, out);
		out.close();

		/* we'll eventually want to put this functionality back in with a
		 * database independent implementation
		File constraintsFile = new File(outputDir, "removeRecursiveConstraints.sql");
		constraintsFile.delete();
		if (!recursiveConstraints.isEmpty()) {
		    out = new LineWriter(constraintsFile, 4 * 1024);
		    writeRemoveRecursiveConstraintsSql(recursiveConstraints, schema, out);
		    out.close();
		}

		constraintsFile = new File(outputDir, "restoreRecursiveConstraints.sql");
		constraintsFile.delete();

		if (!recursiveConstraints.isEmpty()) {
		    out = new LineWriter(constraintsFile, 4 * 1024);
		    writeRestoreRecursiveConstraintsSql(recursiveConstraints, schema, out);
		    out.close();
		}
		 */
	}

	private Connection getConnection(Config config, String connectionURL,
			String driverClass, String driverPath) throws FileNotFoundException, IOException {
		logger.fine("Using database properties:\n" + "  " + config.getDbPropertiesLoadedFrom());

		List<URL> classpath = new ArrayList<URL>();
		List<File> invalidClasspathEntries = new ArrayList<File>();
		StringTokenizer tokenizer = new StringTokenizer(driverPath, File.pathSeparator);
		while (tokenizer.hasMoreTokens()) {
			File pathElement = new File(tokenizer.nextToken());
			if (pathElement.exists())
				classpath.add(pathElement.toURI().toURL());
			else
				invalidClasspathEntries.add(pathElement);
		}

		URLClassLoader loader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
		Driver driver = null;
		try {
			driver = (Driver)Class.forName(driverClass, true, loader).newInstance();

			// have to use deprecated method or we won't see messages generated by older drivers
			//java.sql.DriverManager.setLogStream(System.err);
		} catch (Exception exc) {
			logger.fine("Failed to load driver '" + driverClass + "', stack trace:\n" + exc.toString());
			String message = "Failed to load driver '" + driverClass + "' - " + exc.getMessage();
			if (!classpath.isEmpty()){
				message += "\n from: " + classpath;
			}
			if (!invalidClasspathEntries.isEmpty()) {
				if (invalidClasspathEntries.size() == 1)
					message += "\n This entry doesn't point to a valid file/directory: ";
				else
					message += "\n These entries don't point to valid files/directories: ";
					message += "\n " + invalidClasspathEntries;
			}
			message += "\n\n Use the --driver-path option to specify the location of the database";
			message += "\n drivers for your database (usually in a .jar or .zip/.Z).";
			logger.severe(message);
			throw new ConnectionFailure(exc);
		}

		Properties connectionProperties = config.getConnectionProperties();
		if (config.getUser() != null) {
			connectionProperties.put("user", config.getUser());
		}
		if (config.getPassword() != null) {
			connectionProperties.put("password", config.getPassword());
		}

		Connection connection = null;
		try {
			logger.fine("Connecting to " + connectionURL);
			connection = driver.connect(connectionURL, connectionProperties);
			if (connection == null) {
				logger.severe("Failed to database URL " + connectionURL
						+ "with driver " + driverClass
						+ "\n Additional connection information may be available in "
						+ config.getDbPropertiesLoadedFrom());
				throw new ConnectionFailure("Cannot connect to '" + connectionURL +"' with driver '" + driverClass + "'");
			}
		} catch (UnsatisfiedLinkError badPath) {
			logger.severe("Failed to load driver [" + driverClass + "] from classpath " + classpath
					+ "\n Make sure the reported library (.dll/.lib/.so) from the following line can be"
					+ "\n found by your PATH (or LIB*PATH) environment variable\n "
					+ badPath.toString());
			throw new ConnectionFailure(badPath);
		} catch (Exception exc) {
			logger.severe("Failed to connect to database URL [" + connectionURL + "]\n" + exc.toString());
			throw new ConnectionFailure(exc);
		}
		return connection;
	}

	/**
	 * The Class ConnectionWithMeta.
	 * Wrapper for a connection and related Metadata.
	 * To return of both from the getConnection function.
	 */
	private class ConnectionWithMeta{
		public Connection Connection;
		public DatabaseMetaData Metadata;
		public ConnectionWithMeta(Connection connection, DatabaseMetaData meta){
			Connection = connection;
			Metadata = meta;
		}
	}

	/**
	 * Currently very DB2-specific
	 * @param recursiveConstraints List
	 * @param schema String
	 * @param out LineWriter
	 * @throws IOException
	 */
	/* we'll eventually want to put this functionality back in with a
	 * database independent implementation
    private static void writeRemoveRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.writeln("ALTER TABLE " + schema + "." + constraint.getChildTable() + " DROP CONSTRAINT " + constraint.getName() + ";");
        }
    }
	 */

	/**
	 * Currently very DB2-specific
	 * @param recursiveConstraints List
	 * @param schema String
	 * @param out LineWriter
	 * @throws IOException
	 */
	/* we'll eventually want to put this functionality back in with a
	 * database independent implementation
    private static void writeRestoreRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        Map ruleTextMapping = new HashMap();
        ruleTextMapping.put(new Character('C'), "CASCADE");
        ruleTextMapping.put(new Character('A'), "NO ACTION");
        ruleTextMapping.put(new Character('N'), "NO ACTION"); // Oracle
        ruleTextMapping.put(new Character('R'), "RESTRICT");
        ruleTextMapping.put(new Character('S'), "SET NULL");  // Oracle

        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.write("ALTER TABLE \"" + schema + "\".\"" + constraint.getChildTable() + "\" ADD CONSTRAINT \"" + constraint.getName() + "\"");
            StringBuffer buf = new StringBuffer();
            for (Iterator columnIter = constraint.getChildColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" FOREIGN KEY (" + buf.toString() + ")");
            out.write(" REFERENCES \"" + schema + "\".\"" + constraint.getParentTable() + "\"");
            buf = new StringBuffer();
            for (Iterator columnIter = constraint.getParentColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" (" + buf.toString() + ")");
            out.write(" ON DELETE ");
            out.write(ruleTextMapping.get(new Character(constraint.getDeleteRule())).toString());
            out.write(" ON UPDATE ");
            out.write(ruleTextMapping.get(new Character(constraint.getUpdateRule())).toString());
            out.writeln(";");
        }
    }
	 */
}
