package net.sourceforge.schemaspy.db.write;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Procedure;

public class DbWriter {

	public void write(Config config, Connection connection,
			DatabaseMetaData meta, String dbName, String schema,
			Properties properties, Database db, Database existingDb) throws Exception {
		System.out.println();
		System.out.println("Updating existing database...");
		//add/update stored procs.
		Map<String, Procedure> existingProcs = existingDb.getProcMap();
		for (Procedure updatedProc : db.getProcs()){
			if (existingProcs.containsKey(updatedProc.getName())) {
				System.out.println("Updating existing proc " + updatedProc.getName());
				//TODO: check if definitions match
				//Change definition from CREATE to ALTER and run.
				if (!updatedProc.getDefinition().startsWith("CREATE"))
					throw new Exception(String.format("Procedure definition doesn't start with CREATE. Procedure name: %s", updatedProc.getName()));
				String updateSql = updatedProc.getDefinition().replaceFirst("^CREATE", "ALTER");
				connection.prepareStatement(updateSql).execute();
			} else { //new proc
				System.out.println("Adding new proc " + updatedProc.getName());
				connection.prepareStatement(updatedProc.getDefinition()).execute();
			}
		}
		Map<String, Procedure> updatedProcs = db.getProcMap();
		for (Procedure existingProc : existingProcs.values()){
			if (!updatedProcs.containsKey(existingProc.getName())){
				System.out.println("Dropping unwanted proc " + existingProc.getName());
				connection.prepareStatement("DROP PROCEDURE " + existingProc.getName()).execute();
			}
		}
		//TODO: delete any unexpected procs
	}
}
