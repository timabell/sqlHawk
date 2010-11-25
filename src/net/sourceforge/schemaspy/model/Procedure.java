package net.sourceforge.schemaspy.model;

public class Procedure implements Comparable<Procedure> {
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

    public Procedure(String schema, String name, String definition) {
        this.schema = schema;
        this.name = name;
        this.definition = definition;
    }

    /**
     * compare stored proc based on name and definition
     */
	@Override
	public int compareTo(Procedure proc) {
		int nameCompare = name.compareTo(proc.name);
		if (nameCompare!=0)
			return nameCompare;
		return definition.compareTo(proc.definition);
	}
}
