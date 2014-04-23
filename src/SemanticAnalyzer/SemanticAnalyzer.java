package SemanticAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import Tokenizer.Token;
import SymbolTable.*;



/*
 * The semantic analyzer will write code for the umachine program, given instructions and input from the parser
 * If an instruction cannot be done, an error is given and the program quits
 */
public class SemanticAnalyzer{
	
	private static String FILE_NAME = "uCompiler.il";
	private static String[][] labels = new String[50][3];
	private static String topOfStack = "empty";
	private static String topOfStackBeforeFunctionCall = "empty";
	private static String firstSide = "empty";
	private static boolean makeNeg = false;
	private static boolean makePos = false;
	private static FileWriter program;
	private static int labelnum = 0;
	
	public SemanticAnalyzer(){
		try {
			File file = new File(FILE_NAME);
			if(!(file.exists())){
				file.createNewFile();
			}
			program = new FileWriter(file);
			program.write("BR L1\n");
		} catch (IOException e) {
			System.out.println("File Program.il doesn't exist, stopping");
			System.exit(1);
		}
		
	}
	
	/*
	 * Add a label for a symbol table. Each symbol table will have one set of statements, so one label for one table is sufficient
	 * The label is stored in an array of strings, with 0 indexing the name of the table, 1 indexing the kind of table, and 2 indexing the label itself
	 */
	public void addLabel(SymbolTable symTab){
		labels[labelnum][0] = symTab.getName();
		if(symTab.getParent() != null){
			labels[labelnum][1] = symTab.getParent().findVariable(symTab.getName()).getKind();
		}
		else{
			labels[labelnum][1] = "program";
		}
		labels[labelnum][2] = "L" + (labelnum + 1);
		labelnum++;
	}
	
	/*
	 * Add a label for any control flow statement such as if/else/looping
	 */
	public void addLabel(String name, int num){
		labels[labelnum][0] = name + num;
		labels[labelnum][1] = name;
		labels[labelnum][2] = "L" + (labelnum + 1);
		labelnum++;
	}

	/*
	 * create a branch to a name, and stipulate when you want to branch to that name
	 */
	public void branch(String name, String when){
		if(when.equals("always")){
			for(int i = labelnum -1; i >= 0; i--){
				if(labels[i][0].equals(name)){
					write("BR " + labels[i][2]);
				}
			}
		}
		
		else if(when.equals("true")){
			if(!(topOfStack.equals("boolean"))){
				error("Branching on term that is not a boolean on " + name + " call" + " value is " + topOfStack );
			}
			else{
				for(int i = labelnum -1; i >= 0; i--){
					if(labels[i][0].equals(name)){
						write("BRTS " + labels[i][2]);
					}
				}
			}
		}
		else if(when.equals("false")){
			if(!(topOfStack.equals("boolean"))){
				error("Branching on term that is not a boolean on " + name + " call" + " value is " + topOfStack);
			}
			else{
				for(int i = labelnum -1; i >= 0; i--){
					if(labels[i][0].equals(name)){
						write("BRFS " + labels[i][2]);
					}
				}
			}
		}
		else if(when.equals("equals")){
			for(int i = labelnum -1; i >= 0; i--){
				if(labels[i][0].equals(name)){
					if(topOfStack.equals("integer")||topOfStack.equals("empty")){
						write("CMPEQS");
					}
					else{
						write("CMPEQSF");
					}
					write("BRTS " + labels[i][2]);
				}
			}
		}
		
		topOfStack = "empty";
	}

	/*
	 * Simply place a label at the spot we are currently at
	 */
	public void placeLabel(String name){
		for(int i = labelnum -1; i >= 0; i--){
			if(labels[i][0].equals(name)){
				write(labels[i][2] + ":");
			}
		}
	}
	
	/*
	 * Checks what kind of value is being pushed, either a variable or a literal
	 */
	public void pushCheck(Token variable, SymbolTable symTab){
		Row info = symTab.findVariable(variable.getLexeme());
		
		if (info == null){
			pushLiteral(variable);
		}
		else{
			pushVariable(variable, symTab);
		}
		
	}
	
	/*
	 * Finds out what kind of literal is being pushed, and writes accordingly as well as enforces stack type
	 */
	private void pushLiteral(Token variable){
		
		if(topOfStack.equals("empty")){
			if(variable.getToken().equals("MP_STRING_LIT")){
				write("PUSH #\"" + variable.getLexeme().substring(1,variable.getLexeme().length() -1) + "\"");
				topOfStack = "string";
			}
			else if(variable.getToken().equals("MP_FIXED_LIT")||variable.getToken().equals("MP_FLOAT_LIT")){
				topOfStack = "float";
				write("PUSH #" + variable.getLexeme());
			}
			else if(variable.getToken().equals("MP_TRUE")){
				write("PUSH #1");
				topOfStack = "boolean";
			}
			else if(variable.getToken().equals("MP_FALSE")){
				write("PUSH #0");
				topOfStack = "boolean";
			}
			else if(variable.getToken().equals("MP_INTEGER_LIT")){
				topOfStack = "integer";
				write("PUSH #" + variable.getLexeme());
			}
		}
		
		else if(topOfStack.equals("boolean")){
			if (variable.getToken().equals("MP_NOT")){
				if(topOfStack.equals("integer")){
					error("Can't NOT a non-boolean Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
				write("PUSH #1");
				write("SUBS");
				write("NEGS");
			}
			else{
				error("Doing illegal operations on boolean value Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());

			}
		}
		
		else if(topOfStack.equals("integer")){
			if(variable.getToken().equals("MP_FIXED_LIT")||variable.getToken().equals("MP_FLOAT_LIT")){
				write("CASTSF");
				write("PUSH #" + variable.getLexeme());
				topOfStack = "float";
			}
			else if(variable.getToken().equals("MP_TRUE")){
				if(topOfStack.equals("integer")){
					error("Can't add boolean TRUE as int value Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
				write("PUSH #1");
			}
			else if(variable.getToken().equals("MP_FALSE")){
				if(topOfStack.equals("integer")){
					error("Can't add boolean False as int value Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
				write("PUSH #0");
			}
			else if(variable.getToken().equals("MP_INTEGER_LIT")){
				write("PUSH #" + variable.getLexeme());
				topOfStack = "integer";
			}
			else{
				error("Found a "+ variable.getToken()+" called "+variable.getLexeme() + "in expression");
			}
			
			if(makeNeg || makePos){
				write("NEGS");
				makeNeg = false;
				makePos = false;
			}
			
		}
		else if(topOfStack.equals("float")){
			if(variable.getToken().equals("MP_FIXED_LIT")||variable.getToken().equals("MP_FLOAT_LIT")){
				write("PUSH #" + variable.getLexeme());
			}
			else if(variable.getToken().equals("MP_TRUE")){
				if(topOfStack.equals("integer")){
					error("Can't add boolean TRUE as float value Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
			}
			else if(variable.getToken().equals("MP_FALSE")){
				if(topOfStack.equals("integer")){
					error("Can't add boolean FALSE as float value Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
			}
			else if (variable.getToken().equals("MP_NOT")){
				if(topOfStack.equals("integer")){
					error("Can't NOT a non-boolean Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
			}
			else if(variable.getToken().equals("MP_INTEGER_LIT")){
				write("PUSH #" + variable.getLexeme());
				write("CASTSF");
			}
			else{
				error("Found a "+ variable.getToken()+" called "+variable.getLexeme() + "in expression");
			}
			
			if(makeNeg||makePos){
				write("NEGSF");
				makeNeg = false;
				makePos = false;
			}
		}
		
		
	}
	
	/*
	 * Creates the call to a function or procedure, given a list of the parameters given, the current symbol table, and the token of the procedure or function to call to
	 */
	public void functionProcedureCall(String paramTypesList, SymbolTable symTab, Token call){
		Row callinfo = symTab.findVariable(call.getLexeme());
		String[] paramsGiven = paramTypesList.split(" ");
		String[] actualParams = callinfo.getInputParameters().split(" ");
		if(paramsGiven.length != actualParams.length){
			error("Wrong number of parameters for call " + call.getLexeme() + " at line:" + call.getLineNumber() + " col:" + call.getColumnNumber());
		}
		
		for(int i = 0; i < paramsGiven.length; i++){
			if(!(paramsGiven[i].equals(actualParams[i]))){
				error("Incorrect parameter type. Looking for "+actualParams[i]+" but found "+paramsGiven[i] + " line:" + call.getLineNumber() + " col:" + call.getColumnNumber());
			}
		}
		
		
		for(int i = labelnum -1; i >= 0; i--){//make the call
			if(labels[i][0].equals(callinfo.getID())){
				write("CALL " + labels[i][2]);
			}
		}
		if(!(actualParams[0].equals(""))) write("SUB SP #" + actualParams.length + " SP");//deconstruct the parameters leaving the return value on top, or no value
		
		if(symTab.findVariable(call.getLexeme()).getKind().equals("function")){
			String type = symTab.findVariable(call.getLexeme()).getType();
			if(topOfStackBeforeFunctionCall.equals("empty")){
				topOfStack = type;
				topOfStackBeforeFunctionCall = "empty";
			}
			else if(topOfStackBeforeFunctionCall.equals("float")){
				if(type.equals("integer")||type.equals("boolean")){
					topOfStack = "float";
					write("CASTSF");
					topOfStackBeforeFunctionCall = "empty";
				}
				else if(type.equals("float")){
					topOfStack = "float";
					topOfStackBeforeFunctionCall = "empty";
				}
				else{
					error("String concatenation and stuff not implemented yet Line:" + call.getLineNumber() + " col:" + call.getColumnNumber());
				}
			}
			else if(topOfStackBeforeFunctionCall.equals("integer")||topOfStackBeforeFunctionCall.equals("boolean")){
				if(type.equals("integer")||type.equals("boolean")){
					topOfStack = type;
					topOfStackBeforeFunctionCall = "empty";
				}
				else if(type.equals("float")){
					topOfStack = "float";
					write("CASTSF");
					topOfStackBeforeFunctionCall = "empty";
				}
				else{
					error("String concatenation and stuff not implemented yet Line:" + call.getLineNumber() + " col:" + call.getColumnNumber());
				}
			}

		}
		
	}
	
	/*
	 * Whenever a comma is reached when finding parameters, it clears the type on top of the stack so parameters are not dependent on what is currently on the stack
	 */
	public void comma(){
		topOfStack = "empty";
	}
	
	/*
	 * Push a variable onto the stack, given the current symbol table, and enforce the stack type
	 */
	private void pushVariable(Token variable, SymbolTable symTab){
		
		Row info = symTab.findVariable(variable.getLexeme());
		
		if (topOfStack.equals("empty")){
			write("PUSH " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
			topOfStack =info.getType();
		}
		else if(topOfStack.equals("integer")||topOfStack.equals("boolean")){
			
			if(info.getType().equals("float")){
				write("CASTSF");
				write("PUSH " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				topOfStack =info.getType();
			}
			else if(info.getType().equals("boolean")||info.getType().equals("integer")){
				write("PUSH " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				if(info.getType().equals("integer")) topOfStack = info.getType();
			}
			else{
				error("String found in statement");
			}
			
			if(makeNeg || makePos){
				write("NEGS");
				makeNeg = false;
				makePos = false;
			}
			
		}
		else if(topOfStack.equals("float")){
			if(info.getType().equals("boolean")||info.getType().equals("integer")){
				write("PUSH " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				write("CASTSF");
			}
			else if(info.getType().equals("float")){
				write("PUSH " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
			}
			else{
				error("String found in statement");
			}
			if(makeNeg || makePos){
				write("NEGSF");
				makeNeg = false;
				makePos = false;
			}
			
		}
	}
	
	/*
	 * Pushes room for a return variable in a function call, as well as store the type of the stack before the function call is made
	 */
	public void pushRoomForRetVal(String type){
		
		topOfStackBeforeFunctionCall = topOfStack;
		topOfStack = "empty";
		
		if(type.equals("float")){
			write("PUSH #0.0");
		}
		else if (type.equals("string")){
			write("PUSH #\"\"");
		}
		else{
			write("PUSH #0");
		}
	}
	
	/*
	 * Pop the top of the stack into the variable, given the current symbol table, and also enforces type checking and implicit conversions
	 */
	public void pop(Token variable, SymbolTable symTab){
		
		Row info = symTab.findVariable(variable.getLexeme());
		
		if (info == null){
			System.out.println(symTab.getName());
			error("Trying to assign to value that is not a variable: " + variable.getLexeme() + ". Line:"+ variable.getLineNumber() + " Col:" + variable.getColumnNumber());
		}
		else{
			//write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
			if(topOfStack.equals("boolean")){
				if(info.getType().equals("boolean")){
					write("POP" + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else{
					error("Assigning no boolean value to boolean variable Line:" + variable.getLineNumber() + " col:" + variable.getColumnNumber());
				}
			}
			
			else if(topOfStack.equals("integer")){
				if(info.getType().equals("float")){
					write("CASTSF");
					write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else if(info.getType().equals("integer")||info.getType().equals("boolean")){
					write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else{
					error("Implicit conversion from " + info.getType() + " to " + topOfStack + " not possible Line:" + variable.getLineNumber() + " col: " + variable.getColumnNumber());
				}
			}
			else if(topOfStack.equals("float")){
				if(info.getType().equals("float")){
					write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else if(info.getType().equals("integer")||info.getType().equals("boolean")){
					write("CASTSI");
					write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else{
					error("Implicit conversion from " + info.getType() + " to " + topOfStack + " not possible Line:" + variable.getLineNumber() + " col: " + variable.getColumnNumber());
				}
			}
			else if(topOfStack.equals("string")){
				if(info.getType().equals("string")){
					write("POP " + info.getOffset() + "(D" + info.getNestingLevel() + ")");
				}
				else{
					error("Implicit conversion from " + info.getType() + " to " + topOfStack + " not possible Line:" + variable.getLineNumber() + " col: " + variable.getColumnNumber());
				}
			}
			topOfStack = "empty";
		}
		
	}
	
	/*
	 * Pushes "to" as 1, and downto as "-1" onto the stack, while enforcing stack type
	 */
	public void stepValue(Token Tokenvalue){
		String value = Tokenvalue.getLexeme();
		if(topOfStack.equals("integer")|| topOfStack.equals("boolean")){
			if(value.equals("to")){
				write("PUSH #1");
			}
			else{
				write("PUSH #-1");
			}
		}
		else{
			if(value.equals("to")){
				write("PUSH #1.0");
			}
			else{
				write("PUSH #-1.0");
			}
		}
	}
	
	/*
	 * Simply flips a boolean, so we no to make whatever is pushed on next into a negative
	 */
	public void makeNeg(){
		makeNeg = true;
		
	}
	
	/*
	 * Same as makeNeg semantically, this is just an alias for readability's sake
	 */
	public void makePos(){
		makePos = true;
		
	}
	
	/*
	 * Add a math operation while enforcing stack type and operation ability
	 */
	public void addOp(Token operation){
		
		if(topOfStack.equals("boolean")){
			error("trying math operation on boolean value Line" + operation.getLineNumber() + " col" + operation.getColumnNumber());
		}
		
		else if(topOfStack.equals("integer")){
			//boolean operators
			if(topOfStack.equals("boolean") && firstSide.equals("boolean") && operation.getToken().equals("MP_OR")){
				write("ORS");
				firstSide = "empty";
			}
			else if(topOfStack.equals("boolean") && firstSide.equals("boolean") && operation.getToken().equals("MP_AND")){
				write("ANDS");
				firstSide = "empty";
			}
			//add to the error statement
			
			else if(operation.getToken().equals("MP_AND")||operation.getToken().equals("MP_OR")){
				error("Can't use operation " + operation.getLexeme() + " on type " + topOfStack + " when first side is " + firstSide + " Line:" + operation.getLineNumber() + " col:" + operation.getColumnNumber());
			}
			else {
				//integer or boolean operators
				if(operation.getLexeme().equals("+")){
					write("ADDS");
				}
				else if(operation.getLexeme().equals("-")){
					write("SUBS");
				}
				else if(operation.getToken().equals("MP_DIV")){
					write("DIVS");
				}
				else if(operation.getLexeme().equals("*")){
					write("MULS");
				}
			else{
				error("Can't use operation " + operation.getLexeme() + " on type " + topOfStack + " Line:" + operation.getLineNumber() + " col:" + operation.getColumnNumber());
			}
			}
			
		}
		else if(topOfStack.equals("float")){
			if(operation.getLexeme().equals("+")){
				write("ADDSF");
			}
			else if(operation.getLexeme().equals("-")){
				write("SUBSF");
			}
			else if(operation.getLexeme().equals("/")){
				write("DIVSF");
			}
			else if(operation.getToken().equals("MP_DIV")){
				write("DIVSF");
				write("CASTI");
			}
			else if(operation.getLexeme().equals("*")){
				write("MULSF");
			}
			else{
				error("Can't use operation " + operation.getLexeme() + " on type " + topOfStack + " Line:" + operation.getLineNumber() + " col:" + operation.getColumnNumber());
			}
		}
		else{
			error("String operations and concatatenations not implemented at this point Line: " + operation.getLineNumber() + " col:" + operation.getColumnNumber());
		}
		
	}

	public void checkBothSides(){
		firstSide = topOfStack;
		topOfStack = "empty";
	}
	
	/*
	 * Add a comparison operation while enforcing stack type
	 */
	public void addComp(Token comparator){
		
		if(topOfStack.equals("integer")||topOfStack.equals("empty")){
			if(comparator.getLexeme().equals(">")){
				write("CMPGTS");
			}
			else if(comparator.getLexeme().equals(">=")){
				write("CMPGES");
			}
			else if(comparator.getLexeme().equals("<")){
				write("CMPLTS");
			}
			else if(comparator.getLexeme().equals("<=")){
				write("CMPLES");
			}
			else if(comparator.getLexeme().equals("=")){
				write("CMPEQS");
			}
			else if(comparator.getLexeme().equals("<>")){
				write("CMPNES");
			}
		}
		else if(topOfStack.equals("float")){
			if(comparator.getLexeme().equals(">")){
				write("CMPGTSF");
			}
			else if(comparator.getLexeme().equals(">=")){
				write("CMPGESF");
			}
			else if(comparator.getLexeme().equals("<")){
				write("CMPLTSF");
			}
			else if(comparator.getLexeme().equals("<=")){
				write("CMPLESF");
			}
			else if(comparator.getLexeme().equals("=")){
				write("CMPEQSF");
			}
			else if(comparator.getLexeme().equals("<>")){
				write("CMPNESF");
			}
		}
		else{
			error("Problem adding comparison on line:" + comparator.getLineNumber() + " col:" + comparator.getColumnNumber());
		}
		
		topOfStack = "boolean";
	}
	
	/*
	 * Create read statements given a list of variables
	 */
	public void read(ArrayList<Token> params,SymbolTable symTab){
		Row current;
		for(int i = 0; i < params.size(); i++){
			current = symTab.findVariable(params.get(i).getLexeme());
			if (current == null){
				error("Can't read into variable " + params.get(i).getLexeme() + " at line:"+ params.get(i).getLineNumber() + " col:" + params.get(i).getColumnNumber());
			}
			else{
				String type = current.getType();
				if(type.equals("integer")||type.equals("boolean")){
					write("RD " + current.getOffset() +"(D" + current.getNestingLevel() + ")");
				}
				else if(type.equals("float")){
					write("RDF " + current.getOffset() + "(D" + current.getNestingLevel() + ")");
				}
				else{
					write("RDS " + current.getOffset() + "(D" + current.getNestingLevel() + ")");
				}
			}
		}
	}
	
	/*
	 * Write whatever is on top of the stack, and clear the stack type
	 */
	public void write(){
		write("WRTS");
		topOfStack = "empty";
	}
	
	/*
	 * Write an empty line and clear the stack type
	 */
	public void writeln(){
		write("WRTLN #\"\"");
		topOfStack = "empty";
	}
	
	/*
	 * Given a symbol table, figure out it's type, and "create" room on the stack as well as move parameters in, if any are given
	 */
	public void statementStart(SymbolTable symTab){
		
		int i;
		for(i = labelnum-1; i >= 0; i--){
			if(labels[i][0].equals(symTab.getName())){
				write(labels[i][2] + ":");
				write("PUSH D" + symTab.getNestingLevel());
				write("MOV SP D" + symTab.getNestingLevel());
				write("ADD SP #" + symTab.getSize() + " SP");
				break;
			}
		}
		if(labels[i][1].equals("function")||labels[i][1].equals("procedure")){//grab input variables from under our position
			String[] params = symTab.getParameters().split(" ");
			for(int j = params.length; j > 0; j--){
				write("MOV " + (j - params.length - 3) + "(D" + symTab.getNestingLevel() + ") " + j + "(D" + symTab.getNestingLevel() + ")");
			}
		}
	}
	
	/*
	 * Deconstruct the statement from the stack, and store the return variable, if any
	 */
	public void statementEnd(SymbolTable symTab){

		if(symTab.getParent() != null && symTab.getParent().findVariable(symTab.getName()).getKind() == "function"){
			String[] params = symTab.getParameters().split(" ");
			write("MOV " + (symTab.findVariable(symTab.getName()).getOffset()) + "(D" + symTab.getNestingLevel() + ") " + (-3 - params.length) + "(D" + symTab.getNestingLevel() + ")" );//store return variable at the very bottom of everything

		}
		
		write("SUB SP #" + symTab.getSize() + " SP");
		write("POP D" + symTab.getNestingLevel());
		for(int i = labelnum - 1; i >= 0; i--){
			if(labels[i][0].equals(symTab.getName())&&!(labels[i][1].equals("program"))){
				write("RET");
			}
		}
		
	}
	
	/*
	 * Return the type on top of the stack
	 */
	public  String topOfStackType(){
		return topOfStack;
	}
	
	/*
	 * End the analyzer, append "HLT", and close the file for writing
	 */
	public void end(){
		try{
			program.write("HLT\n");
			program.close();
		}catch(IOException e){
			System.out.println("Couldn't close the file");
			System.exit(1);
		}
	}
	
	/*
	 * Write the string given, and move to a new line
	 */
	private void write(String command){
		try{
			program.write(command + "\n");
		}catch(IOException e){
			System.out.println("Couldn't write command " + command + " for some reason");
		}
	}
	
	/*
	 * Print out the error message given, and end the program
	 */
	private void error(String msg){
		System.out.println("Semantic ERROR:");
		System.out.println(msg);
		System.exit(1);
	}

}
