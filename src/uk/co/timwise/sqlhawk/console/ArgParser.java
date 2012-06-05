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
package uk.co.timwise.sqlhawk.console;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.config.DbSpecificConfig;
import uk.co.timwise.sqlhawk.config.DbType;
import uk.co.timwise.sqlhawk.config.InvalidConfigurationException;
import uk.co.timwise.sqlhawk.util.CaseInsensitiveMap;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

/**
 * The Class ArgParser.
 * Converts supplied command line arguments into a Config object.
 */
public class ArgParser {
	private static final String ESCAPED_EQUALS = "\\=";

	/**
	 * Construct a configuration from an array of options (e.g. from a command
	 * line interface).
	 *
	 * @param options
	 * @throws Exception
	 */
	public Config Parse(String[] argv) throws Exception
	{
		SimpleJSAP jsapParser = getJsapParser();
		JSAPResult jsapConfig = jsapParser.parse(argv);
		if (jsapParser.messagePrinted()) {
			if (!jsapConfig.getBoolean("help")) {
				showUsage();
			}
			System.exit( 1 );
		}
		if (argv.length == 0) {
			showUsage();
			System.exit(0);
		}

		if (jsapConfig.getBoolean("db-help")) {
			showDbUsage();
			System.exit(0);
		}

		Config config = new Config();

		// prompt for password if needed
		if (jsapConfig.getBoolean("pfp")) { // prompt for password
			if (jsapConfig.userSpecified("password")){
				throw new Exception("Specify either --pfp or --password, not both.");
			}
			config.setPassword(new String(PasswordReader.getInstance().readPassword("Password: ")));
		}

		// store all selected options in config
		config.setHtmlGenerationEnabled(jsapConfig.getBoolean("html-output"));
		config.setSourceControlOutputEnabled(jsapConfig.getBoolean("scm-output"));
		config.setXmlOutputEnabled(jsapConfig.getBoolean("xml-output"));
		config.setImpliedConstraintsEnabled(jsapConfig.getBoolean("guess-relationships"));
		config.setMetaDataPath(jsapConfig.getString("metadata-path"));
		config.setTargetDir(jsapConfig.getString("target-path"));
		config.setGraphvizDir(jsapConfig.getString("graphviz-path"));
		config.setDbTypeName(jsapConfig.getString("db-type"));
		config.setDb(jsapConfig.getString("database"));
		config.setSchema(jsapConfig.getString("schema"));
		config.setSchemaDisabled(jsapConfig.getBoolean("no-schema"));
		config.setHost(jsapConfig.getString("host"));
		if (jsapConfig.contains("port")){
			config.setPort(jsapConfig.getInt("port"));
		}
		config.setUser(jsapConfig.getString("user"));
		config.setSingleSignOn(jsapConfig.getBoolean("sso"));
		config.setPassword(jsapConfig.getString("password"));
		config.setCss(jsapConfig.getString("css"));
		if (jsapConfig.contains("diagram-font-size")){
			config.setFontSize(jsapConfig.getInt("diagram-font-size"));
		}
		config.setFont(jsapConfig.getString("diagram-font"));
		config.setDescription(jsapConfig.getString("schema-description"));
		config.setCharset(jsapConfig.getString("charset"));
		config.setRankDirBugEnabled(jsapConfig.getBoolean("rankdirbug"));
		config.setRailsEnabled(jsapConfig.getBoolean("rails"));
		config.setEncodeCommentsEnabled(!jsapConfig.getBoolean("html-comments"));
		config.setNumRowsEnabled(!jsapConfig.getBoolean("disable-row-counts"));
		config.setViewsEnabled(!jsapConfig.getBoolean("disable-views"));
		config.setTableProcessingEnabled(!jsapConfig.getBoolean("disable-tables"));
		config.setColumnExclusions(GetPattern(jsapConfig, "column-exclusion-pattern"));
		config.setIndirectColumnExclusions(GetPattern(jsapConfig, "indirect-column-exclusion-pattern"));
		config.setTableInclusions(GetPattern(jsapConfig, "table-inclusion-pattern"));
		config.setTableExclusions(GetPattern(jsapConfig, "table-exclusion-pattern"));
		config.setProcedureInclusions(GetPattern(jsapConfig, "procedure-inclusion-pattern"));
		config.setProcedureExclusions(GetPattern(jsapConfig, "procedure-exclusion-pattern"));
		if (jsapConfig.userSpecified("schemas")) {
			config.setSchemas(Arrays.asList(jsapConfig.getStringArray("schemas")));
		}
		config.setEvaluateAllEnabled(jsapConfig.getBoolean("all"));
		config.setSchemaSpec(jsapConfig.getString("schema-spec"));
		if (jsapConfig.contains("log-level")){
			// TODO: check that mixed case log level is parsed correctly
			try {
				config.setLogLevel(Level.parse(jsapConfig.getString("log-level")));
			} catch (IllegalArgumentException ex)
			{
				throw new InvalidConfigurationException(ex).setParamName("log-level");
			}
		}
		config.setDatabaseInstance(jsapConfig.getString("database-instance"));
		config.setShowDetailedTablesEnabled(!jsapConfig.getBoolean("compact-relationship-diagram"));
		config.setDriverPath(jsapConfig.getString("driver-path"));
		if (jsapConfig.userSpecified("connection-options")){
			// get the raw value pairs from the command line argument.
			// jsap will parse comma separated values into separate strings,
			// then we manually parse the colon separated key:value into its parts
			// and add to list of option data.
			String[] rawOptions = jsapConfig.getStringArray("connection-options");
			Map<String, String> extraOptions = new CaseInsensitiveMap<String>();
			for(String rawOption : rawOptions){
				String parts[] = rawOption.split(":");
				if (parts.length!=2)
					throw new InvalidConfigurationException("error parsing value in --connection-options '" + rawOption + "'" );
				extraOptions.put(parts[0], parts[1]);
			}
			config.setExtraOptions(extraOptions);
		}
		config.setOrderingOutputEnabled(jsapConfig.getBoolean("ordering-output"));
		config.setDatabaseInputEnabled(jsapConfig.getBoolean("database-input"));
		config.setScmInputEnabled(jsapConfig.getBoolean("scm-input"));
		config.setDatabaseOutputEnabled(jsapConfig.getBoolean("database-output"));
		config.setDryRun(jsapConfig.getBoolean("dry-run"));
		config.setForceEnabled(jsapConfig.getBoolean("force"));
		config.setIntializeLogEnabled(jsapConfig.getBoolean("initialize-tracking"));
		config.setBatch(jsapConfig.getString("upgrade-batch"));
		if(jsapConfig.contains("max-threads")){
			config.setMaxDbThreads(jsapConfig.getInt("max-threads"));
		}
		if (jsapConfig.userSpecified("connprops")) { // TODO: fix this and matching options https://github.com/timabell/sqlHawk/issues/62 
			String props = jsapConfig.getString("connprops");
			if (props.indexOf(ESCAPED_EQUALS) != -1) {
				config.setUserConnectionProperties(parseUserConnectionProperties(props));
			} else {
				config.setConnectionPropertiesFile(props);
			}
		}

		return config;
	}

	/**
	 * Specifies connection properties to use in the format:
	 * <code>key1\=value1;key2\=value2</code><br>
	 * user (from -u) and password (from -p) will be passed in the
	 * connection properties if specified.<p>
	 * This is an alternative form of passing connection properties than by file
	 * (see {@link #setConnectionPropertiesFile(String)})
	 * TODO: ensure connection properties arg is properly documented for users
	 *
	 * @param properties
	 */
	private Properties parseUserConnectionProperties(String properties) {
		Properties userConnectionProperties = new Properties();

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
		return userConnectionProperties;
	}

	/**
	 * Gets a regex pattern from a command line option if set. Throws
	 * InvalidConfigurationException if regex is invalid, giving the name of the
	 * parameter.
	 *
	 * @param jsapConfig
	 *          the jsap config
	 * @param parameterName
	 *          the parameter name
	 * @return the pattern
	 */
	private Pattern GetPattern(JSAPResult jsapConfig, String parameterName) {
		if (!jsapConfig.contains(parameterName)) {
			return null;
		}
		try {
			return Pattern.compile(jsapConfig.getString(parameterName));
		} catch (PatternSyntaxException badPattern) {
			throw new InvalidConfigurationException(badPattern).setParamName(parameterName);
		}
	}

	private void showUsage() throws JSAPException {
		String jarCommandLine = getCommandLine();
		System.err.println();
		System.err.println("Usage:");
		System.err.println("  " + jarCommandLine + " " + getJsapParser().getUsage());
		System.err.println();
		System.err.println("Run");
		System.err.println(" " + jarCommandLine + " --help");
		System.err.println("for full usage information.");
	}

	private void showDbUsage() {
		System.out.println("Built-in database types and their required connection parameters:");
		System.out.println();
		for (String typeName : new DbType().getBuiltInDatabaseTypes()) {
			try {
				new DbSpecificConfig(new DbType().getDbType(typeName)).dumpUsage();
			} catch (InvalidConfigurationException e) {
				System.err.println("Error loading properties for db type '" + typeName + "'");
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Error loading properties for db type '" + typeName + "'");
				e.printStackTrace();
				System.exit(1);
			}
		}
		System.out.println();
		System.out.println("You can use your own database types by specifying the filespec of a .properties file with -t.");
		System.out.println("Grab one out of " + getJarName() + " and modify it to suit your needs.");
		System.out.println();
	}

	/**
	 * Gets the jsap parser.
	 * This is where all the command line options and descriptions
	 * are defined.
	 *
	 * @return the jsap parser
	 * @throws JSAPException the jSAP exception
	 */
	private SimpleJSAP getJsapParser() throws JSAPException{
		String jarCommandLine = getCommandLine();
		return new SimpleJSAP(jarCommandLine, "Maps sql schema to and from file formats.",
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
				new Switch("rankdirbug", JSAP.NO_SHORTFLAG, "rankdirbug", "Don't use this unless absolutely necessary as it screws up the layout. Changes dot's rank direction rankdir to right-to-left (RL). See http://www.graphviz.org/doc/info/attrs.html#d:rankdir"),
				new Switch("compact-relationship-diagram", JSAP.NO_SHORTFLAG, "compact-relationship-diagram", "Switches dot to compact relationship diagrams. Use if generating diagrams for large numbers of tables (suggested for >300)"),
				//options for writing to scm files
				new Switch("scm-output", JSAP.NO_SHORTFLAG, "scm-output", "Generate output suitable for storing in source control."),
				//options for writing to xml
				new Switch("xml-output", JSAP.NO_SHORTFLAG, "xml-output", "Generate file(s) containing xml representation of a schema"),
				//options for writing delete/insert order
				new Switch("ordering-output", JSAP.NO_SHORTFLAG, "ordering-output", "Generate text files containing read/write order of tables that will work give current constraints. Useful for creating insert/delete scripts."),
				//options for writing to a database
				new Switch("initialize-tracking", JSAP.NO_SHORTFLAG, "initialize-tracking", "Creates the tracking table 'SqlHawk_UpgradeLog' that SqlHawk uses to determine if upgrade scripts have already been run."),
				new Switch("database-output", JSAP.NO_SHORTFLAG, "database-output", "Write schema to a database / dbms. RISK OF DATA LOSS! TAKE BACKUPS FIRST!"),
				new Switch("dry-run", JSAP.NO_SHORTFLAG, "dry-run", "Dry run. Don't actually write changes to the database."),
				new Switch("force", JSAP.NO_SHORTFLAG, "force", "Update stored procedures, views and functions even if they don't appear different. This will allow you to revalidate these against the latest schema Recommended for use on continuous integration builds."),
				new FlaggedOption("upgrade-batch", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "upgrade-batch", "When running upgrade scripts this will if set be added to the upgrade log to group together a set of scripts into a single batch. Suggested examples: the output of git describe, or an svn version number. This is to help track down the source of changes."),
				//options for reading extra metadata
				new FlaggedOption("metadata-path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, "metadata-path", "Meta files are XML-based files that provide additional metadata about the schema being evaluated. Use this option to specify either the name of an individual XML file or the directory that contains meta files. If a directory is specified then it is expected to contain files matching the pattern [schema].meta.xml. For databases that don't have schema substitute [schema] with [database]."),
		});
	}

	/**
	 * Gets the base of the command line that should be used to invoke sqlhawk.
	 * Used for display to user.
	 * Figures out the current jar name to ensure display matches filename.
	 *
	 * @return the command line
	 */
	private String getCommandLine() {
		return "java -jar " + getJarName();
	}

	private String getJarName() {
		File jarFile = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		String jarName = jarFile.getName();
		if (jarName=="output") // nicer help output if running outside a jar (i.e. debugging in eclipse)
			jarName = "sqlHawk.jar";
		return jarName;
	}
}
