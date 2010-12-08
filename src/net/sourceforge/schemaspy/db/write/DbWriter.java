package net.sourceforge.schemaspy.db.write;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.db.read.TableReader;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Procedure;

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
		Map<String, Procedure> existingProcs = existingDb.getProcMap();
		for (Procedure updatedProc : db.getProcs()){
			String procName = updatedProc.getName();
			String updatedDefinition = updatedProc.getDefinition();
			if (existingProcs.containsKey(procName)) {
				//check if definitions match
				if (updatedDefinition.equals(existingProcs.get(procName).getDefinition()))
					continue; //already up to date, move on to next proc.
				if (fineEnabled)
					logger.finest("Updating existing proc " + procName);
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
					logger.finest("Adding new proc " + procName);
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
			if (!updatedProcs.containsKey(existingProc.getName())){
				if (fineEnabled)
					logger.finest("Dropping unwanted proc " + existingProc.getName());
				connection.prepareStatement("DROP PROCEDURE " + existingProc.getName()).execute();
			}
		}
	}
}
