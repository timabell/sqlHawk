package net.sourceforge.schemaspy.scm.write;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Procedure;
import net.sourceforge.schemaspy.util.LineWriter;

public class ScmDbWriter {

	public void writeForSourceControl(File outputDir, Database db) throws IOException {
		File procFolder = new File(outputDir, "Procedures");
		if (!procFolder.isDirectory()) {
		    if (!procFolder.mkdirs()) {
		        throw new IOException("Failed to create directory '" + procFolder + "'");
		    }
		}
		Collection<Procedure> procs = db.getProcs();
		for (Procedure proc : procs) {
			LineWriter out = new LineWriter(new File(procFolder, proc.getName() + ".sql"), Config.DOT_CHARSET);
			out.writeln(proc.getDefinition()); //writeln() in preference to write() in order to make patches for sql files cleaner (\n on every line so new lines at end don't affect original last line)
			out.close();		
		}
	}

}
