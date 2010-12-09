package uk.co.timwise.sqlhawk.scm.write;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import uk.co.timwise.sqlhawk.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.model.View;
import uk.co.timwise.sqlhawk.util.LineWriter;


public class ScmDbWriter {

	public void writeForSourceControl(File outputDir, Database db) throws IOException {
		writeProcs(outputDir, db.getProcs());
		writeViews(outputDir, db.getViews());
	}
	
	private void writeProcs(File outputDir, Collection<Procedure> procs) throws IOException {		
		File procFolder = new File(outputDir, "Procedures");
		ensureFolder(procFolder);
		for (Procedure proc : procs) {
			LineWriter out = new LineWriter(new File(procFolder, proc.getName() + ".sql"), Config.DOT_CHARSET);
			out.writeln(proc.getDefinition()); //writeln() in preference to write() in order to make patches for sql files cleaner (\n on every line so new lines at end don't affect original last line)
			out.close();		
		}
	}
	
	private void writeViews(File outputDir, Collection<View> views) throws IOException {		
		File viewFolder = new File(outputDir, "Views");
		ensureFolder(viewFolder);
		for (View view : views) {
			String viewSql = view.getViewSql();
			if (viewSql==null) {
				System.err.println("No definition found for view " + view.getName());
				continue; //don't write empty file.
			}
			LineWriter out = new LineWriter(new File(viewFolder, view.getName() + ".sql"), Config.DOT_CHARSET);
			out.writeln(viewSql); //writeln() in preference to write() in order to make patches for sql files cleaner (\n on every line so new lines at end don't affect original last line)
			out.close();		
		}
	}
	
	private void ensureFolder(File target) throws IOException {
		if (!target.isDirectory()) {
		    if (!target.mkdirs()) {
		        throw new IOException("Failed to create directory '" + target + "'");
		    }
		}		
	}

}
