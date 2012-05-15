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
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.config.Config;
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
import uk.co.timwise.sqlhawk.util.PasswordReader;
import uk.co.timwise.sqlhawk.xml.write.xmlWriter;


public class SchemaMapper {
	private final Logger logger = Logger.getLogger(getClass().getName());
	private boolean fineEnabled;

	/**
	 * Performs whatever mappings are requested by the config.
	 * @param config
	 * @return true if ran without issue. Use to set exit code.
	 * @throws Exception
	 */
	public boolean RunMapping(Config config) throws Exception {
		setupLogger(config);
		//========= schema reading code ============
		//TODO: check for any conflict in request options (read vs write?)
		if (config.isSourceControlOutputEnabled()
				|| config.isXmlOutputEnabled()
				|| config.isOrderingOutputEnabled()
				|| config.isHtmlGenerationEnabled()) { //one or more output type enabled so need a target directory
			setupOuputDir(config.getTargetDir());
		}
		//if (processMultipleSchemas(config, outputDir)) //multischema support temporarily disabled.
		//	return false; //probably checking and tidying up.
		if (config.isDatabaseInputEnabled() || config.isDatabaseOutputEnabled()){
			config.loadDbType();
		}
		Database db = null;
		if (config.isDatabaseInputEnabled())
			db = analyze(config);
		if (config.isScmInputEnabled())
			db = ScmDbReader.Load(config, config.getTargetDir());
		if (db==null)
			throw new Exception("No database information has been read. Make sure you set a read flag.");
		//========= schema writing code ============
		if (config.isHtmlGenerationEnabled()) {
			new HtmlWriter().writeHtml(config, db, fineEnabled);
		}
		if (config.isSourceControlOutputEnabled())
			new ScmDbWriter().writeForSourceControl(config.getTargetDir(), db);
		if (config.isXmlOutputEnabled())
			xmlWriter.writeXml(config.getTargetDir(), db);
		if (config.isOrderingOutputEnabled())
			writeOrderingFiles(config.getTargetDir(), db);
		if (config.isDatabaseOutputEnabled()) {
			db.setSchema(config.getSchema());
			writeDb(config, db);
		}
		System.out.println();
		System.out.println("Done.");
		return true; //success
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

	private boolean processMultipleSchemas(Config config, File outputDir)
			throws Exception {
		List<String> schemas = config.getSchemas();
		if (schemas != null || config.isEvaluateAllEnabled()) {
			String dbName = config.getDb();

			if (schemas != null){
				//MultipleSchemaAnalyzer.getInstance().analyze(dbName, schemas, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
				throw new UnsupportedOperationException("Multi schema support awaiting re-write");
			} else { //EvaluateAllEnabled
				String schemaSpec = config.getSchemaSpec();
				if (schemaSpec == null)
					schemaSpec = config.getDbType().getProps().getProperty("schemaSpec", ".*");
				Connection connection = getConnection(config);
				DatabaseMetaData meta = connection.getMetaData();
				//MultipleSchemaAnalyzer.getInstance().analyze(dbName, meta, schemaSpec, null, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
				throw new UnsupportedOperationException("Multi schema support awaiting re-write");
			}
			//return true;
		}
		return false;
	}

	private Database readDb(Config config)
			throws Exception {
		Connection connection = getConnection(config);
		DatabaseMetaData meta = connection.getMetaData();

		setSchema(config, meta);

		SchemaMeta schemaMeta = config.getMeta() == null ? null : new SchemaMeta(config.getMeta(), config.getDb(), config.getSchema());

		logger.info("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());
		if (schemaMeta != null && schemaMeta.getFile() != null) {
			logger.info("Using additional metadata from " + schemaMeta.getFile());
		}

		// create our representation of the database
		logger.info("Gathering schema details");
		if (!fineEnabled)
			System.out.println("Gathering schema details...");
		DbReader reader = new DbReader();
		Database db = reader.Read(config, connection, meta, schemaMeta);
		return db;
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

	private void writeDb(Config config, Database db)
			throws Exception {
		Connection connection = getConnection(config);
		DatabaseMetaData meta = connection.getMetaData();
		setSchema(config, meta);

		logger.info("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());
		logger.info("Gathering existing schema details");
		if (!fineEnabled)
			System.out.println("Gathering existing schema details...");
		DbReader reader = new DbReader();
		Database existingDb = reader.Read(config, connection, meta, null);
		System.out.println();
		DbWriter writer = new DbWriter(); 
		writer.write(config, connection, meta, db, existingDb);
	}

	private Connection getConnection(Config config)
			throws Exception {
		Connection connection;
		String connectionUrl = new ConnectionURLBuilder().buildUrl(config);
		if (config.getDb() == null)
			config.setDb(connectionUrl);

		Properties properties = config.getDbType().getProps();
		String driverClass = properties.getProperty("driver");
		String driverPath = properties.getProperty("driverPath");
		if (driverPath == null)
			driverPath = "";
		if (config.getDriverPath() != null)
			driverPath = config.getDriverPath() + File.pathSeparator + driverPath;

		connection = getConnection(config, connectionUrl, driverClass, driverPath);
		return connection;
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

		fineEnabled = logger.isLoggable(Level.FINE);
		logger.info("Starting schema analysis");
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

	/**
	 * dumpNoDataMessage
	 *
	 * @param schema String
	 * @param user String
	 * @param meta DatabaseMetaData
	 */
	private static void dumpNoTablesMessage(String schema, String user, DatabaseMetaData meta, boolean specifiedInclusions) throws SQLException {
		System.out.println();
		System.out.println();
		System.out.println("No tables or views were found in schema '" + schema + "'.");
		List<String> schemas = null;
		Exception failure = null;
		try {
			schemas = DbAnalyzer.getSchemas(meta);
		} catch (SQLException exc) {
			failure = exc;
		} catch (RuntimeException exc) {
			failure = exc;
		}

		if (schemas == null) {
			System.out.println("The user you specified (" + user + ')');
			System.out.println("  might not have rights to read the database metadata.");
			System.out.flush();
			if (failure != null)    // to appease the compiler
				failure.printStackTrace();
			return;
		} else if (schema == null || schemas.contains(schema)) {
			System.out.println("The schema exists in the database, but the user you specified (" + user + ')');
			System.out.println("  might not have rights to read its contents.");
			if (specifiedInclusions) {
				System.out.println("Another possibility is that the regular expression that you specified");
				System.out.println("  for what to include (via -i) didn't match any tables.");
			}
		} else {
			System.out.println("The schema does not exist in the database.");
			System.out.println("Make sure that you specify a valid schema with the -s option and that");
			System.out.println("  the user specified (" + user + ") can read from the schema.");
			System.out.println("Note that schema names are usually case sensitive.");
		}
		System.out.println();
		boolean plural = schemas.size() != 1;
		System.out.println(schemas.size() + " schema" + (plural ? "s" : "") + " exist" + (plural ? "" : "s") + " in this database.");
		System.out.println("Some of these \"schemas\" may be users or system schemas.");
		System.out.println();
		for (String unknown : schemas) {
			System.out.print(unknown + " ");
		}

		System.out.println();
		List<String> populatedSchemas = DbAnalyzer.getPopulatedSchemas(meta);
		if (populatedSchemas.isEmpty()) {
			System.out.println("Unable to determine if any of the schemas contain tables/views");
		} else {
			System.out.println("These schemas contain tables/views that user '" + user + "' can see:");
			System.out.println();
			for (String populated : populatedSchemas) {
				System.out.print(" " + populated);
			}
		}
	}

	private Connection getConnection(Config config, String connectionURL,
			String driverClass, String driverPath) throws FileNotFoundException, IOException {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Using database properties:");
			logger.info("  " + config.getDbPropertiesLoadedFrom());
		} else {
			System.out.println("Using database properties:");
			System.out.println("  " + config.getDbPropertiesLoadedFrom());
		}

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
			System.err.println(exc); // people don't want to see a stack trace...
			System.err.println();
			System.err.print("Failed to load driver '" + driverClass + "'");
			if (classpath.isEmpty())
				System.err.println();
			else
				System.err.println("from: " + classpath);
			if (!invalidClasspathEntries.isEmpty()) {
				if (invalidClasspathEntries.size() == 1)
					System.err.print("This entry doesn't point to a valid file/directory: ");
				else
					System.err.print("These entries don't point to valid files/directories: ");
				System.err.println(invalidClasspathEntries);
			}
			System.err.println();
			System.err.println("Use the --driver-path option to specify the location of the database");
			System.err.println("drivers for your database (usually in a .jar or .zip/.Z).");
			System.err.println();
			throw new ConnectionFailure(exc);
		}

		Properties connectionProperties = config.getConnectionProperties();
		if (config.getUser() != null) {
			connectionProperties.put("user", config.getUser());
		}
		if (config.getPassword() != null) {
			connectionProperties.put("password", config.getPassword());
		} else if (config.isPromptForPasswordEnabled()) {
			connectionProperties.put("password",
					new String(PasswordReader.getInstance().readPassword("Password: ")));
		}

		Connection connection = null;
		try {
			connection = driver.connect(connectionURL, connectionProperties);
			if (connection == null) {
				System.err.println();
				System.err.println("Cannot connect to this database URL:");
				System.err.println("  " + connectionURL);
				System.err.println("with this driver:");
				System.err.println("  " + driverClass);
				System.err.println();
				System.err.println("Additional connection information may be available in ");
				System.err.println("  " + config.getDbPropertiesLoadedFrom());
				throw new ConnectionFailure("Cannot connect to '" + connectionURL +"' with driver '" + driverClass + "'");
			}
		} catch (UnsatisfiedLinkError badPath) {
			System.err.println();
			System.err.println("Failed to load driver [" + driverClass + "] from classpath " + classpath);
			System.err.println();
			System.err.println("Make sure the reported library (.dll/.lib/.so) from the following line can be");
			System.err.println("found by your PATH (or LIB*PATH) environment variable");
			System.err.println();
			badPath.printStackTrace();
			throw new ConnectionFailure(badPath);
		} catch (Exception exc) {
			System.err.println();
			System.err.println("Failed to connect to database URL [" + connectionURL + "]");
			System.err.println();
			exc.printStackTrace();
			throw new ConnectionFailure(exc);
		}

		return connection;
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
