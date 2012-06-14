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
package uk.co.timwise.sqlhawk.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.util.CaseInsensitiveMap;

/**
 * Configuration of a sqlHawk run
 */
public class Config
{
	@Deprecated
	private static Config instance; // TODO: remove config "instance", replace with simpler usages

	private File targetDir;
	private File graphvizDir;
	private String dbTypeName;
	private String schema;
	private List<String> schemas;
	private String user;
	private String password;
	private String database;
	private String host;
	private Integer port;
	private Pattern tableInclusions;
	private Pattern tableExclusions;
	private Pattern procedureInclusions;
	private Pattern procedureExclusions;
	private Pattern columnExclusions;
	private Pattern indirectColumnExclusions;
	private String userConnectionPropertiesFile;
	private Properties userConnectionProperties = new Properties();
	private Integer maxDbThreads;
	private String css;
	private String charset;
	private String font;
	private Integer fontSize;
	private String description;
	private Level logLevel = Level.INFO;
	private boolean highQuality;
	private String schemaSpec;  // used in conjunction with evaluateAll

	private DbType dbType;
	private boolean htmlGenerationEnabled;
	private boolean sourceControlOutputEnabled;
	private boolean xmlOutputEnabled;
	private boolean impliedConstraintsEnabled;
	private String metaDataPath;
	private boolean schemaDisabled;
	private boolean singleSignOn;
	private boolean rankDirBugEnabled;
	private boolean railsEnabled;
	private boolean encodeCommentsEnabled;
	private boolean numRowsEnabled;
	private boolean viewsEnabled = true;
	private boolean tableProcessingEnabled = true;
	private boolean evaluateAllEnabled;
	private String databaseInstance;
	private boolean showDetailedTablesEnabled;
	private String driverPath;
	private Map<String, String> extraOptions = new CaseInsensitiveMap<String>();
	private boolean orderingOutputEnabled;
	private boolean databaseInputEnabled;
	private boolean scmInputEnabled;
	private boolean databaseOutputEnabled;
	private boolean dryRun;
	private boolean forceEnabled;
	private boolean intializeLogEnabled;
	private String upgradeBatch;
	private String renderer;

	/**
	 * Default constructor. Intended for when you want to inject properties
	 * independently (i.e. not from a command line interface).
	 */
	public Config()
	{
		if (instance == null)
			setInstance(this);
	}

	@Deprecated
	public static Config getInstance() {
		if (instance == null)
			instance = new Config();

		return instance;
	}

	/**
	 * Validate that the selected configuration is consistent with itself
	 * and isn't missing options that are required due to the presence of
	 * other options.
	 * Throws an exception upon encountering an issue.
	 * @throws Exception
	 */
	public void Validate() throws Exception {
		if (isIntializeLogEnabled() && isDatabaseInputEnabled()){
			throw new Exception("Conflicting configuration: both IntializeLog and DatabaseInput set. Set one or other.");
		}
		if (getPassword() != null && isSingleSignOn()){
			throw new Exception("Conflicting configuration: Password and Single Sign On set. Set one or other.");
		}
		// TODO: validate all selected options and combinations
		// https://github.com/timabell/sqlHawk/issues/32
		/*
			if (dbTypeName==null) {
				throw new MissingRequiredParameterException("db-type", false);
			}
			if (targetPath == null) {
				throw new MissingRequiredParameterException("target-path", false);
			}
		 */
	}

	/**
	 * Sets the global instance.
	 *
	 * Useful for things like selecting a specific configuration in a UI.
	 *
	 * @param config
	 */
	@Deprecated
	public static void setInstance(Config config) {
		instance = config;
	}

	public boolean isHtmlGenerationEnabled() {
		return htmlGenerationEnabled;
	}

	public boolean isSourceControlOutputEnabled() {
		return sourceControlOutputEnabled;
	}

	public boolean isXmlOutputEnabled() {
		return xmlOutputEnabled;
	}

	public boolean isImpliedConstraintsEnabled() {
		return impliedConstraintsEnabled;
	}

	public void setTargetDir(File targetDir) {
		this.targetDir = targetDir;
	}

	public File getTargetDir() {
		return targetDir;
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
	public String getMetaDataPath() {
		return metaDataPath;
	}

	public void setDbTypeName(String dbTypeName) {
		this.dbTypeName = dbTypeName;
	}

	public String getDbTypeName() {
		return dbTypeName;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDatabase() {
		return database;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getSchema() {
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
		return schemaDisabled;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getPort() {
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
		return user;
	}

	/**
	 * By default a "user" (as specified with -u) is required.
	 * This option allows disabling of that requirement for
	 * single sign-on environments.
	 */
	public boolean isSingleSignOn() {
		return singleSignOn;
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
		return password;
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
		if (getUserConnectionProperties() == null)
			setUserConnectionProperties(new Properties());
		getUserConnectionProperties().load(new FileInputStream(propertiesFilename));
		setUserConnectionPropertiesFile(propertiesFilename);
	}

	/**
	 * Returns a {@link Properties} populated either from the properties file specified
	 * by {@link #setConnectionPropertiesFile(String)}, the properties specified by
	 * {@link #setConnectionProperties(String)} or not populated.
	 * TODO: fix this and matching options https://github.com/timabell/sqlHawk/issues/62
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Properties getConnectionProperties() throws FileNotFoundException, IOException {
		return getUserConnectionProperties();
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
		return css;
	}

	/**
	 * The font to use within diagram images.
	 */
	public String getFont() {
		return font;
	}

	/**
	 * The font size to use within diagrams.  This is the size of the font used for
	 * 'large' (e.g. not 'compact') diagrams.<p>
	 *
	 * Defaults to 11.
	 */
	public Integer getFontSize() {
		return fontSize;
	}

	/**
	 * The character set to use within HTML pages (defaults to
	 * <code>"ISO-8859-1"</code>).
	 */
	public String getCharset() {
		return charset;
	}

	/**
	 * Description of schema that gets display on main pages.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Maximum number of threads to use when querying database metadata information.
	 * @throws InvalidConfigurationException if unable to load properties
	 * @throws IOException
	 */
	public Integer getMaxDbThreads() {
		return maxDbThreads;
	}

	/**
	 * Don't use this unless absolutely necessary as it screws up the layout.
	 * Changes dot's rank direction rankdir to right-to-left (RL)
	 * http://www.graphviz.org/doc/info/attrs.html#d:rankdir
	 */
	public boolean isRankDirBugEnabled() {
		return rankDirBugEnabled;
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
		return railsEnabled;
	}

	/**
	 * Allow Html In Comments - encode them unless otherwise specified
	 */
	public boolean isEncodeCommentsEnabled() {
		return encodeCommentsEnabled;
	}

	/**
	 * If enabled we'll attempt to query/render the number of rows that
	 * each table contains.<p/>
	 *
	 * Defaults to <code>true</code> (enabled).
	 */
	public boolean isNumRowsEnabled() {
		return numRowsEnabled;
	}

	/**
	 * If enabled we'll include views in the analysis.<p/>
	 */
	public boolean isViewsEnabled() {
		return viewsEnabled;
	}

	public boolean isTableProcessingEnabled() {
		return tableProcessingEnabled;
	}

	/**
	 * Set the columns to exclude from all relationship diagrams.
	 * Regular expression of the columns to exclude.
	 */
	public Pattern getColumnExclusions() {
		return columnExclusions;
	}

	/**
	 * Set the columns to exclude from relationship diagrams where the specified
	 * columns aren't directly referenced by the focal table.
	 *
	 * columnExclusions - regular expression of the columns to exclude
	 */
	public Pattern getIndirectColumnExclusions() {
		return indirectColumnExclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which tables to include in the analysis.
	 *
	 * @return
	 */
	public Pattern getTableInclusions() {
		return tableInclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which tables to exclude from the analysis.
	 *
	 * @return
	 */
	public Pattern getTableExclusions() {
		return tableExclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which procedures to include in the analysis.
	 *
	 * @return
	 */
	public Pattern getProcedureInclusions() {
		return procedureInclusions;
	}

	/**
	 * Get the regex {@link Pattern} for which procedures to exclude from the analysis.
	 *
	 * @return
	 */
	public Pattern getProcedureExclusions() {
		return procedureExclusions;
	}

	public List<String> getSchemas() {
		return schemas;
	}

	public boolean isEvaluateAllEnabled() {
		return evaluateAllEnabled;
	}

	/**
	 * Returns true if we're evaluating a bunch of schemas in one go and
	 * at this point we're evaluating a specific schema.
	 *
	 * @return boolean
	 */
	public boolean isOneOfMultipleSchemas() {
		// set by MultipleSchemaAnalyzer
		 // TODO: remove this property and setting when sorting out multiple schema support - issue #61
		return Boolean.getBoolean("oneofmultipleschemas");
	}

	/**
	 * When -all (evaluateAll) is specified then this is the regular
	 * expression that determines which schemas to evaluate.
	 */
	public String getSchemaSpec() {
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
		this.renderer = renderer;
	}

	/**
	 * @see #setRenderer(String)
	 * @return
	 */
	public String getRenderer() {
		return renderer;
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
	}

	/**
	 * @see #setHighQuality(boolean)
	 */
	public boolean isHighQuality() {
		return highQuality;
	}

	/**
	 * Returns the level of logging to perform.
	 * See {@link #setLogLevel(String)}.
	 *
	 * @return
	 */
	public Level getLogLevel() {
		return logLevel;
	}

	public String getDatabaseInstance() {
		return databaseInstance;
	}

	public boolean isShowDetailedTablesEnabled() {
		return showDetailedTablesEnabled;
	}

	public String getDriverPath() {
		return driverPath;
	}

	public boolean isOrderingOutputEnabled() {
		return orderingOutputEnabled;
	}

	public boolean isDatabaseInputEnabled() {
		return databaseInputEnabled;
	}

	public boolean isScmInputEnabled() {
		return scmInputEnabled;
	}

	public boolean isDatabaseOutputEnabled() {
		return databaseOutputEnabled;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public boolean isForceEnabled() {
		return forceEnabled;
	}

	/**
	 * Gets the db type.
	 * Loads DbType object on demand (and then caches it), so may throw exceptions.
	 * Makes use of dbTypeName so make sure that is set first.
	 *
	 * @return the loaded db type
	 * @throws InvalidConfigurationException the invalid configuration exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public DbType getDbType() throws InvalidConfigurationException, IOException {
		if (dbType == null) {
			dbType = new DbType(dbTypeName);
		}
		return dbType;
	}

	public boolean isIntializeLogEnabled() {
		return intializeLogEnabled;
	}

	public String getUpgradeBatch() {
		return upgradeBatch;
	}


	public void setHtmlGenerationEnabled(boolean htmlGenerationEnabled) {
		this.htmlGenerationEnabled = htmlGenerationEnabled;
	}


	public void setXmlOutputEnabled(boolean xmlOutputEnabled) {
		this.xmlOutputEnabled = xmlOutputEnabled;
	}


	public void setSourceControlOutputEnabled(boolean sourceControlOutputEnabled) {
		this.sourceControlOutputEnabled = sourceControlOutputEnabled;
	}


	public void setImpliedConstraintsEnabled(boolean impliedConstraintsEnabled) {
		this.impliedConstraintsEnabled = impliedConstraintsEnabled;
	}

	public void setMetaDataPath(String metaDataPath) {
		this.metaDataPath = metaDataPath;
	}


	public void setSchemaDisabled(boolean schemaDisabled) {
		this.schemaDisabled = schemaDisabled;
	}


	public void setSingleSignOn(boolean singleSignOn) {
		this.singleSignOn = singleSignOn;
	}


	public void setCss(String css) {
		this.css = css;
	}


	public void setFontSize(Integer fontSize) {
		this.fontSize = fontSize;
	}


	public void setFont(String font) {
		this.font = font;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public void setCharset(String charset) {
		this.charset = charset;
	}


	public void setRankDirBugEnabled(boolean rankDirBugEnabled) {
		this.rankDirBugEnabled = rankDirBugEnabled;
	}


	public void setRailsEnabled(boolean railsEnabled) {
		this.railsEnabled = railsEnabled;
	}


	public void setEncodeCommentsEnabled(boolean encodeCommentsEnabled) {
		this.encodeCommentsEnabled = encodeCommentsEnabled;
	}


	public void setNumRowsEnabled(boolean numRowsEnabled) {
		this.numRowsEnabled = numRowsEnabled;
	}


	public void setViewsEnabled(boolean viewsEnabled) {
		this.viewsEnabled = viewsEnabled;
	}


	public void setTableProcessingEnabled(boolean tableProcessingEnabled) {
		this.tableProcessingEnabled = tableProcessingEnabled;
	}


	public void setColumnExclusions(Pattern columnExclusions) {
		this.columnExclusions = columnExclusions;
	}


	public void setIndirectColumnExclusions(Pattern indirectColumnExclusions) {
		this.indirectColumnExclusions = indirectColumnExclusions;
	}


	public void setTableInclusions(Pattern tableInclusions) {
		this.tableInclusions = tableInclusions;
	}


	public void setTableExclusions(Pattern tableExclusions) {
		this.tableExclusions = tableExclusions;
	}


	public void setProcedureInclusions(Pattern procedureInclusions) {
		this.procedureInclusions = procedureInclusions;
	}


	public void setProcedureExclusions(Pattern procedureExclusions) {
		this.procedureExclusions = procedureExclusions;
	}


	public void setSchemas(List<String> schemas) {
		this.schemas = schemas;
	}


	public void setEvaluateAllEnabled(boolean evaluateAllEnabled) {
		this.evaluateAllEnabled = evaluateAllEnabled;
	}


	public void setSchemaSpec(String schemaSpec) {
		this.schemaSpec = schemaSpec;
	}


	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}


	public void setDatabaseInstance(String databaseInstance) {
		this.databaseInstance = databaseInstance;
	}


	public void setShowDetailedTablesEnabled(boolean showDetailedTablesEnabled) {
		this.showDetailedTablesEnabled = showDetailedTablesEnabled;
	}


	public void setDriverPath(String driverPath) {
		this.driverPath = driverPath;
	}


	public Map<String, String> getExtraOptions() {
		return extraOptions;
	}


	public void setExtraOptions(Map<String, String> extraOptions) {
		this.extraOptions = extraOptions;
	}


	public void setOrderingOutputEnabled(boolean orderingOutputEnabled) {
		this.orderingOutputEnabled = orderingOutputEnabled;
	}


	public void setDatabaseInputEnabled(boolean databaseInputEnabled) {
		this.databaseInputEnabled = databaseInputEnabled;
	}


	public void setScmInputEnabled(boolean scmInputEnabled) {
		this.scmInputEnabled = scmInputEnabled;
	}


	public void setDatabaseOutputEnabled(boolean databaseOutputEnabled) {
		this.databaseOutputEnabled = databaseOutputEnabled;
	}


	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}


	public void setForceEnabled(boolean forceEnabled) {
		this.forceEnabled = forceEnabled;
	}


	public void setIntializeLogEnabled(boolean intializeLogEnabled) {
		this.intializeLogEnabled = intializeLogEnabled;
	}


	public void setUpgradeBatch(String upgradeBatch) {
		this.upgradeBatch = upgradeBatch;
	}


	public void setMaxDbThreads(Integer maxDbThreads) {
		this.maxDbThreads = maxDbThreads;
	}


	public Properties getUserConnectionProperties() {
		return userConnectionProperties;
	}


	public void setUserConnectionProperties(Properties userConnectionProperties) {
		this.userConnectionProperties = userConnectionProperties;
	}


	public String getUserConnectionPropertiesFile() {
		return userConnectionPropertiesFile;
	}


	public void setUserConnectionPropertiesFile(String userConnectionPropertiesFile) {
		this.userConnectionPropertiesFile = userConnectionPropertiesFile;
	}
}
