/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy.util;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.sourceforge.schemaspy.Config;

/**
 * @author John Currier
 */
public class ConnectionURLBuilder {
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * 
     * @param config
     * @param properties
     * @throws Exception 
     */
    public String buildUrl(Config config, Properties properties) throws Exception {
        DbSpecificConfig dbConfig = new DbSpecificConfig(config.getDbType());
        List<DbSpecificOption> driverOptions = dbConfig.getOptions();
        String connectionURL = buildUrlFromArgs(properties, config, driverOptions);
        logger.config("connectionURL: " + connectionURL);
        return connectionURL;
    }

    private String buildUrlFromArgs(Properties properties, Config config, List<DbSpecificOption> driverOptions) throws Exception {
        String connectionSpec = properties.getProperty("connectionSpec");
        Map<String, String> extraConnectionOptions = config.getExtraConnectionOptions();
        for (DbSpecificOption option : driverOptions) {
        	//options available directly in hard coded command line arguments of sqlHawk
        	if (option.getName().equalsIgnoreCase("host") && config.getHost() != null)
        		option.setValue(config.getHost());
        	else if (option.getName().equalsIgnoreCase("port") && config.getPort() != null)
        		option.setValue(config.getPort());
        	else if (option.getName().equalsIgnoreCase("db") && config.getDb() != null)
        		option.setValue(config.getDb());
        	else if (option.getName().equalsIgnoreCase("instance") && config.getDatabaseInstance() != null)
        		option.setValue(config.getDatabaseInstance());
        	//options available through the "connection-options" multi-part command line argument of sqlHawk
        	else if (extraConnectionOptions.containsKey(option.getName()))
        		option.setValue(extraConnectionOptions.get(option.getName()));
        	else
        		throw new Exception("The specified database driver requires option '" + option.getName() + "' which has not been supplied. You can supply extra options with --connection-options (see --help for more information)");
            //perform actual replacement in driver string e.g. <host> with <myDbHost>
            connectionSpec = connectionSpec.replaceAll("\\<" + option.getName() + "\\>", option.getValue().toString());
        }
        return connectionSpec;
    }
}
