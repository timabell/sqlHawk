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
package uk.co.timwise.sqlhawk.scm.read;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.model.Database;
import uk.co.timwise.sqlhawk.model.Function;
import uk.co.timwise.sqlhawk.model.ISqlObject;
import uk.co.timwise.sqlhawk.model.Procedure;
import uk.co.timwise.sqlhawk.model.View;
import uk.co.timwise.sqlhawk.util.CaseInsensitiveMap;
import uk.co.timwise.sqlhawk.util.FileHandling;


public class ScmDbReader {
	private final Logger logger = Logger.getLogger(getClass().getName());

	public Database Load(Config config) throws Exception {
		File inputDir = config.getTargetDir();
		logger.info("Loading database definitions from folder '" + inputDir + "'");
		if (!inputDir.isDirectory()) {
			throw new Exception("specified scm input folder not found: " + inputDir);
		}
		Database db = new Database(null, null);
		db.setProcs(readSqlObjects(new File(inputDir, "Procedures"), Procedure.class));
		db.setViews(readSqlObjects(new File(inputDir, "Views"), View.class));
		db.setFunctions(readSqlObjects(new File(inputDir, "Functions"), Function.class));
		return db;
	}

	private <TSqlObject extends ISqlObject>
				Map<String, TSqlObject> readSqlObjects(File inputDir, Class<TSqlObject> clazz)
			throws Exception{
		logger.fine("Loading scm files from " + inputDir);
		Map<String, TSqlObject> sqlObjects = new CaseInsensitiveMap<TSqlObject>();
		if (!inputDir.isDirectory()) {
			logger.warning(inputDir + " not found");
			return sqlObjects; //nothing to do
		}
		File[] files = inputDir.listFiles();
		for(File file : files){
			if (!file.getName().endsWith(".sql")) {
				//skip non sql files
				logger.finest("Ignoring non .sql file " + file);
				continue;
			}
			logger.fine("Loading " + file);
			String name = file.getName();
			name = name.substring(0, name.length()-4);// trim extension from filename
			String definition = FileHandling.readFile(file);
			TSqlObject sqlObject = clazz.newInstance();//.Create(name, definition);
			sqlObject.setName(name);
			sqlObject.setDefinition(definition);
			sqlObjects.put(name, sqlObject);
		}
		return sqlObjects;
	}
}
