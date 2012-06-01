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
package uk.co.timwise.sqlhawk.db.write;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.InvalidConfigurationException;
import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.db.NameValidator;
import uk.co.timwise.sqlhawk.db.SqlManagement;
import uk.co.timwise.sqlhawk.db.read.TableReader;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.ISqlObject;
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.model.View;
import uk.co.timwise.sqlhawk.util.FileHandling;


public class DbWriter {
	private final static Logger logger = Logger.getLogger(TableReader.class.getName());
	private String upgradeLogInsertSql;
	private String upgradeLogFindSql;

	public void write(Config config, Connection connection,
			DatabaseMetaData meta, Database db, Database existingDb) throws Exception {
		logger.info("Updating existing database...");
		createUpdateDrop(config, connection, db.getProcMap(), existingDb.getProcMap(), "proc");
		createUpdateDrop(config, connection, db.getViewMap(), existingDb.getViewMap(), "view");
		createUpdateDrop(config, connection, db.getFunctionMap(), existingDb.getFunctionMap(), "function");
	}

	/**
	 * Update views/functions/procs in target db to match contents of "updatedObjects".
	 * Note that exclusion patterns are expeccted to have already
	 * been applied to existingObjects data to avoid accidentally
	 * dropping excluded objects. */
	private <TSqlObject extends ISqlObject> void createUpdateDrop(Config config, Connection connection,
			Map<String, TSqlObject> updatedObjects, Map<String, TSqlObject> existingObjects, String typeName)
				throws Exception, SQLException {
		logger.fine("Synchronising " + typeName + "s...");
		for (TSqlObject updatedObject : updatedObjects.values()){
			String name = updatedObject.getName();
			logger.finest("Processing " + typeName + " " + name);
			String updatedDefinition = updatedObject.getDefinition();
			if (existingObjects.containsKey(name)) {
				//check if definitions match
				if (updatedDefinition.equals(existingObjects.get(name).getDefinition())) {
					if (!config.isForceEnabled()) {
						logger.fine("Existing " + typeName + " " + name + " already up to date");
						continue; //already up to date, move on to next object.
					} else {
						logger.fine("Forcing update of up to date " + typeName + " " + name);
					}
				}
				logger.info("Updating existing " + typeName + " " + name);
				//Change definition from CREATE to ALTER and run.
				String updateSql = SqlManagement.ConvertCreateToAlter(updatedDefinition);
				try {
					if (!config.isDryRun())
						connection.prepareStatement(updateSql).execute();
				} catch (SQLException ex){
					//rethrow with information on which object failed.
					throw new Exception("Error updating " + typeName + " " + name, ex);
				}
			} else { //new object
				logger.info("Adding new " + typeName + " " + name);
				String createSql = SqlManagement.ConvertAlterToCreate(updatedDefinition);
				try {
					if (!config.isDryRun())
						connection.prepareStatement(createSql).execute();
				} catch (SQLException ex){
					//rethrow with information on which object failed.
					throw new Exception("Error updating " + typeName + " " + name, ex);
				}
			}
		}
		logger.fine("Deleting unwanted " + typeName + "s...");
		for (TSqlObject existingView : existingObjects.values()){
			String objectName = existingView.getName();
			logger.finest("Checking if " + typeName + " " + objectName + " needs dropping...");
			if (!updatedObjects.containsKey(objectName)){
				logger.info("Dropping unwanted " + typeName + " " + objectName);
				if (!config.isDryRun())
					connection.prepareStatement("DROP " + typeName + " " + objectName).execute(); //TODO: move syntax to property files
			}
		}
	}

	public void initializeLog(Connection connection, Config config) throws SQLException, InvalidConfigurationException, IOException {
		Properties properties = config.getDbType().getProps();
		String upgradeLogTableSql = properties.getProperty("upgradeLogTable");
		if (!config.isDryRun()) {
			logger.info("Creating table SqlHawk_UpgradeLog...");
			logger.finest("Running initialization sql:\n" + upgradeLogTableSql);
			connection.prepareStatement(upgradeLogTableSql).execute();
		}
	}

	public void runUpgradeScripts(Config config, Connection connection,
			DatabaseMetaData meta) throws Exception {
		File scriptFolder = new File(config.getTargetDir(), "UpgradeScripts");
		if (!scriptFolder.isDirectory()) {
			logger.warning("Upgrade script directory '" + scriptFolder + "' not found. Skipping upgrade scripts.");
			return;
		}
		String batch = config.getBatch();
		Properties properties = config.getDbType().getProps();
		upgradeLogInsertSql = properties.getProperty("upgradeLogInsert");
		upgradeLogFindSql = properties.getProperty("upgradeLogFind");
		int strip = scriptFolder.toString().length() + 1; // remove base path + trailing slash
		boolean useTransactions = meta.supportsTransactions();
		boolean savedTransactionsSetting = false;
		if (useTransactions) {
			logger.fine("Starting transaction for scripted update...");
			savedTransactionsSetting = connection.getAutoCommit();
			connection.setAutoCommit(false);
		} else {
			logger.fine("Transactions not supported by this db type. Transactions will not be used.");
		}
		try {
			runScriptDirectory(config, connection, scriptFolder, batch, strip);
			if (useTransactions) {
				logger.fine("Committing scripted update transaction...");
				connection.commit();
				connection.setAutoCommit(savedTransactionsSetting);
			}
		} catch (Exception ex) {
			if (useTransactions) {
				logger.fine("Rolling back scripted update transaction...");
				connection.rollback();
				connection.setAutoCommit(savedTransactionsSetting);
			}
			throw ex;
		}
	}

	/**
	 * Recursively run all the upgrade scripts in a directory.
	 *
	 * @param config the config
	 * @param connection the connection
	 * @param scriptFolder the script folder
	 * @param batch string to tie all the scripts together with in the upgrade log table
	 * @param strip number of chars to remove from paths when logging
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws Exception the exception
	 */
	private void runScriptDirectory(Config config, Connection connection, File scriptFolder, String batch, int strip) throws IOException,
			Exception {
		File[] files = scriptFolder.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			Pattern fileNumbers = Pattern.compile("^([0-9]+)(.*)");
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() ^ o2.isDirectory()){
					// run directories last
					return o1.isDirectory() ? 1 : -1;
				}
				Matcher o1Number = fileNumbers.matcher(o1.getName());
				Matcher o2Number = fileNumbers.matcher(o2.getName());
				if (!o1Number.find() || !o2Number.find()) {
					// not both numeric, so simple string compare
					return o1.getName().compareTo(o2.getName());
				}
				Integer o1n = Integer.parseInt(o1Number.group(1));
				Integer o2n = Integer.parseInt(o2Number.group(1));
				if (o1n != o2n) {
					return o1n.compareTo(o2n);
				}
				// same number, compare rest of script (avoiding leading zero differences)
				return o1Number.group(2).compareTo(o2Number.group(2));
			}
		});
		// Split where GO on its own on a line (ignoring whitespace, case insensitive)
		// This is a best attempt short of full SQL parsing to establish quoting & commenting.
		// see: http://stackoverflow.com/questions/10734824
		Pattern batchSplitter = Pattern.compile("^\\s*GO\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		for(File file : files){
			if (file.isDirectory()) {
				logger.fine("Processing script directory '" + file + "'...");
				runScriptDirectory(config, connection, file, batch, strip);
			}
			if (!file.getName().endsWith(".sql")) //skip non sql files
				continue;
			String relativePath = file.toString().substring(strip);
			String definition = FileHandling.readFile(file);
			// TODO: see if the script has already been run
			if (!config.isDryRun()) {
				try {
					PreparedStatement log = connection.prepareStatement(upgradeLogFindSql);
					log.setString(1, relativePath);
					log.execute();
					ResultSet resultSet = log.getResultSet();
					if (resultSet.next()) { // existing record of this script found in log
						int upgradeId = resultSet.getInt(1);
						logger.fine("Script '" + file + "' already run. UpgradeId " + upgradeId);
						continue;
					}
				} catch (Exception ex) {
					throw new Exception("Reading table SqlHawk_UpgradeLog failed, use --initialize-tracking before first run.", ex);
				}
				try {
					logger.info("Running upgrade script '" + file + "'...");
					// Split into batches similar to the sql server tools,this makes
					// management of scripts easier as you can include a reference to a
					// new table in the same sql file as the create statement.
					String[] splitSql = batchSplitter.split(definition);
					for (String sql : splitSql) {
						logger.finest("Running upgrade script batch\n" + sql);
						connection.prepareStatement(sql).execute();
					}
				} catch (Exception ex) {
					throw new Exception("Failed to run upgrade script '" + file + "'.", ex);
				}
				try {
					PreparedStatement log = connection.prepareStatement(upgradeLogInsertSql);
					log.setString(1, batch);
					log.setString(2, relativePath);
					log.execute();
				} catch (Exception ex) {
					throw new Exception("INSERT INTO SqlHawk_UpgradeLog failed.", ex);
				}
			}
		}
	}
}
