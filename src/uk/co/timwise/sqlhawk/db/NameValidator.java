package uk.co.timwise.sqlhawk.db;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *  "macro" to validate that a table is somewhat valid
 */
public class NameValidator {
    /**
	 * 
	 */
	private final String clazz;
    private final Pattern include;
    private final Pattern exclude;
    private final Set<String> validTypes;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final boolean fineEnabled = logger.isLoggable(Level.FINE);

    /**
     * @param clazz table or view
     * @param include
     * @param exclude
     * @param verbose
     * @param validTypes pass null to allow any type
     */
    public NameValidator(String clazz, Pattern include, Pattern exclude, String[] validTypes) {
		this.clazz = clazz;
        this.include = include;
        this.exclude = exclude;
        if (validTypes != null) {
            this.validTypes = new HashSet<String>();
            for (String type : validTypes)
            {
                this.validTypes.add(type.toUpperCase());
            }
        } else {
        	this.validTypes = null;
        }
    }

    public boolean isValid(String name) {
		return isValid(name, null);
	}

	/**
     * Returns <code>true</code> if the table/view name is deemed "valid"
     *
     * @param name name of the table or view
     * @param type type as returned by metadata.getTables():TABLE_TYPE
     * 	or null if not filtering by type
     * @return
     */
    public boolean isValid(String name, String type) {
        //some databases (MySQL) return more types of object than we wanted
    	//these can be filtered out with a validTypes list.
        if (validTypes!=null && type!=null && !validTypes.contains(type.toUpperCase())){
            if (fineEnabled) {
                logger.finest("Excluding " + clazz + " " + name +
                            ": unwanted object type");
            }
            return false;
        }

        // Oracle 10g introduced problematic flashback tables
        // with bizarre illegal names
        if (name.indexOf("$") != -1) {
            if (fineEnabled) {
                logger.finest("Excluding " + clazz + " " + name +
                            ": embedded $ implies illegal name");
            }
            return false;
        }

        if (exclude.matcher(name).matches()) {
            if (fineEnabled) {
                logger.finest("Excluding " + clazz + " " + name +
                            ": matches exclusion pattern \"" + exclude + '"');
            }
            return false;
        }

        boolean valid = include.matcher(name).matches();
        if (fineEnabled) {
            if (valid) {
                logger.finest("Including " + clazz + " " + name +
                            ": matches inclusion pattern \"" + include + '"');
            } else {
                logger.finest("Excluding " + clazz + " " + name +
                            ": doesn't match inclusion pattern \"" + include + '"');
            }
        }
        return valid;
    }
}