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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.co.timwise.sqlhawk.util.CaseInsensitiveMap;
import uk.co.timwise.sqlhawk.util.DbSpecificConfig;
import uk.co.timwise.sqlhawk.util.Dot;
import uk.co.timwise.sqlhawk.view.DefaultSqlFormatter;
import uk.co.timwise.sqlhawk.view.SqlFormatter;


import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

/**
 * Configuration of a sqlHawk run
 */
public class Config
{
	private static Config instance;
	private File targetDir;
	private File graphvizDir;
	private String dbType;
	private String schema;
	private List<String> schemas;
	private String user;
	private String password;
	private String db;
	private String host;
	private Integer port;
	private String server;
	private Pattern tableInclusions;
	private Pattern tableExclusions;
	private Pattern procedureInclusions;
	private Pattern procedureExclusions;
	private Pattern columnExclusions;
	private Pattern indirectColumnExclusions;
	private String userConnectionPropertiesFile;
	private Properties userConnectionProperties;
	private Integer maxDbThreads;
	private String css;
	private String charset;
	private String font;
	private Integer fontSize;
	private String description;
	private String dbPropertiesLoadedFrom;
	private Level logLevel;
	private SqlFormatter sqlFormatter;
	private String sqlFormatterClass;
	private Boolean highQuality;
	private String schemaSpec;  // used in conjunction with evaluateAll
	public static final String DOT_CHARSET = "UTF-8";
	private static final String ESCAPED_EQUALS = "\\=";
	private JSAPResult jsapConfig;

	/**
	 * Default constructor. Intended for when you want to inject properties
	 * independently (i.e. not from a command line interface).
	 */
	public Config()
	{
		if (instance == null)
			setInstance(this);
	}

	/**
	 * Construct a configuration from an array of options (e.g. from a command
	 * line interface).
	 *
	 * @param options
	 * @throws JSAPException 
	 */
	public Config(String[] argv) throws JSAPException
	{
		setInstance(this);
		//new code for arg parsing using jsap library.
		File jarFile = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		String jarName = jarFile.getName();
		if (jarName=="output") //nicer help output if running outside a jar (i.e. debugging in eclipse)
			jarName = "sqlHawk.jar";
		String usage = "java -jar " + jarName;
		SimpleJSAP jsap = new SimpleJSAP(usage, "Maps sql schema to and from file formats.",
				new Parameter[] {
				//global options
				new FlaggedOption("log-level", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "log-level", "Set the level of logging to perform. The available levels in ascending order of verbosity are: severe, warning, info, config, fine, finer, finest"),
				new Switch("disable-tables", JSAP.NO_SHORTFLAG, "disable-tables", "Disables read and output of table details."),
				new Switch("disable-views", JSAP.NO_SHORTFLAG, "disable-views", "Disables read and output of view details."),
				//options for connecting to db
				new Switch("db-help", JSAP.NO_SHORTFLAG, "db-help", "Show database specific usage information."),
				new FlaggedOption("db-type", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 't', "db-type"),
				new FlaggedOption("host", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 'h', "host"),
				new FlaggedOption("port", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "port"),
				new FlaggedOption("user", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 'u', "user", "Username to use when connecting to the database."),
				new Switch("sso", JSAP.NO_SHORTFLAG, "sso", "Use single-signon when connecting to the database."),
				new Switch("pfp", JSAP.NO_SHORTFLAG, "pfp", "Prompt For Password to use when connecting to the database."),
				new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 'p', "password", "Password to use when connecting to the database."),
				new FlaggedOption("database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 'd', "database", "Name of the database to connect to."),
				new FlaggedOption("schema", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, 's', "schema", "Name of the schema to use/analyse."),
				new FlaggedOption("schemas", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "schemas", "Names of multiple schemas to use/analyse."),
				new FlaggedOption("driver-path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "driver-path", "Path to look for database driver jars."),
				new FlaggedOption("connection-options-file", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "connection-options-file", "File containing a set of extra options to pass to the database driver."),
				new FlaggedOption("connection-options", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "connection-options", "Set of extra options to pass to the database driver. Format of this option is --connection-options property1:value1,property2:value2...")
				.setList(JSAP.LIST).setListSeparator(','),
				//dbms vendor specific options. Options that don't have an entry here can be specified in connection-options. These options will work when specified either way. Explicit command line arguments are supplied purely to improve usability.
				new FlaggedOption("database-instance", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "database-instance", "Sql server instance to connect to. If you want to use this then you need to use the db-type 'mssql-jtds-instance'"),
				//options for reading from db
				new Switch("database-input", JSAP.NO_SHORTFLAG, "database-input", "Read schema information from a database / dbms."),
				new FlaggedOption("max-threads", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "max-threads", "Set a limit the number of threads used to connect to the database. The default is 1. Set to -1 for no limit."),
				new FlaggedOption("column-exclusion-pattern", JSAP.STRING_PARSER, "[^.]", false, JSAP.NO_SHORTFLAG, "column-exclusion-pattern", "Set the columns to exclude from all relationship diagrams. Regular expression of the columns to exclude."), // default value matches nothing, i.e. nothing excluded
				new FlaggedOption("indirect-column-exclusion-pattern", JSAP.STRING_PARSER, "[^.]", false, JSAP.NO_SHORTFLAG, "indirect-column-exclusion-pattern", "Set the columns to exclude from relationship diagrams where the specified columns aren't directly referenced by the focal table. Regular expression of the columns to exclude."), // default value matches nothing, i.e. nothing excluded
				new FlaggedOption("table-inclusion-pattern", JSAP.STRING_PARSER, ".*", false, JSAP.NO_SHORTFLAG, "table-inclusion-pattern", "Set the tables to include in analysis. Regular expression for matching table names. By default everything is included."), // default value matches anything, i.e. everything included
				new FlaggedOption("table-exclusion-pattern", JSAP.STRING_PARSER, "", false, JSAP.NO_SHORTFLAG, "table-exclusion-pattern", "Set the tables to exclude from analysis. Regular expression for matching table names."), // default value matches nothing, i.e. everything included
				new FlaggedOption("procedure-inclusion-pattern", JSAP.STRING_PARSER, ".*", false, JSAP.NO_SHORTFLAG, "procedure-inclusion-pattern", "Set the procedures to include in analysis. Regular expression for matching procedure names. By default everything is included."), // default value matches anything, i.e. everything included
				new FlaggedOption("procedure-exclusion-pattern", JSAP.STRING_PARSER, "", false, JSAP.NO_SHORTFLAG, "procedure-exclusion-pattern", "Set the procedures to exclude from analysis. Regular expression for matching procedure names."), // default value matches nothing, i.e. everything included
				new Switch("guess-relationships", JSAP.NO_SHORTFLAG, "guess-relationships", "Guess the relationships between tables based on matches of column name & type. Use if you database has names like CustomerId for the PK of one table (Customer) and the matching field in a child table (Order.CustomerId) but doesn't have foreign keys constraints defined."),
				new Switch("no-schema", JSAP.NO_SHORTFLAG, "no-schema", "Some databases types (e.g. older versions of Informix) don't really have the concept of a schema but still return true from 'supportsSchemasInTableDefinitions()'. This option lets you ignore that and treat all the tables as if they were in one flat namespace."),
				new Switch("all", JSAP.NO_SHORTFLAG, "all", "Output all the available schemas"),
				new FlaggedOption("schema-spec", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "schema-spec", "When -all is specified then this is the regular expression that determines which schemas to evaluate."),
				new Switch("rails", JSAP.NO_SHORTFLAG, "rails", "Look for Ruby on Rails-based naming conventions in relationships between logical foreign keys and primary keys. Basically all tables have a primary key named 'ID'. All tables are named plural names. The columns that logically reference that 'ID' are the singular form of the table name suffixed with '_ID'."),
				new Switch("disable-row-counts", JSAP.NO_SHORTFLAG, "disable-row-counts", "Disables read and output of current row count of each table."),
				//options for reading from scm files
				new Switch("scm-input", JSAP.NO_SHORTFLAG, "scm-input", "Read schema information from source control files."),
				//options for all file based operations
				new FlaggedOption("target-path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "target-path", "Sets the folder where generated files will be put or read from. The folder will be created if missing for write operations."),
				//options for writing to html
				new Switch("html-output", JSAP.NO_SHORTFLAG, "html-output", "Generate sqlHawk style html documentation."),
				new Switch("html-comments", JSAP.NO_SHORTFLAG, "html-comments", "If this is set then raw html in comments will be allowed to pass through unencoded, otherwise html content will be encoded."),
				new FlaggedOption("graphviz-path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "graphviz-path", "Path to graphviz binaries. Used to find the 'dot' executable used to generate ER diagrams. If not specified then the program expects to find Graphviz's bin directory on the PATH."),
				new FlaggedOption("diagram-font", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "diagram-font", "An alternate font name to use within diagram images. The default is 'Helvetica'."),
				new FlaggedOption("diagram-font-size", JSAP.INTEGER_PARSER, "11", false, JSAP.NO_SHORTFLAG, "diagram-font-size", "An alternate font size to use within diagram images. The default is 11."),
				new Switch("high-quality", JSAP.NO_SHORTFLAG, "high-quality", "Use a high quality 'dot' renderer. Higher quality output takes longer to generate and results in significantly larger image files (which take longer to download / display), but it generally looks better."),
				new FlaggedOption("renderer", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "renderer", "Set the renderer to use for the -Tpng[:renderer[:formatter]] dot option as specified at http://www.graphviz.org/doc/info/command.html Note that the leading ':' is required while :formatter is optional. The default renderer is typically GD. Note that using the high-quality option is the preferred approach over using this option."),
				new FlaggedOption("css", JSAP.STRING_PARSER, "sqlHawk.css", false, JSAP.NO_SHORTFLAG, "css", "The filename of an alternative cascading style sheet to use in generated html. Note that this file is parsed and used to determine characteristics of the generated diagrams, so it must contain specific settings that are documented within sqlHawk.css."),
				new FlaggedOption("charset", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "charset", "The character set to use within HTML pages. Default is 'ISO-8859-1')."),
				new FlaggedOption("schema-description", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "schema-description", "Description of schema that gets display on main html pages."),
				new FlaggedOption("sql-formatter", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "sql-formatter", "The name of the SQL formatter class to use to format SQL into HTML. The implementation of the class must be made available to the class loader, typically by specifying the path to its jar with option 'driver-path'"),
				new Switch("no-logo", JSAP.NO_SHORTFLAG, "no-logo", "Supress inclusion of SourceForge logo in html output."),
				new Switch("rankdirbug", JSAP.NO_SHORTFLAG, "rankdirbug", "Don't use this unless absolutely necessary as it screws up the layout. Changes dot's rank direction rankdir to right-to-left (RL). See http://www.graphviz.org/doc/info/attrs.html#d:rankdir"),
				new Switch("compact-relationship-diagram", JSAP.NO_SHORTFLAG, "compact-relationship-diagram", "Switches dot to compact relationship diagrams. Use if generating diagrams for large numbers of tables (suggested for >300)"),
				//options for writing to scm files
				new Switch("scm-output", JSAP.NO_SHORTFLAG, "scm-output", "Generate output suitable for storing in source control."),
				//options for writing to xml
				new Switch("xml-output", JSAP.NO_SHORTFLAG, "xml-output", "Generate file(s) containing xml representation of a schema"),
				//options for writing delete/insert order
				new Switch("ordering-output", JSAP.NO_SHORTFLAG, "ordering-output", "Generate text files containing read/write order of tables that will work give current constraints. Useful for creating insert/delete scripts."),
				//options for writing to a database
				new Switch("database-output", JSAP.NO_SHORTFLAG, "database-output", "Write schema to a database / dbms. RISK OF DATA LOSS! TAKE BACKUPS FIRST!"),
				new Switch("dry-run", JSAP.NO_SHORTFLAG, "dry-run", "Dry run. Don't actually write changes to the database."),
				//options for reading extra metadata
				new FlaggedOption("metadata-path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "metadata-path", "Meta files are XML-based files that provide additional metadata about the schema being evaluated. Use this option to specify either the name of an individual XML file or the directory that contains meta files. If a directory is specified then it is expected to contain files matching the pattern [schema].meta.xml. For databases that don't have schema substitute [schema] with [database]."),
		});
		jsapConfig = jsap.parse(argv);
		if (jsap.messagePrinted()) {
			if (!jsapConfig.getBoolean("help")) {
				System.err.println();
				System.err.println("Usage:");
				System.err.println("  " + usage + " " + jsap.getUsage());
				System.err.println();
				System.err.println("Run");
				System.err.println(" " + usage + " --help");
				System.err.println("for full usage information.");
			}
			System.exit( 1 );
		}
	}

	public static Config getInstance() {
		if (instance == null)
			instance = new Config();

		return instance;
	}

	/**
	 * Sets the global instance.
	 *
	 * Useful for things like selecting a specific configuration in a UI.
	 *
	 * @param config
	 */
	public static void setInstance(Config config) {
		instance = config;
	}

	public boolean isHtmlGenerationEnabled() {
		return jsapConfig.getBoolean("html-output");
	}

	public boolean isSourceControlOutputEnabled() {
		return jsapConfig.getBoolean("scm-output");
	}

	public boolean isXmlOutputEnabled() {
		return jsapConfig.getBoolean("xml-output");
	}

	public boolean isImpliedConstraintsEnabled() {
		return jsapConfig.getBoolean("guess-relationships");
	}

	public void setTargetDir(String targetDirName) {
		if (targetDirName.endsWith("\""))
			targetDirName = targetDirName.substring(0, targetDirName.length() - 1);

		setTargetDir(new File(targetDirName));
	}

	public void setTargetDir(File outputDir) {
		this.targetDir = outputDir;
	}

	public File getTargetDir() {
		if (targetDir == null)
			setTargetDir(jsapConfig.getString("target-path"));
		return targetDir;
	}

	/**
	 * Set the path to Graphviz so we can find dot to generate ER diagrams
	 *
	 * @param graphvizDir
	 */
	public void setGraphvizDir(String graphvizDir) {
		if (graphvizDir.endsWith("\""))
			graphvizDir = graphvizDir.substring(0, graphvizDir.length() - 1);
		setGraphvizDir(new File(graphvizDir));
	}

	/**
	 * Set the path to Graphviz so we can find dot to generate ER diagrams
	 *
	 * @param graphvizDir
	 */
	public void setGraphvizDir(File graphvizDir) {
		this.graphvizDir = graphvizDir;
	}

	/**
	 * Return the path to Graphviz (used to find the dot executable to run to
	 * generate ER diagrams).
	 * 
	 * If not specified then the program expects to find Graphviz's bin directory on the PATH.
	 *
	 * Returns {@link #getDefaultGraphvizPath()} if a specific Graphviz path
	 * was not specified.
	 *
	 * @return
	 */
	public File getGraphvizDir() {
		if (graphvizDir == null) {        	
			String gv = jsapConfig.getString("graphviz-path");
			if (gv != null)
				setGraphvizDir(gv);
		}
		return graphvizDir;
	}

	/**
	 * Meta files are XML-based files that provide additional metadata
	 * about the schema being evaluated.<p>
	 * <code>meta</code> is either the name of an individual XML file or
	 * the directory that contains meta files.<p>
	 * If a directory is specified then it is expected to contain files
	 * matching the pattern <code>[schema].meta.xml</code>.
	 * For databases that don't have schema substitute [schema] with [database].
	 */
	public String getMeta() {
		return jsapConfig.getString("metadata-path");
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getDbType() {
		if (dbType == null)
			dbType = jsapConfig.getString("db-type");
		if (dbType==null)
			throw new MissingRequiredParameterException("db-type", false);
		return dbType;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getDb() {
		if (db == null)
			db = jsapConfig.getString("database");
		return db;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getSchema() {
		if (schema == null)
			schema = jsapConfig.getString("schema");
		return schema;
	}

	/**
	 * Some databases types (e.g. older versions of Informix) don't really
	 * have the concept of a schema but still return true from
	 * {@link DatabaseMetaData#supportsSchemasInTableDefinitions()}.
	 * This option lets you ignore that and treat all the tables
	 * as if they were in one flat namespace.
	 */
	public boolean isSchemaDisabled() {
		return jsapConfig.getBoolean("no-schema");
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		if (host == null)
			host = jsapConfig.getString("host");
		return host;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getPort() {
		if (port == null && jsapConfig.contains("port")) {
			port = jsapConfig.getInt("port");
		}
		return port;
	}

	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * User used to connect to the database.
	 * Required unless single sign-on is enabled
	 */
	public String getUser() {
		if (user == null)
			user = jsapConfig.getString("user");
		return user;
	}

	/**
	 * By default a "user" (as specified with -u) is required.
	 * This option allows disabling of that requirement for
	 * single sign-on environments.
	 */
	public boolean isSingleSignOn() {
		return jsapConfig.getBoolean("sso");
	}

	/**
	 * Set the password used to connect to the database.
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @see #setPassword(String)
	 * @return
	 */
	public String getPassword() {
		if (password == null)
			password = jsapConfig.getString("password");
		return password;
	}

	/**
	 * @see #setPromptForPasswordEnabled(boolean)
	 * @return
	 */
	public boolean isPromptForPasswordEnabled() {
		return jsapConfig.getBoolean("pfp");
	}

	public String getConnectionPropertiesFile() {
		return userConnectionPropertiesFile;
	}

	/**
	 * Properties from this file (in key=value pair format) are passed to the
	 * database connection.<br>
	 * user (from -u) and password (from -p) will be passed in the
	 * connection properties if specified.
	 * @param propertiesFilename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void setConnectionPropertiesFile(String propertiesFilename) throws FileNotFoundException, IOException {
		if (userConnectionProperties == null)
			userConnectionProperties = new Properties();
		userConnectionProperties.load(new FileInputStream(propertiesFilename));
		userConnectionPropertiesFile = propertiesFilename;
	}

	/**
	 * Returns a {@link Properties} populated either from the properties file specified
	 * by {@link #setConnectionPropertiesFile(String)}, the properties specified by
	 * {@link #setConnectionProperties(String)} or not populated.
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Properties getConnectionProperties() throws FileNotFoundException, IOException {
		if (userConnectionProperties == null) {
			if (jsapConfig.userSpecified("connprops")) {
				String props = jsapConfig.getString("connprops");
				if (props.indexOf(ESCAPED_EQUALS) != -1) {
					setConnectionProperties(props);
				} else {
					setConnectionPropertiesFile(props);
				}
			} else {
				userConnectionProperties = new Properties();
			}
		}
		return userConnectionProperties;
	}

	/**
	 * Specifies connection properties to use in the format:
	 * <code>key1\=value1;key2\=value2</code><br>
	 * user (from -u) and password (from -p) will be passed in the
	 * connection properties if specified.<p>
	 * This is an alternative form of passing connection properties than by file
	 * (see {@link #setConnectionPropertiesFile(String)})
	 *
	 * @param properties
	 */
	public void setConnectionProperties(String properties) {
		userConnectionProperties = new Properties();

		StringTokenizer tokenizer = new StringTokenizer(properties, ";");
		while (tokenizer.hasMoreElements()) {
			String pair = tokenizer.nextToken();
			int index = pair.indexOf(ESCAPED_EQUALS);
			if (index != -1) {
				String key = pair.substring(0, index);
				String value = pair.substring(index + ESCAPED_EQUALS.length());
				userConnectionProperties.put(key, value);
			}
		}
	}

	/**
	 * The filename of the cascading style sheet to use in generated html.
	 * Note that this file is parsed and used to determine characteristics
	 * of the generated diagrams, so it must contain specific settings that
	 * are documented within sqlHawk.css.<p>
	 *
	 * Defaults to <code>"sqlHawk.css"</code>.
	 */
	public String getCss() {
		if (css == null) {
			css = jsapConfig.getString("css");
		}
		return css;
	}

	/**
	 * The font to use within diagram images.
	 * Default: Helvetica
	 */
	public String getFont() {
		if (font == null) {
			font = jsapConfig.getString("diagram-font");
			if (font == null)
				font = "Helvetica";
		}
		return font;
	}

	/**
	 * The font size to use within diagrams.  This is the size of the font used for
	 * 'large' (e.g. not 'compact') diagrams.<p>
	 *
	 * Defaults to 11.
	 */
	public int getFontSize() {
		if (fontSize == null) {
			fontSize = jsapConfig.getInt("diagram-font-size");
		}
		return fontSize.intValue();
	}

	/**
	 * The character set to use within HTML pages (defaults to <code>"ISO-8859-1"</code>).
	 */
	public String getCharset() {
		if (charset == null) {
			charset = jsapConfig.getString("charset");
			if (charset == null)
				charset = "ISO-8859-1";
		}
		return charset;
	}

	/**
	 * Description of schema that gets display on main pages.
	 */
	public String getDescription() {
		if (description == null)
			description = jsapConfig.getString("schema-description");
		return description;
	}

	/**
	 * Maximum number of threads to use when querying database metadata information.
	 * @throws InvalidConfigurationException if unable to load properties
	 */
	public int getMaxDbThreads() throws InvalidConfigurationException {
		if (maxDbThreads == null) {
			Properties properties;
			try {
				properties = getDbProperties(getDbType());
			} catch (IOException exc) {
				throw new InvalidConfigurationException("Failed to load properties for " + getDbType() + ": " + exc)
				.setParamName("-type");
			}

			int max = Integer.MAX_VALUE;
			String threads = properties.getProperty("dbThreads");
			if (threads == null)
				threads = properties.getProperty("dbthreads");
			if (threads != null)
				max = Integer.parseInt(threads);
			if(jsapConfig.contains("max-threads"))
				max = jsapConfig.getInt("max-threads");
			if (max < 0) //-1 means no limit
				max = Integer.MAX_VALUE;
			else if (max == 0)
				max = 1;
			maxDbThreads = new Integer(max);
		}
		return maxDbThreads.intValue();
	}

	public boolean isLogoEnabled() {
		return jsapConfig.getBoolean("no-logo");
	}

	/**
	 * Don't use this unless absolutely necessary as it screws up the layout.
	 * Changes dot's rank direction rankdir to right-to-left (RL)
	 * http://www.graphviz.org/doc/info/attrs.html#d:rankdir
	 */
	public boolean isRankDirBugEnabled() {
		return jsapConfig.getBoolean("rankdirbug");
	}

	/**
	 * Look for Ruby on Rails-based naming conventions in
	 * relationships between logical foreign keys and primary keys.<p>
	 *
	 * Basically all tables have a primary key named <code>ID</code>.
	 * All tables are named plural names.
	 * The columns that logically reference that <code>ID</code> are the singular
	 * form of the table name suffixed with <code>_ID</code>.<p>
	 */
	public boolean isRailsEnabled() {
		return jsapConfig.getBoolean("rails");
	}

	/**
	 * Allow Html In Comments - encode them unless otherwise specified
	 */
	public boolean isEncodeCommentsEnabled() {
		return !jsapConfig.getBoolean("html-comments");
	}

	/**
	 * If enabled we'll attempt to query/render the number of rows that
	 * each table contains.<p/>
	 *
	 * Defaults to <code>true</code> (enabled).
	 */
	public boolean isNumRowsEnabled() {
		return !jsapConfig.getBoolean("disable-row-counts");
	}

	/**
	 * If enabled we'll include views in the analysis.<p/>
	 */
	public boolean isViewsEnabled() {
		return !jsapConfig.getBoolean("disable-views");
	}

	public boolean isTableProcessingEnabled() {
		return !jsapConfig.getBoolean("disable-tables");
	}

	/**
	 * Set the columns to exclude from all relationship diagrams.
	 * Regular expression of the columns to exclude.
	 */
	public Pattern getColumnExclusions() {
		if (columnExclusions == null) {
			String strExclusions = jsapConfig.getString("column-exclusion-pattern");
			columnExclusions = Pattern.compile(strExclusions);
		}
		return columnExclusions;
	}

	/**
	 * Set the columns to exclude from relationship diagrams where the specified
	 * columns aren't directly referenced by the focal table.
	 *
	 * columnExclusions - regular expression of the columns to exclude
	 */
	public Pattern getIndirectColumnExclusions() {
		if (indirectColumnExclusions == null) {
			String strExclusions = jsapConfig.getString("indirect-column-exclusion-pattern");
			indirectColumnExclusions = Pattern.compile(strExclusions);
		}
		return indirectColumnExclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which tables to include in the analysis.
	 *
	 * @return
	 */
	public Pattern getTableInclusions() {
		if (tableInclusions == null) {
			String strInclusions = jsapConfig.getString("table-inclusion-pattern");
			try {
				tableInclusions = Pattern.compile(strInclusions);
			} catch (PatternSyntaxException badPattern) {
				throw new InvalidConfigurationException(badPattern).setParamName("table-inclusion-pattern");
			}
		}
		return tableInclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which tables to exclude from the analysis.
	 *
	 * @return
	 */
	public Pattern getTableExclusions() {
		if (tableExclusions == null) {
			String strExclusions = jsapConfig.getString("table-exclusion-pattern");
			try {
				tableExclusions = Pattern.compile(strExclusions);
			} catch (PatternSyntaxException badPattern) {
				throw new InvalidConfigurationException(badPattern).setParamName("table-exclusion-pattern");
			}
		}
		return tableExclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which procedures to include in the analysis.
	 *
	 * @return
	 */
	public Pattern getProcedureInclusions() {
		if (procedureInclusions == null) {
			String strInclusions = jsapConfig.getString("procedure-inclusion-pattern");
			try {
				procedureInclusions = Pattern.compile(strInclusions);
			} catch (PatternSyntaxException badPattern) {
				throw new InvalidConfigurationException(badPattern).setParamName("procedure-inclusion-pattern");
			}
		}
		return procedureInclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which procedures to exclude from the analysis.
	 *
	 * @return
	 */
	public Pattern getProcedureExclusions() {
		if (procedureExclusions == null) {
			String strExclusions = jsapConfig.getString("procedure-exclusion-pattern");
			try {
				procedureExclusions = Pattern.compile(strExclusions);
			} catch (PatternSyntaxException badPattern) {
				throw new InvalidConfigurationException(badPattern).setParamName("procedure-exclusion-pattern");
			}
		}
		return procedureExclusions;
	}

	public List<String> getSchemas() {
		if (schemas == null) {
			if (jsapConfig.userSpecified("schemas")) {
				String[] tmp = jsapConfig.getStringArray("schemas");
				schemas = Arrays.asList(tmp);
			}
		}
		return schemas;
	}

	/**
	 * Set the name of the {@link SqlFormatter SQL formatter} class to use to
	 * format SQL into HTML.<p/>
	 * The implementation of the class must be made available to the class
	 * loader, typically by specifying the path to its jar with <em>--driver-path</em>
	 * ({@link #setDriverPath(String)}).
	 */
	public void setSqlFormatter(String formatterClassName) {
		sqlFormatterClass = formatterClassName;
		sqlFormatter = null;
	}

	/**
	 * Set the {@link SqlFormatter SQL formatter} to use to format
	 * SQL into HTML.
	 */
	public void setSqlFormatter(SqlFormatter sqlFormatter) {
		this.sqlFormatter = sqlFormatter;
		if (sqlFormatter != null)
			sqlFormatterClass = sqlFormatter.getClass().getName();
	}

	/**
	 * Returns an implementation of {@link SqlFormatter SQL formatter} to use to format
	 * SQL into HTML.  The default implementation is {@link DefaultSqlFormatter}.
	 *
	 * @return
	 * @throws InvalidConfigurationException if unable to instantiate an instance
	 */
	@SuppressWarnings("unchecked")
	public SqlFormatter getSqlFormatter() throws InvalidConfigurationException {
		if (sqlFormatter == null) {
			if (sqlFormatterClass == null) {
				if (jsapConfig.userSpecified("sql-formatter"))
					sqlFormatterClass = jsapConfig.getString("sql-formatter");
				else
					sqlFormatterClass = DefaultSqlFormatter.class.getName();
			}
			try {
				Class<SqlFormatter> clazz = (Class<SqlFormatter>)Class.forName(sqlFormatterClass);
				sqlFormatter = clazz.newInstance();
			} catch (Exception exc) {
				throw new InvalidConfigurationException("Failed to initialize instance of SQL formatter: ", exc)
				.setParamName("sql-formatter");
			}
		}
		return sqlFormatter;
	}

	public boolean isEvaluateAllEnabled() {
		return jsapConfig.getBoolean("all");
	}

	/**
	 * Returns true if we're evaluating a bunch of schemas in one go and
	 * at this point we're evaluating a specific schema.
	 *
	 * @return boolean
	 */
	public boolean isOneOfMultipleSchemas() {
		// set by MultipleSchemaAnalyzer
		return Boolean.getBoolean("oneofmultipleschemas");
	}

	/**
	 * When -all (evaluateAll) is specified then this is the regular
	 * expression that determines which schemas to evaluate.
	 */
	public String getSchemaSpec() {
		if (schemaSpec == null)
			schemaSpec = jsapConfig.getString("schema-spec");
		return schemaSpec;
	}

	/**
	 * Set the renderer to use for the -Tpng[:renderer[:formatter]] dot option as specified
	 * at <a href='http://www.graphviz.org/doc/info/command.html'>
	 * http://www.graphviz.org/doc/info/command.html</a>.<p>
	 * Note that the leading ":" is required while :formatter is optional.<p>
	 * The default renderer is typically GD.<p>
	 * Note that using {@link #setHighQuality(boolean)} is the preferred approach
	 * over using this method.
	 */
	public void setRenderer(String renderer) {
		Dot.getInstance().setRenderer(renderer);
	}

	/**
	 * @see #setRenderer(String)
	 * @return
	 */
	public String getRenderer() {
		String renderer = jsapConfig.getString("renderer");
		if (renderer != null)
			setRenderer(renderer);
		return Dot.getInstance().getRenderer();
	}

	/**
	 * If <code>false</code> then generate output of "lower quality"
	 * than the default.
	 * Note that the default is intended to be "higher quality",
	 * but various installations of Graphviz may have have different abilities.
	 * That is, some might not have the "lower quality" libraries and others might
	 * not have the "higher quality" libraries.<p>
	 * Higher quality output takes longer to generate and results in significantly
	 * larger image files (which take longer to download / display), but it generally
	 * looks better.
	 */
	public void setHighQuality(boolean highQuality) {
		this.highQuality = highQuality;
		Dot.getInstance().setHighQuality(highQuality);
	}

	/**
	 * @see #setHighQuality(boolean)
	 */
	public boolean isHighQuality() {
		if (highQuality == null) {
			highQuality = jsapConfig.getBoolean("high-quality");
			if (highQuality) {
				// use whatever is the default unless explicitly specified otherwise
				Dot.getInstance().setHighQuality(highQuality);
			}
		}
		highQuality = Dot.getInstance().isHighQuality();
		return highQuality;
	}

	/**
	 * Set the level of logging to perform.<p/>
	 * The levels in descending order are:
	 * <ul>
	 *  <li><code>severe</code> (highest - least detail)
	 *  <li><code>warning</code> (default)
	 *  <li><code>info</code>
	 *  <li><code>config</code>
	 *  <li><code>fine</code>
	 *  <li><code>finer</code>
	 *  <li><code>finest</code>  (lowest - most detail)
	 * </ul>
	 *
	 * @param logLevel
	 */
	public void setLogLevel(String logLevel) {
		if (logLevel == null) {
			this.logLevel = Level.WARNING;
			return;
		}

		Map<String, Level> levels = new LinkedHashMap<String, Level>();
		levels.put("severe", Level.SEVERE);
		levels.put("warning", Level.WARNING);
		levels.put("info", Level.INFO);
		levels.put("config", Level.CONFIG);
		levels.put("fine", Level.FINE);
		levels.put("finer", Level.FINER);
		levels.put("finest", Level.FINEST);

		this.logLevel = levels.get(logLevel.toLowerCase());
		if (this.logLevel == null) {
			throw new InvalidConfigurationException("Invalid logLevel: '" + logLevel +
					"'. Must be one of: " + levels.keySet());
		}
	}

	/**
	 * Returns the level of logging to perform.
	 * See {@link #setLogLevel(String)}.
	 *
	 * @return
	 */
	public Level getLogLevel() {
		if (logLevel == null) {
			setLogLevel(jsapConfig.getString("log-level"));
		}

		return logLevel;
	}

	public boolean isDbHelpRequired() {
		return jsapConfig.getBoolean("db-help");// dbHelpRequired;
	}

	public static String getLoadedFromJar() {
		String classpath = System.getProperty("java.class.path");
		return new StringTokenizer(classpath, File.pathSeparator).nextToken();
	}

	/**
	 * @param type
	 * @return
	 * @throws IOException
	 * @throws InvalidConfigurationException if db properties are incorrectly formed
	 */
	public Properties getDbProperties(String type) throws IOException, InvalidConfigurationException {
		ResourceBundle bundle = null;

		try {
			File propertiesFile = new File(type);
			bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
			dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
		} catch (FileNotFoundException notFoundOnFilesystemWithoutExtension) {
			try {
				File propertiesFile = new File(type + ".properties");
				bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
				dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
			} catch (FileNotFoundException notFoundOnFilesystemWithExtensionTackedOn) {
				try {
					bundle = ResourceBundle.getBundle(type);
					dbPropertiesLoadedFrom = "[" + getLoadedFromJar() + "]" + File.separator + type + ".properties";
				} catch (Exception notInJarWithoutPath) {
					try {
						String path = TableOrderer.class.getPackage().getName() + ".dbTypes." + type;
						path = path.replace('.', '/');
						bundle = ResourceBundle.getBundle(path);
						dbPropertiesLoadedFrom = "[" + getLoadedFromJar() + "]/" + path + ".properties";
					} catch (Exception notInJar) {
						notInJar.printStackTrace();
						notFoundOnFilesystemWithExtensionTackedOn.printStackTrace();
						throw notFoundOnFilesystemWithoutExtension;
					}
				}
			}
		}

		Properties props = asProperties(bundle);
		bundle = null;
		String saveLoadedFrom = dbPropertiesLoadedFrom; // keep original thru recursion

		// bring in key/values pointed to by the include directive
		// example: include.1=mysql::selectRowCountSql
		for (int i = 1; true; ++i) {
			String include = (String)props.remove("include." + i);
			if (include == null)
				break;

			int separator = include.indexOf("::");
			if (separator == -1)
				throw new InvalidConfigurationException("include directive in " + dbPropertiesLoadedFrom + " must have '::' between dbType and key");

			String refdType = include.substring(0, separator).trim();
			String refdKey = include.substring(separator + 2).trim();

			// recursively resolve the ref'd properties file and the ref'd key
			Properties refdProps = getDbProperties(refdType);
			props.put(refdKey, refdProps.getProperty(refdKey));
		}

		// bring in base properties files pointed to by the extends directive
		String baseDbType = (String)props.remove("extends");
		if (baseDbType != null) {
			baseDbType = baseDbType.trim();
			Properties baseProps = getDbProperties(baseDbType);

			// overlay our properties on top of the base's
			baseProps.putAll(props);
			props = baseProps;
		}

		// done with this level of recursion...restore original
		dbPropertiesLoadedFrom = saveLoadedFrom;

		return props;
	}

	protected String getDbPropertiesLoadedFrom() throws IOException {
		if (dbPropertiesLoadedFrom == null)
			getDbProperties(getDbType());
		return dbPropertiesLoadedFrom;
	}

	public String getDatabaseInstance() {
		if (jsapConfig.userSpecified("database-instance"))
			return jsapConfig.getString("database-instance");
		return null;
	}

	/**
	 * Returns a {@link Properties} populated with the contents of <code>bundle</code>
	 *
	 * @param bundle ResourceBundle
	 * @return Properties
	 */
	public static Properties asProperties(ResourceBundle bundle) {
		Properties props = new Properties();
		Enumeration<String> iter = bundle.getKeys();
		while (iter.hasMoreElements()) {
			String key = iter.nextElement();
			props.put(key, bundle.getObject(key));
		}

		return props;
	}

	/**
	 * Thrown to indicate that a required parameter is missing
	 */
	public static class MissingRequiredParameterException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final boolean dbTypeSpecific;

		public MissingRequiredParameterException(String paramId, boolean dbTypeSpecific) {
			this(paramId, null, dbTypeSpecific);
		}

		public MissingRequiredParameterException(String paramId, String description, boolean dbTypeSpecific) {
			super("Required parameter '" + paramId + "' " +
					(description == null ? "" : "(" + description + ") ") +
					"was not specified." +
					(dbTypeSpecific ? "  It is required for this database type." : ""));
			this.dbTypeSpecific = dbTypeSpecific;
		}

		public boolean isDbTypeSpecific() {
			return dbTypeSpecific;
		}
	}

	public static Set<String> getBuiltInDatabaseTypes(String loadedFromJar) {
		Set<String> databaseTypes = new TreeSet<String>();
		JarInputStream jar = null;

		try {
			jar = new JarInputStream(new FileInputStream(loadedFromJar));
			JarEntry entry;

			while ((entry = jar.getNextJarEntry()) != null) {
				String entryName = entry.getName();
				int dotPropsIndex = entryName.indexOf(".properties");
				if (dotPropsIndex != -1)
					databaseTypes.add(entryName.substring(0, dotPropsIndex));
			}
		} catch (IOException exc) {
			System.out.println("Failed to open jar to read properties files:\n" + exc);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException ignore) {}
			}
		}

		return databaseTypes;
	}

	protected void dumpDbUsage() {
		System.out.println("Built-in database types and their required connection parameters:");
		for (String type : getBuiltInDatabaseTypes(getLoadedFromJar())) {
			new DbSpecificConfig(type).dumpUsage();
		}
		System.out.println();
		System.out.println("You can use your own database types by specifying the filespec of a .properties file with -t.");
		System.out.println("Grab one out of " + getLoadedFromJar() + " and modify it to suit your needs.");
		System.out.println();
	}

	public boolean isShowDetailedTablesEnabled() {
		return !jsapConfig.getBoolean("compact-relationship-diagram");
	}

	public String getDriverPath() {
		return jsapConfig.getString("driver-path");
	}

	public Map<String, String> getExtraConnectionOptions() {
		Map<String, String> extraOptions = new CaseInsensitiveMap<String>();
		if (!jsapConfig.userSpecified("connection-options"))
			return extraOptions; //return empty list
		//get the raw value pairs from the command line argument.
		//jsap will parse comma separated values into separate strings,
		//then we manually parse the colon separated key:value into its parts
		//and add to list of option data.
		String[] rawOptions = jsapConfig.getStringArray("connection-options");
		for(String rawOption : rawOptions){
			String parts[] = rawOption.split(":");
			if (parts.length!=2)
				throw new InvalidConfigurationException("error parsing value in --connection-options '" + rawOption + "'" );
			extraOptions.put(parts[0], parts[1]);
		}
		return extraOptions;
	}

	public boolean isOrderingOutputEnabled() {
		return jsapConfig.getBoolean("ordering-output");
	}

	public boolean isDatabaseInputEnabled() {
		return jsapConfig.getBoolean("database-input");
	}

	public boolean isScmInputEnabled() {
		return jsapConfig.getBoolean("scm-input");
	}

	public boolean isDatabaseOutputEnabled() {
		return jsapConfig.getBoolean("database-output");
	}

	public boolean isDryRun() {
		return jsapConfig.getBoolean("dry-run");
	}
}
