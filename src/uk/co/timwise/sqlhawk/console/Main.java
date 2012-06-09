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
package uk.co.timwise.sqlhawk.console;

import uk.co.timwise.sqlhawk.config.Config;
import uk.co.timwise.sqlhawk.config.InvalidConfigurationException;
import uk.co.timwise.sqlhawk.config.MissingRequiredParameterException;
import uk.co.timwise.sqlhawk.controller.SchemaMapper;
import uk.co.timwise.sqlhawk.db.read.ConnectionFailure;
import uk.co.timwise.sqlhawk.db.read.EmptySchemaException;
import uk.co.timwise.sqlhawk.db.read.ProcessExecutionException;
import uk.co.timwise.sqlhawk.ui.MainFrame;

public class Main {
	public static void main(String[] argv) throws Exception {
		if (argv.length == 1 && argv[0].equals("-gui")) { // warning: serious temp hack
			new MainFrame().setVisible(true);
			return;
		}

		//print welcome message to console
		String version = Main.class.getPackage().getImplementationVersion();
		if (version!=null) //will be null if run outside package, i.e. in eclipse.
			System.out.println("sqlHawk " + Main.class.getPackage().getImplementationVersion());
		System.out.println("More information at http://timabell.github.com/sqlHawk/");
		System.out.println("License: GPLv3.");
		System.out.println();

		try {
			// load config
			ArgParser argParser = new ArgParser();
			Config config = argParser.Parse(argv);
			config.Validate();

			// run requested operations
			SchemaMapper mapper = new SchemaMapper();
			mapper.RunMapping(config);
		} catch (ConnectionFailure couldntConnect) {
			// failure already logged
			System.exit(3);
		} catch (EmptySchemaException noData) {
			// failure already logged
			System.exit(2);
		} catch (MissingRequiredParameterException missingParam) {
			System.err.println(missingParam.getMessage());
			System.exit(1);
		} catch (InvalidConfigurationException badConfig) {
			System.err.println();
			if (badConfig.getParamName() != null)
				System.err.println("Bad parameter specified for " + badConfig.getParamName());
			System.err.println(badConfig.getMessage());
			if (badConfig.getCause() != null && !badConfig.getMessage().endsWith(badConfig.getMessage()))
				System.err.println(" caused by " + badConfig.getCause().getMessage());
			System.exit(1);
		} catch (ProcessExecutionException badLaunch) {
			System.err.println(badLaunch.getMessage());
			System.exit(1);
		} catch (Exception exc) {
			exc.printStackTrace();
			System.exit(1);
		}
	}
}
