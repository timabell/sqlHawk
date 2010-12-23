package uk.co.timwise.sqlhawk.model;

public class Function implements Comparable<Function> {
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

    public Function(String schema, String name, String definition) {
        this.schema = schema;
        this.name = name;
        //Remove all leading and trailing and whitespace for the function sql when saving.
        //This prevents the alter code failing if there is whitespace before the CREATE block.
        //It also means trailing and leading whitespace will be ignored when checking if an sp has been updated
        // which seems sensible.
        this.definition = definition.trim(); 
    }

    /**
     * compare stored function based on name and definition
     */
	@Override
	public int compareTo(Function function) {
		int nameCompare = name.compareTo(function.name);
		if (nameCompare!=0)
			return nameCompare;
		return definition.compareTo(function.definition);
	}
}
