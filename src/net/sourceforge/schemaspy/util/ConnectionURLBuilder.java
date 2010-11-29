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

import java.util.ArrayList;
import java.util.List;
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
     */
    public String buildUrl(Config config, Properties properties) {
        List<String> madeUpCommandLineArguments = new ArrayList<String>();

        for (String key : config.getDbSpecificOptions().keySet()) {
            madeUpCommandLineArguments.add((key.startsWith("-") ? "" : "-") + key);
            madeUpCommandLineArguments.add(config.getDbSpecificOptions().get(key));
        }
        madeUpCommandLineArguments.addAll(config.getConnectionParameters());

        DbSpecificConfig dbConfig = new DbSpecificConfig(config.getDbType());
        List<DbSpecificOption> options = dbConfig.getOptions();
        String connectionURL = buildUrlFromArgs(madeUpCommandLineArguments, properties, config, options);
        logger.config("connectionURL: " + connectionURL);
        return connectionURL;
    }

    private String buildUrlFromArgs(List<String> madeUpCommandLineArguments, Properties properties, Config config, List<DbSpecificOption> options) {
        String connectionSpec = properties.getProperty("connectionSpec");
        for (DbSpecificOption option : options) {
            option.setValue(getParam(madeUpCommandLineArguments, option, config));

            // replace e.g. <host> with <myDbHost>
            connectionSpec = connectionSpec.replaceAll("\\<" + option.getName() + "\\>", option.getValue().toString());
        }

        return connectionSpec;
    }

    private String getParam(List<String> args, DbSpecificOption option, Config config) {
        String param = null;
        int paramIndex = args.indexOf("-" + option.getName());

        if (paramIndex < 0) {
            if (config != null)
                param = null;// config.getParam(option.getName());  // not in args...might be one of
                                                            // the common db params
            if (param == null)
                throw new Config.MissingRequiredParameterException(option.getName(), option.getDescription(), true);
        } else {
            args.remove(paramIndex);
            param = args.get(paramIndex).toString();
            args.remove(paramIndex);
        }

        return param;
    }
}
