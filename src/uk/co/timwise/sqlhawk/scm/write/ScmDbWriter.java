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
package uk.co.timwise.sqlhawk.scm.write;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Function;
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.model.View;
import uk.co.timwise.sqlhawk.util.LineWriter;


public class ScmDbWriter {
	private final Logger logger = Logger.getLogger(getClass().getName());

	public void writeForSourceControl(File outputDir, Database db) throws IOException {
		writeProcs(outputDir, db.getProcs());
		writeFunctions(outputDir, db.getFunctions());
		writeViews(outputDir, db.getViews());
	}

	private void writeProcs(File outputDir, Collection<Procedure> procs) throws IOException {		
		File procFolder = new File(outputDir, "Procedures");
		ensureFolder(procFolder);
		for (Procedure proc : procs) {
			LineWriter out = new LineWriter(new File(procFolder, proc.getName() + ".sql"), "UTF-8");
			out.writeln(proc.getDefinition()); //writeln() in preference to write() in order to make patches for sql files cleaner (\n on every line so new lines at end don't affect original last line)
			out.close();		
		}
	}

	private void writeFunctions(File outputDir, Collection<Function> functions) throws IOException {		
		File functionFolder = new File(outputDir, "Functions");
		ensureFolder(functionFolder);
		for (Function function : functions) {
			LineWriter out = new LineWriter(new File(functionFolder, function.getName() + ".sql"), "UTF-8");
			out.writeln(function.getDefinition()); //writeln() in preference to write() in order to make patches for sql files cleaner (\n on every line so new lines at end don't affect original last line)
			out.close();		
		}
	}

	private void writeViews(File outputDir, Collection<View> views) throws IOException {		
		File viewFolder = new File(outputDir, "Views");
		ensureFolder(viewFolder);
		for (View view : views) {
			String viewSql = view.getDefinition();
			if (viewSql==null) {
				logger.warning("No definition found for view " + view.getName());
				continue; //don't write empty file.
			}
			LineWriter out = new LineWriter(new File(viewFolder, view.getName() + ".sql"), "UTF-8");
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
