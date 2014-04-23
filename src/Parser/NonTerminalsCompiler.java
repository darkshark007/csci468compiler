package Parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

import SymbolTable.*;
import Tokenizer.Token;

public class NonTerminalsCompiler {

    static int tokindex;
    static LinkedList<Token> tokens;
    static FileOutputStream outfs;
    static File outFile;
    static Token LATok;
    static String Lookahead;
    static SymbolTable symTab;
    static String lastID;//Jon: to store last ID
    static String[] idList = new String[20];//Jon: used in identifierList() for keeping track of a list of id's before we know their type
    static int idListIndex = 0;				//if we don't know their type, we can't add them to the symbol table yet.
	
    // Variables for the parse-tree print
    static int indent = 0;
	static final boolean PRINT_PARSE_TREE = true;
	static final int LAPad = 20;
	
	// Variables for the compilation
    static boolean flag_printExtraStuff = true;
    static Stack<String> headingStack = new Stack<String>();
	
	

    public static void match(String in) {
        if (in.equals(Lookahead)) {
            
    		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" =="+String.format("%1$" + (indent*2+1) + "s", "")
    				+Lookahead+"\n");
    		
    		lastID = LATok.getLexeme();//**Jon: This is to store the last matched ID for use in adding to the symbol table afterwards
        	
        	if ( tokindex < tokens.size() ) {
        		LATok = tokens.get(tokindex++);
                Lookahead = LATok.getToken();
        	}
        	else { 
        		// **Stephen:  Modified this so it doesnt throw an out-of-bounds exception.  Also, added an else statement to insert the EOF token.  Dont know if this should be the final solution to the EOF problem, but meh.  WORKAROUND!
        		Lookahead = "MP_EOF";
        	}
            
            
        } else {
			System.out.println("Match Error>\n  Expected: "+in+"  =<>=  "+Lookahead);
            syntaxError();
        }

        return;
    }

    public static void syntaxError() { //int line, int column) {
        System.out.println("Syntax error found on line " + LATok.getLineNumber() + ", column" + LATok.getColumnNumber() + ".");
        return;
    }

    private static void addProcedureToParent(SymbolTable myTable){
    	
    	String paramList = myTable.getParameters();
    	String retType = "none";//Jon:Procedures don't have a return type
    	
    	myTable.getParent().addRow(myTable.getName(), "procedure", retType, retType, paramList); 	
    	
    }
    
    private static void addFunctionToParent(SymbolTable myTable){
    	
    	String paramList = myTable.getParameters();
    	String retType = myTable.findVariable(myTable.getName()).getType();//Jon: find the return variable, and get its type. It will be the return type
    	
    	myTable.getParent().addRow(myTable.getName(), "function", retType, retType, paramList); 	
    }
      
    
    public static void compile(LinkedList<Token> list, File fout) {
        tokens = list;
        tokindex = 1;
        LATok = tokens.getFirst();
        Lookahead = LATok.getToken();
        outFile = fout;
        try {
			outfs = new FileOutputStream(fout);
		} catch (FileNotFoundException e) {
			System.out.println("Bad file name!");
			e.printStackTrace();
			System.exit(1);
		} 

        systemGoal();
        
        // Be a good boy, close the stream!
        try { outfs.close(); } catch (IOException e) { e.printStackTrace(); }
        
    }
    private static void compileWrite( String in ) {
		try {
			outfs.write(in.getBytes());
		} catch (IOException e) {
			System.out.println("I/O Exception.  whuuuuut?");
			e.printStackTrace();
		}
    	
    }

    public static void systemGoal() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"SystemGoal");
        switch (Lookahead) {
            case "MP_PROGRAM":
            	System.out.println(" (#1)"); // Rule #1
                program();
                match("MP_EOF");
                compileWrite("HLT\r\n");
                symTab.printTableFromTop();
                break;

            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void program() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Program");
        switch (Lookahead) {
            case "MP_PROGRAM":
            	System.out.println(" (#2)"); // Rule #2
                programHeading();
                match("MP_SCOLON");
                block();
                match("MP_PERIOD");
                break;
            default:
                syntaxError();// syntaxError
                break;
        }
        indent--;
    }

    public static void programHeading() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProgramHeading");
        switch (Lookahead) {
            case "MP_PROGRAM":
            	System.out.println(" (#3)"); // Rule #3
                match("MP_PROGRAM");
                programIdentifier();
                
                // COMPILER CODE
                	compileWrite("BR L_Prog_"+lastID+"\r\n");
            		headingStack.push("L_Prog_"+lastID);
            	// COMPILER END CODE
                
                symTab = new SymbolTable(lastID);//creates initial symbol table for the program
                break;
            default:
                syntaxError();// syntaxError
                break;
        }
        indent--;
    }

    public static void block() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Block");
        switch (Lookahead) {
            case "MP_BEGIN":
            case "MP_FUNCTION":
            case "MP_PROCEDURE":
            case "MP_VAR":
            	System.out.println(" (#4)"); // Rule #4
                variableDeclarationPart();
                procedureAndFunctionDeclarationPart();
                
                // COMPILER CODE
                	// Print a header label for this Block
                	if ( flag_printExtraStuff ) compileWrite("\r\n");
                	compileWrite(headingStack.pop()+":\r\n");
                
	            	// Check for function/procedure arguments
                	int numArgs = symTab.getParameters().split(" ").length;
                	if ( numArgs > 0 ) {
	            		int offset = numArgs-symTab.getSize();
	            		while ( numArgs > 0 ) {
	            			compileWrite("POP "+(offset)+"(SP)\r\n");
	            			numArgs--;
	            		}
	            	}
	            
	            	// Build the Activation record
	            	int symTsize = (symTab.getSize()+1); 
	            	compileWrite("PUSH D"+symTab.getNestingLevel()+"\r\n");
	            	compileWrite("MOV SP D"+symTab.getNestingLevel()+"\r\n");
	            	compileWrite("ADD SP #"+symTsize+"\r\n");             
	            // COMPILER END CODE

                statementPart();
                
                // COMPILER CODE
                	// Restore the previous Activation record
                	compileWrite("SUB SP #"+symTsize+"\r\n");
                	// If this block completes a function, then its return value must be pushed onto the top of the stack.
                	if ( symTab.getParent() != null )
                		if ( symTab.getParent().findVariable(symTab.getName()).getKind().equals("function") ) {
                			compileWrite("PUSH "+symTab.findVariable(symTab.getName()).getOffset()+"(D"+symTab.getNestingLevel()+")\r\n");             			
                		}
                	compileWrite("MOV D"+symTab.getNestingLevel()+" "+symTsize+"(D"+symTab.getNestingLevel()+")\r\n");
                	
                	
                	// If this block is not the main program, it must return to the calling function
                	if ( headingStack.size() >= 1 ) compileWrite("RET\r\n");
                	
                	
                	if ( flag_printExtraStuff ) compileWrite("{End block}\r\n\r\n");
                // COMPILER END CODE
                	
                break;
            default:
                syntaxError();// syntaxError
                break;
        }
        indent--;
    }

    public static void variableDeclarationPart() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"VariableDeclarationPart");
        switch (Lookahead) {
            case "MP_BEGIN":
            case "MP_FUNCTION":
            case "MP_PROCEDURE":
            	System.out.println(" (#6)"); // Rule #6
                break;
            case "MP_VAR":
            	System.out.println(" (#5)"); // Rule #5
                match("MP_VAR");
                variableDeclaration();
                match("MP_SCOLON");
                variableDeclarationTail();
                break;
            default: // End thingy?
                syntaxError();
                break;
        }
        indent--;
    }

    public static void variableDeclarationTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"VariableDeclarationTail");
        switch (Lookahead) {
            case "MP_BEGIN":
            case "MP_FUNCTION":
            case "MP_PROCEDURE":
            	System.out.println(" (#8)"); // Rule #8
                break;
            case "MP_IDENTIFIER":
            	System.out.println(" (#7)"); // Rule #7
                variableDeclaration();
                match("MP_SCOLON");
                variableDeclarationTail();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void variableDeclaration() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"VariableDelcaration");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#9)"); // Rule #9
                identifierList();
                match("MP_COLON");
                type();
                for(int i = 0; i < idListIndex; i++){//Jon: go through our list of identifiers and add them to the symbol table now that we know their type
                	symTab.addRow(idList[i], "var", lastID, "none", "none");//Jon: lastID should be set to the last type matched in type()
                	idList[i] = "";//Jon: empty the spot after we've added it to the table
                }
                idListIndex = 0;//Jon: reset the index now that we're done with the array
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void type() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Type");
        switch (Lookahead) {
            case "MP_INTEGER":
            	System.out.println(" (#10)"); // Rule #10
                match("MP_INTEGER");
                break;
            case "MP_FLOAT":
            	System.out.println(" (#11)"); // Rule #11
                match("MP_FLOAT");
                break;
            case "MP_STRING":
            	System.out.println(" (#12)"); // Rule #12
                match("MP_STRING");
                break;
            case "MP_BOOLEAN":
            	System.out.println(" (#13)"); // Rule #13
                match("MP_BOOLEAN");
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void procedureAndFunctionDeclarationPart() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProcedureAndFunctionDeclarationPart");
        switch (Lookahead) {
            case "MP_PROCEDURE":
            	
            	System.out.println(" (#14)"); // Rule #14
                procedureDeclaration();
                procedureAndFunctionDeclarationPart();
                break;
            case "MP_FUNCTION":
            	System.out.println(" (#15)"); // Rule #15
                functionDeclaration();
                procedureAndFunctionDeclarationPart();
                break;
            case "MP_BEGIN":
            	System.out.println(" (#16)"); // Rule #16
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void procedureDeclaration() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProcedureDeclaration");
        switch (Lookahead) {
            case "MP_PROCEDURE":
            	System.out.println(" (#17)"); // Rule #17
                procedureHeading();
                addProcedureToParent(symTab);//Jon: adds this procedure as a row to the parent class
                match("MP_SCOLON");
                block();
                match("MP_SCOLON");
                symTab = symTab.getParent();//Jon: move the table back to it's parent because we're done with it at this point
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void functionDeclaration() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FunctionDeclaration");
        switch (Lookahead) {
            case "MP_FUNCTION":
            	System.out.println(" (#18)"); // Rule #18
                functionHeading();
                addFunctionToParent(symTab);
                match("MP_SCOLON");
                block();
                match("MP_SCOLON");
                symTab = symTab.getParent();//Jon: move the table back to it's parent because we're done with it at this point
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void procedureHeading() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProcedureHeading");
        switch (Lookahead) {
            case "MP_PROCEDURE":
            	System.out.println(" (#19)"); // Rule #19
                match("MP_PROCEDURE");
                procedureIdentifier();
                
                // COMPILER CODE
                	headingStack.push("L_Procedure_"+lastID);
            	// COMPILER END CODE
            
   
                symTab = symTab.makeNewTable(lastID);//Jon: make a new table, and lastID should be set to the name from matching in procedureIdentifier()
                optionalFormalParameterList();

                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void functionHeading() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FunctionHeading");
        switch (Lookahead) {
            case "MP_FUNCTION":
            	System.out.println(" (#20)"); // Rule #20
                match("MP_FUNCTION");
                functionIdentifier();
                
                // COMPILER CODE
            		headingStack.push("L_Function_"+lastID);
           		// COMPILER END CODE
            
                
                String funcID = lastID;//Jon: used at the end to add a variable value for returning at the end of the function call
                symTab = symTab.makeNewTable(lastID);//Jon: make a new table, and lastID should be set to the name from matching in procedureIdentifier()
                optionalFormalParameterList();
                
                match("MP_COLON"); 
                type();
                symTab.addRow(funcID, "var", lastID, "none", "none");//Jon: adds a last variable to store the return value of the function
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void optionalFormalParameterList() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalFormalParameterList");
        switch (Lookahead) {
            case "MP_LPAREN":
            	System.out.println(" (#21)"); // Rule #21
                match("MP_LPAREN");
                formalParameterSection();
                formalParameterSectionTail();
                match("MP_RPAREN");
                break;
            case "MP_COLON":
            case "MP_SCOLON":
            	System.out.println(" (#22)"); // Rule #22
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void formalParameterSectionTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FormalParameterList");
        switch (Lookahead) {
            case "MP_SCOLON":
            	System.out.println(" (#23)"); // Rule #23
                match("MP_SCOLON");
                formalParameterSection();
                formalParameterSectionTail();
                break;
            case "MP_RPAREN":
            	System.out.println(" (#24)"); // Rule #24
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void formalParameterSection() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FormalParameterSection");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#25)"); // Rule #25
                valueParameterSection();
                break;
            case "MP_VAR":
            	System.out.println(" (#26)"); // Rule #26
                variableParameterSection();
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void valueParameterSection() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ValueParameterSection");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#27)"); // Rule #27
                identifierList();
                match("MP_COLON");
                type();
                for(int i = 0; i < idListIndex; i++){//Jon: go through our list of identifiers and add them to the symbol table now that we know their type
                	symTab.addRow(idList[i], "param", lastID, "none", "none");//Jon: lastID should be set to the last type matched in type()
                	idList[i] = "";//Jon: empty the spot after we've added it to the table
                }
                idListIndex = 0;//Jon: reset the index now that we're done with the array
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void variableParameterSection() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"VariableParameterSection");
        switch (Lookahead) {
            case "MP_VAR":
            	System.out.println(" (#28)"); // Rule #28
                match("MP_VAR");
                identifierList();
                match("MP_COLON");
                type();
                for(int i = 0; i < idListIndex; i++){//Jon: go through our list of identifiers and add them to the symbol table now that we know their type
                	symTab.addRow(idList[i], "param", lastID, "none", "none");//Jon: lastID should be set to the last type matched in type()
                	idList[i] = "";//Jon: empty the spot after we've added it to the table
                }
                idListIndex = 0;//Jon: reset the index now that we're done with the array
                break;

            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void statementPart() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"StatementPart");
        switch (Lookahead) {
            case "MP_BEGIN":
            	System.out.println(" (#29)"); // Rule #29
                compoundStatement();
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void compoundStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"CompoundStatement");
        switch (Lookahead) {
            case "MP_BEGIN":
            	System.out.println(" (#30)"); // Rule #30
                match("MP_BEGIN");
                statementSequence();
                match("MP_END");
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void statementSequence() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"StatementSequence");
        switch (Lookahead) {
            case "MP_BEGIN":
            case "MP_ELSE":
            case "MP_END":
            case "MP_FOR":
            case "MP_IF":
            case "MP_READ":
            case "MP_REPEAT":
            case "MP_UNTIL":
            case "MP_WHILE":
            case "MP_WRITE":
            case "MP_WRITELN":
            case "MP_IDENTIFIER":
            case "MP_SCOLON":
            	System.out.println(" (#31)"); // Rule #31
                statement();
                statementTail();
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void statementTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"StatementTail");
        switch (Lookahead) {
            case "MP_SCOLON":
            	System.out.println(" (#32)"); // Rule #32
                match("MP_SCOLON");
                statement();
                statementTail();
                break;
            case "MP_END":
            case "MP_UNTIL":
            	System.out.println(" (#33)"); // Rule #33
                break;
            default: // syntaxError
                syntaxError();
                break;
        }
        indent--;
    }

    public static void statement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Statement");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	// **Stephen: Not a complete workaround.  We're going to have to find a better way to solve this, or at least do alot more thorough job.   The rule for ProcedureStatement(#67) states that a procedure call can have parameters OPTIONALLY, thus checking for a Lparen wouldnt always be sufficient to cover this case.
            	System.out.println();
                match("MP_IDENTIFIER");//workaround, just match id and move on
                switch (Lookahead) {
                    case "MP_ASSIGN":
                    	//System.out.println(" (#38)"); // Rule #38
                        assignmentStatement();
                        break;

                    default:
                    	//System.out.println(" (#43)"); // Rule #43
                        procedureStatement();
                        break;
                }
                break;

            case "MP_BEGIN":
            	System.out.println(" (#35)"); // Rule #35
                compoundStatement();
                break;

            case "MP_READ":
            	System.out.println(" (#36)"); // Rule #36
                readStatement();
                break;

            case "MP_WRITE":
            case "MP_WRITELN":
            	System.out.println(" (#37)"); // Rule #37
                writeStatement();
                break;

            case "MP_IF":
            	System.out.println(" (#39)"); // Rule #39
                ifStatement();
                break;

            case "MP_WHILE":
            	System.out.println(" (#40)"); // Rule #40
                whileStatement();
                break;

            case "MP_REPEAT":
            	System.out.println(" (#41)"); // Rule #41
                repeatStatement();
                break;

            case "MP_FOR":
            	System.out.println(" (#42)"); // Rule #42
                forStatement();
                break;
                
            case "MP_ELSE":
            case "MP_END":
            case "MP_UNTIL":
            case "MP_SCOLON":
            	System.out.println(" (#34)"); // Rule #34
                emptyStatement();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void emptyStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"EmptyStatement");
        switch (Lookahead) {
            case "MP_ELSE":
            case "MP_END":
            case "MP_UNTIL":
            case "MP_SCOLON":
            	System.out.println(" (#44)"); // Rule #44
                break;
            default:
                syntaxError();// syntaxError
                break;
        }
        indent--;
    }

    public static void readStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ReadStatement");
        switch (Lookahead) {
            case "MP_READ":
            	System.out.println(" (#45)"); // Rule #45
                match("MP_READ");
                match("MP_LPAREN");
                readParameter();
                readParameterTail();
                match("MP_RPAREN");
                break;
            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void readParameterTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ReadParameterTail");
        switch (Lookahead) {
            case "MP_COMMA":
            	System.out.println(" (#46)"); // Rule #46
                match("MP_COMMA");
                readParameter();
                readParameterTail();
                break;
            case "MP_RPAREN":
            	System.out.println(" (#47)"); // Rule #47
                break;
            default: // syntaxError OR Empty-String
                break;
        }
        indent--;
    }

    public static void readParameter() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ReadParameter");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#48)"); // Rule #48
                variableIdentifier();
                
                // COMPILER CODE
            		symTab.printTableFromTop();
            		Row rp = symTab.findVariable(lastID);
            		switch (rp.getType()) {
            			case "integer":
            				compileWrite("RD ");
            				break;
            			case "float":
            				compileWrite("RDF ");
            				break;
            			case "string":
            				compileWrite("RDS ");
            				break;
            		}
            		compileWrite(rp.getOffset()+"(D"+symTab.getNestingLevel()+")\r\n");
            	// COMPILER END CODE

                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void writeStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"WriteStatement");
        switch (Lookahead) {
            case "MP_WRITE":
            	System.out.println(" (#49)"); // Rule #49
                match("MP_WRITE");
                match("MP_LPAREN");
                
                // COMPILER CODE
            		compileWrite("WRT ");
            		// TODO -- Finish the WriteStatement, left out the expression portion.
            			compileWrite("\r\n");
            	// COMPILER END CODE
            
                writeParameter();
                writeParameterTail();
                match("MP_RPAREN");
                break;
            case "MP_WRITELN":
            	System.out.println(" (#50)"); // Rule #50
                match("MP_WRITELN");
                match("MP_LPAREN");
                
                // COMPILER CODE
            		compileWrite("WRTLN ");
            	// COMPILER END CODE
            

                
                writeParameter();
                writeParameterTail();
                match("MP_RPAREN");
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void writeParameterTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"WriteParameterTail");
        switch (Lookahead) {
            case "MP_COMMA":
            	System.out.println(" (#51)"); // Rule #51
                match("MP_COMMA");
                writeParameter();
                writeParameterTail();
                break;
            case "MP_RPAREN":
            	System.out.println(" (#52)"); // Rule #52
                break;
            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void writeParameter() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"WriteParameter");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_INTEGER_LIT":
            case "MP_TRUE":
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_LPAREN":
            case "MP_IDENTIFIER":
            	System.out.println(" (#53)"); // Rule #53
                ordinalExpression();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void assignmentStatement() {
    	if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"AssignmentStatement");
    	// **Stephen** The parser breaks here, the logical consistency is broken in the Statement method, where it uses an "extra" look-ahead.  
        /*
    	switch (Lookahead) {
        case "MP_IDENTIFIER"://both rules are the same
            match("MP_IDENTIFIER");
            match("MP_ASSIGN");
            expression();
            break;
        default: // syntaxError OR Empty-String
            syntaxError();
            break;
        }
        */
        
        
        switch (Lookahead) {
        case "MP_ASSIGN"://both rules are the same
        	System.out.println(" (#54,55)"); // Rule #54,55
        	
        	// COMPILER CODE
        		Row ap = symTab.findVariable(lastID);
        	// COMPILER END CODE
        		
            match("MP_ASSIGN");
            expression();
            
            // COMPILER CODE
            	compileWrite("POP "+ap.getOffset()+"(D"+symTab.getNestingLevel()+")\r\n");
        	// COMPILER CODE

            break;
        default: // syntaxError OR Empty-String
            syntaxError();
            break;
    }


        indent--;
    }

    public static void ifStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"IfStatement");
        switch (Lookahead) {
            case "MP_IF":
            	System.out.println(" (#56)"); // Rule #56
                match("MP_IF");
                booleanExpression();
                match("MP_THEN");
                statement();
                optionalElsePart();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void optionalElsePart() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalElsePart");
        switch (Lookahead) {
            case "MP_ELSE":
            	System.out.println(" (#57)"); // Rule #57
                match("MP_ELSE");
                statement();
                break;
            case "MP_END":
            case "MP_UNTIL":
            case "MP_SCOLON":
            	System.out.println(" (#58)"); // Rule #58
                break;

            default: // syntaxError OR Empty-String
                syntaxError(); // **Stephen: This used to be emptyStatement(), is that right?
                break;
        }
        indent--;
    }

    public static void repeatStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"RepeatStatement");
        switch (Lookahead) {
            case "MP_REPEAT":
            	System.out.println(" (#59)"); // Rule #59
                match("MP_REPEAT");
                statementSequence();
                match("MP_UNTIL");
                booleanExpression();
                break;
            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void whileStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"WhileStatement");
        switch (Lookahead) {
            case "MP_WHILE":
            	System.out.println(" (#60)"); // Rule #60
                match("MP_WHILE");
                booleanExpression();
                match("MP_DO");
                statement();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void forStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ForStatement");
        switch (Lookahead) {
            case "MP_FOR":
            	System.out.println(" (#61)"); // Rule #61
                match("MP_FOR");
                controlVariable();
                match("MP_ASSIGN");
                initialValue();
                stepValue();
                finalValue();
                match("MP_DO");
                statement();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void controlVariable() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ControlVariable");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#62)"); // Rule #62
                variableIdentifier();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void initialValue() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"InitialValue");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_TRUE":
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_LPAREN":
            case "MP_IDENTIFIER":
            	System.out.println(" (#63)"); // Rule #63
                ordinalExpression();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void stepValue() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"StepValue");
        switch (Lookahead) {
            case "MP_TO":
            	System.out.println(" (#64)"); // Rule #64
                match("MP_TO");
                break;
            case "MP_DOWNTO":
            	System.out.println(" (#65)"); // Rule #65
                match("MP_DOWNTO");
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void finalValue() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FinalValue");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_TRUE":
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_LPAREN":
            case "MP_IDENTIFIER":
            	System.out.println(" (#66)"); // Rule #66
                ordinalExpression();
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void procedureStatement() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProcedureStatment");

    	System.out.println(" (#67)"); // Rule #67
		optionalActualParameterList();
		/*
		switch (Lookahead) {
            //case "MP_IDENTIFIER":
            //    procedureIdentifier();
            //    optionalActualParameterList();
            //    break;
        
        	case "MP_LPAREN":
            	System.out.println(" (#67)"); // Rule #67
        		optionalActualParameterList();
        		break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        */
        indent--;
    }

    public static void optionalActualParameterList() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalActualParameterList");
        switch (Lookahead) {
            case "MP_LPAREN":
            	System.out.println(" (#68)"); // Rule #68
                match("MP_LPAREN");
                actualParameter();
                actualParameterTail();
                match("MP_RPAREN");
                break;
            case "MP_AND":
            case "MP_DIV":
            case "MP_DO":
            case "MP_DOWNTO":
            case "MP_ELSE":
            case "MP_END":
            case "MP_MOD":
            case "MP_OR":
            case "MP_THEN":
            case "MP_TO":
            case "MP_UNTIL":
            case "MP_COMMA":
            case "MP_EQUAL":
            case "MP_FLOAT_DIVIDE":
            case "MP_GEQUAL":
            case "MP_GTHAN":
            case "MP_LEQUAL":
            case "MP_LTHAN":
            case "MP_MINUS":
            case "MP_NEQUAL":
            case "MP_PLUS":
            case "MP_RPAREN":
            case "MP_SCOLON":
            case "MP_TIMES":
            	System.out.println(" (#69)"); // Rule #69
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void actualParameterTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ActualParameterTail");
        switch (Lookahead) {
            case "MP_COMMA":
            	System.out.println(" (#70)"); // Rule #70
                match("MP_COMMA");
                actualParameter();
                actualParameterTail();
                break;
            case "MP_RPAREN":
            	System.out.println(" (#71)"); // Rule #71
                break;
            default:
                syntaxError(); // **Stephen: This used to be emptyStatement(), is that right?
                break;
        }
        indent--;
   }

    public static void actualParameter() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ActualParameter");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_TRUE":
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_LPAREN":
            case "MP_IDENTIFIER":
            	System.out.println(" (#72)"); // Rule #72
                ordinalExpression();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void expression() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Expression");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_TRUE":
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_LPAREN":
            case "MP_IDENTIFIER":
            	System.out.println(" (#73)"); // Rule #73
                simpleExpression();
                optionalRelationalPart();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void optionalRelationalPart() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalRelationalPart");
        switch (Lookahead) {
            case "MP_EQUAL":
            case "MP_GEQUAL":
            case "MP_GTHAN":
            case "MP_LEQUAL":
            case "MP_LTHAN":
            case "MP_NEQUAL":
            	System.out.println(" (#74)"); // Rule #74
                relationalOperator();
                simpleExpression();
                break;
            case "MP_DO":
            case "MP_DOWNTO":
            case "MP_ELSE":
            case "MP_END":
            case "MP_THEN":
            case "MP_TO":
            case "MP_UNTIL":
            case "MP_COMMA":
            case "MP_RPAREN":
            case "MP_SCOLON":
            	System.out.println(" (#75)"); // Rule #75
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void relationalOperator() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"RelationalOperator");
        switch (Lookahead) {
            case "MP_EQUAL":
            	System.out.println(" (#76)"); // Rule #76
                match("MP_EQUAL");
                break;
            case "MP_LTHAN":
            	System.out.println(" (#77)"); // Rule #77
                match("MP_LTHAN");
                break;
            case "MP_GTHAN":
            	System.out.println(" (#78)"); // Rule #78
                match("MP_GTHAN");
                break;
            case "MP_GEQUAL":
            	System.out.println(" (#80)"); // Rule #80
                match("MP_GEQUAL");
                break;
            case "MP_LEQUAL":
            	System.out.println(" (#79)"); // Rule #79
                match("MP_LEQUAL");
                break;
            case "MP_NEQUAL":
            	System.out.println(" (#81)"); // Rule #81
                match("MP_NEQUAL");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void simpleExpression() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"SimpleExpression");
        switch (Lookahead) {
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_TRUE":
            case "MP_IDENTIFIER":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_LPAREN":
            case "MP_MINUS":
            case "MP_PLUS":
            	System.out.println(" (#82)"); // Rule #82
                optionalSign();
                term();
                termTail();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void termTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"TermTail");
        switch (Lookahead) {
            case "MP_PLUS":
            case "MP_MINUS":
            case "MP_OR":
            	System.out.println(" (#83)"); // Rule #83
                addingOperator();
                term();
                termTail();
                break;
            case "MP_EQUAL":
            case "MP_GEQUAL":
            case "MP_GTHAN":
            case "MP_LEQUAL":
            case "MP_LTHAN":
            case "MP_NEQUAL":
            case "MP_DO":
            case "MP_DOWNTO":
            case "MP_ELSE":
            case "MP_END":
            case "MP_THEN":
            case "MP_TO":
            case "MP_UNTIL":
            case "MP_COMMA":
            case "MP_RPAREN":
            case "MP_SCOLON":
            	System.out.println(" (#84)"); // Rule #84
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void optionalSign() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalSign");
        switch (Lookahead) {
            case "MP_PLUS":
            	System.out.println(" (#85)"); // Rule #85
                match("MP_PLUS");
                break;
            case "MP_MINUS":
            	System.out.println(" (#86)"); // Rule #86
                match("MP_MINUS");
                break;
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_TRUE":
            case "MP_IDENTIFIER":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_LPAREN":
            	System.out.println(" (#87)"); // Rule #87
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
  }

    public static void addingOperator() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"AddingOperator");
        switch (Lookahead) {
            case "MP_PLUS":
            	System.out.println(" (#88)"); // Rule #88
                match("MP_PLUS");
                break;
            case "MP_MINUS":
            	System.out.println(" (#89)"); // Rule #89
                match("MP_MINUS");
                break;
            case "MP_OR":
            	System.out.println(" (#90)"); // Rule #90
                match("MP_OR");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void term() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Term");
        switch (Lookahead) {
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_TRUE":
            case "MP_IDENTIFIER":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_LPAREN":
            	System.out.println(" (#91)"); // Rule #91
                factor();
                factorTail();
                break;
            default:
                syntaxError();
        }
        indent--;
    }

    public static void factorTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FactorTail");
        switch (Lookahead) {
            case "MP_DO":
            case "MP_DOWNTO":
            case "MP_ELSE":
            case "MP_END":
            case "MP_OR":
            case "MP_THEN":
            case "MP_TO":
            case "MP_UNTIL":
            case "MP_COMMA":
            case "MP_EQUAL":
            case "MP_GEQUAL":
            case "MP_GTHAN":
            case "MP_LEQUAL":
            case "MP_LTHAN":
            case "MP_MINUS":
            case "MP_NEQUAL":
            case "MP_PLUS":
            case "MP_RPAREN":
            case "MP_SCOLON":
            	System.out.println(" (#93)"); // Rule #93
                break;
                
            case "MP_TIMES":
            case "MP_FLOAT_DIVIDE":
            case "MP_AND":
            case "MP_DIV":
            case "MP_MOD":
            	System.out.println(" (#92)"); // Rule #92
                multiplyingOperator();
                factor();
                factorTail();
                break;
            default:
                syntaxError(); // **Stephen: This used to be emptyStatement(), is that right?
                break;
        }
        indent--;
    }

    public static void multiplyingOperator() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"MultiplyingOperator");
        switch (Lookahead) {
            case "MP_TIMES":
            	System.out.println(" (#94)"); // Rule #94
                match("MP_TIMES");
                break;
            case "MP_FLOAT_DIVIDE":
            	System.out.println(" (#95)"); // Rule #95
                match("MP_FLOAT_DIVIDE");
                break;
            case "MP_DIV":
            	System.out.println(" (#96)"); // Rule #96
                match("MP_DIV");
                break;
            case "MP_MOD":
            	System.out.println(" (#97)"); // Rule #97
                match("MP_MOD");
                break;
            case "MP_AND":
            	System.out.println(" (#98)"); // Rule #98
                match("MP_AND");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void factor() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"Factor");
        switch (Lookahead) {
            case "MP_INTEGER_LIT":
            	System.out.println(" (#99)"); // Rule #99
                match("MP_INTEGER_LIT");
                break;
            case "MP_FLOAT_LIT":  
            	System.out.println(" (#100)"); // Rule #100
                match("MP_FLOAT_LIT");
                break;
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            	System.out.println(" (#100)"); // Rule #100
                match("MP_FIXED_LIT");
                break;
            case "MP_STRING_LIT":
            	System.out.println(" (#101)"); // Rule #101
                match("MP_STRING_LIT");
                break;
            case "MP_TRUE":
            	System.out.println(" (#102)"); // Rule #102
                match("MP_TRUE");
                break;
            case "MP_FALSE":
            	System.out.println(" (#103)"); // Rule #103
                match("MP_FALSE");
                break;
            case "MP_NOT":
            	System.out.println(" (#104)"); // Rule #104
                match("MP_NOT");
                factor();
                break;
            case "MP_LPAREN":
            	System.out.println(" (#105)"); // Rule #105
                match("MP_LPAREN");
                expression();
                match("MP_RPAREN");
                break;
            case "MP_IDENTIFIER":
            	System.out.println(" (#106)"); // Rule #106
                functionIdentifier();
                optionalActualParameterList();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void programIdentifier() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProgramIdentifier");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#107)"); // Rule #107
                match("MP_IDENTIFIER");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void variableIdentifier() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"VariableIdentifier");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#108)"); // Rule #108
                match("MP_IDENTIFIER");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void procedureIdentifier() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"ProcedureIdentifier");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#109)"); // Rule #109
                match("MP_IDENTIFIER");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void functionIdentifier() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"FunctionIdentifier");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#110)"); // Rule #110
                match("MP_IDENTIFIER");
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void booleanExpression() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"BooleanExpression");
        switch (Lookahead) {
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_TRUE":
            case "MP_IDENTIFIER":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_LPAREN":
            case "MP_MINUS":
            case "MP_PLUS":
            	System.out.println(" (#111)"); // Rule #111
                expression();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void ordinalExpression() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OrdinalExpression");
        switch (Lookahead) {
            case "MP_FALSE":
            case "MP_NOT":
            case "MP_TRUE":
            case "MP_IDENTIFIER":
            case "MP_INTEGER_LIT":
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            case "MP_FLOAT_LIT":
            case "MP_STRING_LIT":
            case "MP_LPAREN":
            case "MP_MINUS":
            case "MP_PLUS":
            	System.out.println(" (#112)"); // Rule #112
                expression();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void identifierList() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"IdentifierList");
        switch (Lookahead) {
            case "MP_IDENTIFIER":
            	System.out.println(" (#113)"); // Rule #113
                match("MP_IDENTIFIER");
                idList[idListIndex++] = lastID;//Jon: add the id to our running list and increment the index
                identifierTail();
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }

    public static void identifierTail() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"IdentifierTail");
        switch (Lookahead) {
            case "MP_COMMA":
            	System.out.println(" (#114)"); // Rule #114
                match("MP_COMMA");
                match("MP_IDENTIFIER");
                idList[idListIndex++] = lastID;//Jon: add the id to our running list and increment the index
                identifierTail();
                break;
            case "MP_COLON":
            	System.out.println(" (#115)"); // Rule #115
                break;
            default:
                syntaxError();
                break;
        }
        indent--;
    }
}
