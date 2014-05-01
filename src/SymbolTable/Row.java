package SymbolTable;

public class Row {

	String ID, kind, type, returnValues, inputParameters, paramKinds;
	int offset, size, nestLevel;
	
    public Row(String ID_in, String kind_in, String type_in, int offset_in, int size_in, String returnValues_in, String inputParameters_in, int nestLevel, String paramKinds) {
    	ID = ID_in;
    	kind = kind_in;
    	type = type_in;
    	offset = offset_in;
    	size = size_in;
    	returnValues = returnValues_in;
    	inputParameters = inputParameters_in;
    	this.paramKinds = paramKinds;
    	this.nestLevel = nestLevel;
    }
    
    public void printRow()
    {
    	//System.out.println("ID: " + ID + " Kind: " + kind + " Type: " + type + " Return Values: " + returnValues + 
    						//" Input Parameters: " + inputParameters);

    	System.out.println(String.format("%15s %10s %10s   %4d   %4d      %10s%46s ", ID, kind, type, offset, size, returnValues, inputParameters));
    }
    
    public int getNestingLevel(){
    	return nestLevel;
    }
    
    public String getID() {
    	return ID;
    }
    public String getKind() {
    	return kind;
    }
    public String getType() {
    	return type;
    }
    public int getOffset() {
    	return offset;
    }
    public int getSize() {
    	return size;
    }
    public String getReturnValues() {
    	return returnValues;
    }
    public String getInputParameters() {
    	return inputParameters;
    }
    
    public String getParameterKinds(){
    	return paramKinds;
    }
    
    public void setID(String ID_in) {
    	ID = ID_in;
    }
    public void setKind(String kind_in) {
    	kind = kind_in;
    }
    public void setType(String type_in) {
    	type = type_in;
    }
    public void setOffset(int offset_in) {
    	offset = offset_in;
    }
    public void setSize(int size_in) {
    	size = size_in;
    }
    public void setReturnValues(String returnValues_in) {
    	returnValues = returnValues_in;
    }
    public void setInputParameters(String inputParameters_in) {
    	inputParameters = inputParameters_in;
    }
    
    public String[] getSeparatedInputParameters() {
    	return inputParameters.split(" ");
    }
    
}
