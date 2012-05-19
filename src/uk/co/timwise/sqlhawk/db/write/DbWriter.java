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
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.util.FileHandling;


public class DbWriter {
	private final static Logger logger = Logger.getLogger(TableReader.class.getName());
	private final boolean fineEnabled = logger.isLoggable(Level.FINE);
	private String upgradeLogInsertSql;
	private String upgradeLogFindSql;

	public void write(Config config, Connection connection,
			DatabaseMetaData meta, Database db, Database existingDb) throws Exception {
		System.out.println();
		System.out.println("Updating existing database...");
		//add/update stored procs.
		if (db.getProcs().isEmpty()){
			logger.warning("No procedures requested, skipping stored procedure update.");
		} else {
			updateProcs(config, connection, db, existingDb);
		}
	}

	/**
	 * Update procs in target db to match contents of "db".
	 *
	 * @param config the config
	 * @param connection the connection
	 * @param db the db
	 * @param existingDb the existing db
	 * @throws Exception the exception
	 * @throws SQLException the sQL exception
	 */
	private void updateProcs(Config config, Connection connection, Database db, Database existingDb) throws Exception,
			SQLException {
		if (fineEnabled)
			logger.fine("Adding/updating stored procedures...");
		Map<String, Procedure> existingProcs = existingDb.getProcMap();
		final Pattern include = config.getProcedureInclusions();
		final Pattern exclude = config.getProcedureExclusions();
		NameValidator validator = new NameValidator("procedure", include, exclude, null);
		for (Procedure updatedProc : db.getProcs()){
			String procName = updatedProc.getName();
			if (!validator.isValid(procName))
				continue;
			if (fineEnabled)
				logger.finest("Processing proc " + procName);
			String updatedDefinition = updatedProc.getDefinition();
			if (existingProcs.containsKey(procName)) {
				//check if definitions match
				if (updatedDefinition.equals(existingProcs.get(procName).getDefinition()))
					continue; //already up to date, move on to next proc.
				if (fineEnabled)
					logger.fine("Updating existing proc " + procName);
				//Change definition from CREATE to ALTER and run.
				String updateSql = SqlManagement.ConvertCreateToAlter(updatedDefinition);
				try {
					if (!config.isDryRun())
						connection.prepareStatement(updateSql).execute();
				} catch (SQLException ex){
					//rethrow with information on which proc failed.
					throw new Exception("Error updating proc " + procName, ex);
				}
			} else { //new proc
				if (fineEnabled)
					logger.fine("Adding new proc " + procName);
				String createSql = SqlManagement.ConvertAlterToCreate(updatedDefinition);
				try {
					if (!config.isDryRun())
						connection.prepareStatement(createSql).execute();
				} catch (SQLException ex){
					//rethrow with information on which proc failed.
					throw new Exception("Error updating proc " + procName, ex);
				}
			}
		}
		if (fineEnabled)
			logger.fine("Deleting unwanted stored procedures...");
		Map<String, Procedure> updatedProcs = db.getProcMap();
		for (Procedure existingProc : existingProcs.values()){
			String procName = existingProc.getName();
			if (fineEnabled)
				logger.finest("Checking if proc " + procName + " needs dropping...");
			if (!updatedProcs.containsKey(procName)){
				if (fineEnabled)
					logger.fine("Dropping unwanted proc " + procName);
				if (!config.isDryRun())
					connection.prepareStatement("DROP PROCEDURE " + procName).execute();
			}
		}
	}

	public void initializeLog(Connection connection, Config config) throws SQLException, InvalidConfigurationException, IOException {
		Properties properties = config.getDbType().getProps();
		String upgradeLogTableSql = properties.getProperty("upgradeLogTable");
		if (!config.isDryRun()) {
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
		runScriptDirectory(config, connection, scriptFolder, batch, strip);
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
		// TODO: sort upgrade scripts by filename, paying attention to numbers at start
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
					throw new Exception("SELECT FROM SqlHawk_UpgradeLog failed.", ex);
				}
				try {
					logger.info("Running upgrade script '" + file + "'...");
					connection.prepareStatement(definition).execute();
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
