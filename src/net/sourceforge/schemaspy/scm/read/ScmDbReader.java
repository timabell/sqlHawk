package net.sourceforge.schemaspy.scm.read;

import javax.naming.OperationNotSupportedException;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;

public class ScmDbReader {

	public static Database Load(Config config) throws Exception {
		throw new OperationNotSupportedException("no support for loading from scm yet");
	}

}
