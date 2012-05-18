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
package uk.co.timwise.sqlhawk.scm.read;

import java.io.File;
import java.util.Map;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.util.CaseInsensitiveMap;
import uk.co.timwise.sqlhawk.util.FileHandling;


public class ScmDbReader {

	public static Database Load(Config config, File inputDir) throws Exception {
		Database db = new Database(null, null);
		db.setProcs(readProcs(inputDir));
		return db;
	}

	private static Map<String, Procedure> readProcs(File inputDir) throws Exception{
		File procFolder = new File(inputDir, "Procedures");
		Map<String, Procedure> procs = new CaseInsensitiveMap<Procedure>();
		if (!procFolder.isDirectory())
			return procs; //nothing to do
		File[] files = procFolder.listFiles();
		for(File file : files){
			if (!file.getName().endsWith(".sql")) //skip non sql files
				continue;
			String name = file.getName();
			name = name.substring(0, name.length()-4);// trim extension from filename
			String definition = FileHandling.readFile(file);
			Procedure proc = new Procedure(null, name, definition);
			procs.put(name, proc);
		}
		return procs;
	}
}
