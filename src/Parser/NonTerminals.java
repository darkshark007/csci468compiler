package Parser;

import java.util.ArrayList;
import java.util.LinkedList;

import SymbolTable.*;
import SemanticAnalyzer.*;
import Tokenizer.Token;

public class NonTerminals {

    static int tokindex;
    static LinkedList<Token> tokens;
    static Token LATok;
    static String Lookahead;
    
    //Variables for symbol table making
    static SymbolTable symTab;
    static String lastID;//Jon: to store last ID
    static String[] idList = new String[20];//Jon: used in identifierList() for keeping track of a list of id's before we know their type
    static int idListIndex = 0;				//if we don't know their type, we can't add them to the symbol table yet.
	
    //Variables for the semantic analyzer
    static SemanticAnalyzer semAn = new SemanticAnalyzer();
    static Token lastTok;
    static int wrtRdNum = 0;
    static ArrayList<Token> readParams = new ArrayList<Token>();
    static String paramTypeList = "";
    static int controlCounter = 0;
    static Token stepVal;
    
    // Variables for the parse-tree print
    static int indent = 0;
	static final boolean PRINT_PARSE_TREE = true;
	static final int LAPad = 20;
	

    public static void match(String in) {
		// TODO STUB!!!!!!

        if (in.equals(Lookahead)) {
            
    		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" =="+String.format("%1$" + (indent*2+1) + "s", "")
    				+Lookahead+"\n");
    		
    		lastID = LATok.getLexeme();//**Jon: This is to store the last matched ID for use in adding to the symbol table afterwards
    		lastTok = LATok;//This is for the semantic analyzer
        	
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
        // TODO STUB!!!!!!
        System.out.println("Syntax error found on line " + lastTok.getLineNumber() + ", column" + lastTok.getColumnNumber() + ".");
        System.exit(1);
        return;
    }
    /*
     * These two procedure will add a "call" entry to their parent symbol tables
     */
    private static void addProcedureToParent(SymbolTable myTable){
    	
    	String paramList = myTable.getParameters();
    	String retType = "";//Jon:Procedures don't have a return type
    	
    	myTable.getParent().addFunctionOrParameterRow(myTable.getName(), "procedure", retType, retType, paramList, myTable.getSize()); 	
    	
    }
    
    private static void addFunctionToParent(SymbolTable myTable){
    	
    	String paramList = myTable.getParameters();
    	String retType = myTable.findVariable(myTable.getName()).getType();//Jon: find the return variable, and get its type. It will be the return type
    	
    	myTable.getParent().addFunctionOrParameterRow(myTable.getName(), "function", retType, retType, paramList, myTable.getSize()); 	
    }
    
    public static void start(LinkedList<Token> list) {
        tokens = list;
        tokindex = 1;
        LATok = tokens.getFirst();
        Lookahead = LATok.getToken();
        systemGoal();
    }

    public static void systemGoal() {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"SystemGoal");
        switch (Lookahead) {
            case "MP_PROGRAM":
            	System.out.println(" (#1)"); // Rule #1
                program();
                match("MP_EOF");
                symTab.printTableFromTop();
                semAn.end();
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
                symTab = new SymbolTable(lastID);//creates initial symbol table for the program
                semAn.addLabel(symTab);//adds a label for the "main" program, and since it's the first, the label will b L1
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
                semAn.statementStart(symTab);//every time a block of statements starts, we need to "build" room in the stack based on the current symbol table
                statementPart();
                semAn.statementEnd(symTab);//after the block is done, we need to "deconstruct" the stack
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
                semAn.addLabel(symTab);//create a label for this procedure to use in calling it later
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
                semAn.addLabel(symTab);//add a label for this function so that we can call it later
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
                String funcID = lastID;//Jon: used at the end to add a variable value for returning at the end of the function call
                symTab = symTab.makeNewTable(lastID);//Jon: make a new table, and lastID should be set to the name from matching in procedureIdentifier()
                optionalFormalParameterList();
                match("MP_COLON"); 
                type();
                symTab.addRow(funcID, "retVar", lastID, "none", "none");//Jon: adds a last variable to store the return value of the function
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
                readParams = new ArrayList<Token>();//this is to guarantee a "fresh" list, and is used to gather all the variables that we'll read into
                readParameter();
                readParams.add(lastTok);
                readParameterTail();
                match("MP_RPAREN");
                semAn.read(readParams, symTab);//create code to read into all of the parameters given
                readParams.clear();
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
                readParams.add(lastTok);
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
                writeParameter();
                semAn.write();//since write parameter goes to expression, what needs to be writting will be on top of the stack naturally, we just write from it
                writeParameterTail();
                match("MP_RPAREN");
                break;
            case "MP_WRITELN":
            	System.out.println(" (#50)"); // Rule #50
                match("MP_WRITELN");
                match("MP_LPAREN");
                writeParameter();
                semAn.write();
                writeParameterTail();
                match("MP_RPAREN");
                semAn.writeln();//this operates the same as write, but we put an empty writeln at the end to jump to a new line
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
                semAn.write();
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
        
        switch (Lookahead) {
        case "MP_ASSIGN"://both rules are the same
        	System.out.println(" (#54,55)"); // Rule #54,55
        	Token popInto = lastTok; // this is the variable we will be assigning a value to
            match("MP_ASSIGN");
            expression();
            semAn.pop(popInto, symTab);//pop the end result of the expression into our variable
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
            	String name = "if" + controlCounter;//create unique name
            	semAn.addLabel("if", controlCounter);//add a label for it's type and unique name
            	controlCounter++;
                match("MP_IF");
                booleanExpression();
                semAn.branch(name, "false");//branch to the end if false
                match("MP_THEN");
                statement();
                optionalElsePart(name);//pass the name variable to the else statement
                break;

            default: // syntaxError OR Empty-String
                syntaxError();
                break;
        }
        indent--;
    }

    public static void optionalElsePart(String prevIf) {
		if ( PRINT_PARSE_TREE ) System.out.printf(""+String.format("%1$" + LAPad + "s", Lookahead)+" ->"+String.format("%1$" + (indent++*2+1) + "s", "")
				+"OptionalElsePart");
        switch (Lookahead) {
            case "MP_ELSE":
            	System.out.println(" (#57)"); // Rule #57
            	String name = "else" + controlCounter;//create a unique name as well
            	semAn.addLabel("else", controlCounter);//add a label for it
            	controlCounter++;
            	semAn.branch(name, "always");//always branch past the else if the previous if came true
            	semAn.placeLabel(prevIf);//this is where the previous if branches to on failure
                match("MP_ELSE");
                statement();
                semAn.placeLabel(name);//place the exit of the else statement. If the if statement came true, it will always branch to here
                break;
            case "MP_END":
            case "MP_UNTIL":
            case "MP_SCOLON":
            	System.out.println(" (#58)"); // Rule #58
            	semAn.placeLabel(prevIf);//there is no else statement, so the if statement branches to here
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
                String name = "repeat" + controlCounter;//create a unique name
                semAn.addLabel("repeat", controlCounter);//add label for it
                controlCounter++;
                semAn.placeLabel(name);//place that label here
                statementSequence();
                match("MP_UNTIL");
                booleanExpression();
                semAn.branch(name, "false");//branch back to the beginning on failure
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
                
                String whileEnter = "whileEnter" + controlCounter;//create unique beginning name
                semAn.addLabel("whileEnter", controlCounter);//add label for it
                controlCounter++;
                
                String whileExit = "whileExit" + controlCounter;//create a unique exit name
                semAn.addLabel("whileExit", controlCounter);//add label for it
                controlCounter++;
                
                semAn.placeLabel(whileEnter);//place the beginning label here
                booleanExpression();
                semAn.branch(whileExit, "false");//branch to exit if condition fails
                
                match("MP_DO");
                statement();
                semAn.branch(whileEnter, "always");//always branch back to top to check boolean again
                semAn.placeLabel(whileExit);//branch exits and continues on with code
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
                Token controlVar = lastTok;//store the control variable
                
                String forLoopEnter = "forEnter" + controlCounter;//create unique beginning name
                semAn.addLabel("forEnter", controlCounter);//add label for it
                controlCounter++;
                
                String forLoopExit = "forExit" + controlCounter;//create unique exit name
                semAn.addLabel("forExit", controlCounter);//add label for it
                controlCounter++;
                
                match("MP_ASSIGN");
                initialValue();
                semAn.pop(controlVar, symTab);//pop the initial value into the control variable
                
                stepValue();
                Token ourStepVal = stepVal;//store the step value for later
                
                semAn.placeLabel(forLoopEnter);//the loop starts here
                finalValue();//final value will naturally be on the top of the stack
                semAn.pushCheck(controlVar, symTab);//push the control variable on the stack
                semAn.branch(forLoopExit, "equals");//check to see if they are equal, and branch if they are
                
                match("MP_DO");
                statement();
                
                semAn.pushCheck(controlVar, symTab);//push the control var
                semAn.stepValue(ourStepVal);//find the step val and push it on the stack
                semAn.addOp(new Token("+", "MP_PLUS", -1, -1));//do an add stack
                semAn.pop(controlVar, symTab);//pop that into the control var
                semAn.branch(forLoopEnter, "always");//loop back top to check condition
                semAn.placeLabel(forLoopExit);//exit label goes here
                
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
                stepVal = lastTok;
                break;
            case "MP_DOWNTO":
            	System.out.println(" (#65)"); // Rule #65
                match("MP_DOWNTO");
                stepVal = lastTok;
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

    	System.out.println(" (#67)");// Rule #67
    	Token procedure = lastTok;
    	paramTypeList = "";//create a list of the parameter types to check for when we call the procedure
		optionalActualParameterList();
		semAn.functionProcedureCall(paramTypeList, symTab, procedure);//make a call to the procedure
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
                paramTypeList += semAn.topOfStackType() + " ";//get the type of whatever's been pushed on, and append to our parameter type list
                actualParameterTail();
                match("MP_RPAREN");
                paramTypeList = paramTypeList.substring(0, paramTypeList.length() - 1);//pull the last trailing space off
                semAn.comma();
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
                semAn.comma();
                actualParameter();
                paramTypeList += semAn.topOfStackType() + " ";//append the type to our list
                actualParameterTail();
                break;
            case "MP_RPAREN":
            	System.out.println(" (#71)"); // Rule #71
                break;
            default:
                syntaxError(); 
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
                Token operator = lastTok;//keep track of the operator used
                simpleExpression();
                semAn.addComp(operator);//add the comparison after the second half of the equation's been pushed on
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
                Token operator = lastTok;//Store the last match, which will have been the adding operation
                term();
                semAn.addOp(operator);//adds the operation after the next term has been pushed
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
                semAn.makePos();//sets a boolean so we know the next value pushed on has to change sign
                break;
            case "MP_MINUS":
            	System.out.println(" (#86)"); // Rule #86
                match("MP_MINUS");
                semAn.makeNeg();//same as makePos
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
                semAn.checkBothSides();
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
                Token operator = lastTok;
                factor();
                semAn.addOp(operator);//add the operator after the last factor has been pushed on
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
                semAn.checkBothSides();
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
                semAn.pushCheck(lastTok, symTab);//pushes the value onto the stack
                break;
            case "MP_FLOAT_LIT":  
            	System.out.println(" (#100)"); // Rule #100
                match("MP_FLOAT_LIT");
                semAn.pushCheck(lastTok, symTab);
                break;
            case "MP_FIXED_LIT":// **Stephen: Added FIXED_LIT
            	System.out.println(" (#100)"); // Rule #100
                match("MP_FIXED_LIT");
                semAn.pushCheck(lastTok, symTab);
                break;
            case "MP_STRING_LIT":
            	System.out.println(" (#101)"); // Rule #101
                match("MP_STRING_LIT");
                semAn.pushCheck(lastTok, symTab);
                break;
            case "MP_TRUE":
            	System.out.println(" (#102)"); // Rule #102
                match("MP_TRUE");
                semAn.pushCheck(lastTok, symTab);
                break;
            case "MP_FALSE":
            	System.out.println(" (#103)"); // Rule #103
                match("MP_FALSE");
                semAn.pushCheck(lastTok, symTab);
                break;
            case "MP_NOT":
            	System.out.println(" (#104)"); // Rule #104
                match("MP_NOT");
                Token not = lastTok;
                factor();
                semAn.pushCheck(not, symTab);
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
                Token identifier = lastTok;
                Row funcOrVar = symTab.findVariable(lastID);//we need to know if the id we grabbed is for a variable or for a function call
                
                if(funcOrVar == null)syntaxError();//if it doesn't come back right, the identifier isn't in a symbol table
                
                if(funcOrVar.getKind().equals("function")){//if it's function call
                	semAn.pushRoomForRetVal(funcOrVar.getType());//makes room in the stack, as well as saves the type of the stack before this call is made
                	String oldParams = paramTypeList;//store the old parameters list in case function call is used as parameter type
                	paramTypeList = "";//reset the list
                	optionalActualParameterList();//fill the list
                	semAn.functionProcedureCall(paramTypeList, symTab, identifier);//make the calls and check that the parameters given fit what is needed
                	paramTypeList = oldParams;//restore the old list
                }
                else if(funcOrVar.getKind().equals("retVar")){//if it's a recursive function call, the type that will come back is the returnVariable type
                	semAn.pushRoomForRetVal(funcOrVar.getType());
                	String oldParams = paramTypeList;
                	paramTypeList = "";
                	optionalActualParameterList();
                	semAn.functionProcedureCall(paramTypeList, symTab.getParent(), identifier);
                	paramTypeList = oldParams;
                }
                else{// if the identifier is just a normal variable
                	semAn.pushCheck(identifier, symTab);
                }
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
