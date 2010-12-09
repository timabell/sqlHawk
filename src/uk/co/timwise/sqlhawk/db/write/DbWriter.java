package uk.co.timwise.sqlhawk.db.write;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.db.NameValidator;
import uk.co.timwise.sqlhawk.db.read.TableReader;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Procedure;


public class DbWriter {
    private final static Logger logger = Logger.getLogger(TableReader.class.getName());
    private final boolean fineEnabled = logger.isLoggable(Level.FINE);

	public void write(Config config, Connection connection,
			DatabaseMetaData meta, String dbName, String schema,
			Properties properties, Database db, Database existingDb) throws Exception {
		System.out.println();
		System.out.println("Updating existing database...");
		//add/update stored procs.
		if (fineEnabled)
			logger.fine("Adding/updating stored procedures...");
		final Pattern include = config.getProcedureInclusions();
		final Pattern exclude = config.getProcedureExclusions();
		NameValidator validator = new NameValidator("procedure", include, exclude, null);
		Map<String, Procedure> existingProcs = existingDb.getProcMap();
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
				String updateSql = updatedDefinition.replaceFirst("CREATE", "ALTER");
				try {
					connection.prepareStatement(updateSql).execute();
				} catch (SQLException ex){
					//rethrow with information on which proc failed.
					throw new Exception("Error updating proc " + procName, ex);
				}
			} else { //new proc
				if (fineEnabled)
					logger.fine("Adding new proc " + procName);
				String createSql = updatedDefinition.replaceFirst("ALTER", "CREATE");
				try {
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
				connection.prepareStatement("DROP PROCEDURE " + procName).execute();
			}
		}
	}
}
