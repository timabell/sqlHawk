package net.sourceforge.schemaspy.model;

import java.util.Properties;

public class Proc implements Comparable<Proc> {
    private final String schema;
    private final String name;
    private String definition;

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }
    
    public String getDefinition(){
    	return definition;
    }

    public Proc(String schema, String name, String definition) {
        this.schema = schema;
        this.name = name;
        this.definition = definition;
    }

    /**
     * compare stored proc based on name and definition
     */
	@Override
	public int compareTo(Proc proc) {
		int nameCompare = name.compareTo(proc.name);
		if (nameCompare!=0)
			return nameCompare;
		return definition.compareTo(proc.definition);
	}

}
