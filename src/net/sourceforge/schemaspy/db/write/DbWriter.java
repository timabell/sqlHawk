package net.sourceforge.schemaspy.db.write;

import javax.naming.OperationNotSupportedException;

import net.sourceforge.schemaspy.model.Database;

public class DbWriter {

	public static void Write(Database db) throws Exception {
		throw new OperationNotSupportedException("no support for writing to db yet");
	}

}
