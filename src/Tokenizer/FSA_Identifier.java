package Tokenizer;

public class FSA_Identifier extends Tokenizer {
	
    		/* INHERITED VARIABLES FROM TOKENIZER
	private Token token;
	private Scanner myScanner;
     		*/
	
	public FSA_Identifier(MicroPascalScanner in) {
		super(in);
	}

	
	public enum State {
		firstChar,
		nextChar,
		nonUnderscore,
		
		RETURN,
		ERROR
	}
	@Override
	public Token getToken() {
		
		String lexeme = "";
		String lastAcceptableLexeme = "";
		String token = "MP_IDENTIFIER";
		String errorMsg = "";
		State t = State.firstChar;
		int lineNum = myScanner.getLineNum();
		int colNum = myScanner.getColNum();

		
		char nextChar = '0';
		if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
		else t = State.RETURN;
		
		while (true) {
			// Process this character depending on what state i'm in
			switch(t) {
			case firstChar:
				// In this state, i am only able to accept a LETTER or an UNDERSCORE
				if ( Character.isLetter(nextChar) ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.nextChar;
					break;
				}
				else if ( nextChar == '_' ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;					
					t = State.nonUnderscore;
					break;
				}
				else {
					t = State.RETURN;
					// Invalid character
					break;
				}
				
			case nextChar:
				// This state is a valid accept state;
				lastAcceptableLexeme = lexeme;
				
				// In this state, i am able to accept a LETTER, DIGIT or an UNDERSCORE
				if ( Character.isLetter(nextChar) ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.nextChar;
					break;
				}
				else if ( Character.isDigit(nextChar) ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.nextChar;
					break;
				}
				else if ( nextChar == '_' ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;					
					t = State.nonUnderscore;
					break;
				}
				else {
					t = State.RETURN;
					// Invalid character
					break;
				}
				
			case nonUnderscore:
				// In this state, i am able to accept a LETTER or DIGIT
				if ( Character.isLetter(nextChar) ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.nextChar;
					break;
				}
				else if ( Character.isDigit(nextChar) ) {
					// If the next character is a letter, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.nextChar;
					break;
				}
				else if ( nextChar == '_' ) {
					// ERROR, not an acceptable character.
					errorMsg = "Underscores cannot follow an underscore in an Identifier.";
					t = State.ERROR;
					break;
				}
				else {
					t = State.RETURN;
					// Invalid character
					break;
				}
				
			default:
				break;
				
			}
			
			if ( t == State.ERROR ) {
				myScanner.throwError(errorMsg);
				t = State.RETURN;
			}

			if ( t == State.RETURN ) {
				
				if ( lastAcceptableLexeme.length() == 0 ) {
					myScanner.throwError("Invalid Identifier Syntax: "+'"'+lexeme+'"');
				}
				
				lastAcceptableLexeme = lastAcceptableLexeme.toLowerCase();
				
				// Check this token against a list of known strings
				if ( lastAcceptableLexeme.toLowerCase().equals("begin") ) token = "MP_BEGIN";
				else if ( lastAcceptableLexeme.toLowerCase().equals("boolean") ) token = "MP_BOOLEAN";
				else if ( lastAcceptableLexeme.toLowerCase().equals("and") ) token = "MP_AND";
				else if ( lastAcceptableLexeme.toLowerCase().equals("div") ) token = "MP_DIV";
				else if ( lastAcceptableLexeme.toLowerCase().equals("do") ) token = "MP_DO";
				else if ( lastAcceptableLexeme.toLowerCase().equals("downto") ) token = "MP_DOWNTO";
				else if ( lastAcceptableLexeme.toLowerCase().equals("else") ) token = "MP_ELSE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("end") ) token = "MP_END";
				else if ( lastAcceptableLexeme.toLowerCase().equals("false") ) token = "MP_FALSE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("fixed") ) token = "MP_FIXED";
				else if ( lastAcceptableLexeme.toLowerCase().equals("float") ) token = "MP_FLOAT";
				else if ( lastAcceptableLexeme.toLowerCase().equals("for") ) token = "MP_FOR";
				else if ( lastAcceptableLexeme.toLowerCase().equals("function") ) token = "MP_FUNCTION";
				else if ( lastAcceptableLexeme.toLowerCase().equals("if") ) token = "MP_IF";
				else if ( lastAcceptableLexeme.toLowerCase().equals("integer") ) token = "MP_INTEGER";
				else if ( lastAcceptableLexeme.toLowerCase().equals("mod") ) token = "MP_MOD";
				else if ( lastAcceptableLexeme.toLowerCase().equals("not") ) token = "MP_NOT";
				else if ( lastAcceptableLexeme.toLowerCase().equals("or") ) token = "MP_OR";
				else if ( lastAcceptableLexeme.toLowerCase().equals("procedure") ) token = "MP_PROCEDURE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("program") ) token = "MP_PROGRAM";
				else if ( lastAcceptableLexeme.toLowerCase().equals("read") ) token = "MP_READ";
				else if ( lastAcceptableLexeme.toLowerCase().equals("repeat") ) token = "MP_REPEAT";
				else if ( lastAcceptableLexeme.toLowerCase().equals("string") ) token = "MP_STRING";
				else if ( lastAcceptableLexeme.toLowerCase().equals("then") ) token = "MP_THEN";
				else if ( lastAcceptableLexeme.toLowerCase().equals("true") ) token = "MP_TRUE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("to") ) token = "MP_TO";
				else if ( lastAcceptableLexeme.toLowerCase().equals("until") ) token = "MP_UNTIL";
				else if ( lastAcceptableLexeme.toLowerCase().equals("var") ) token = "MP_VAR";
				else if ( lastAcceptableLexeme.toLowerCase().equals("while") ) token = "MP_WHILE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("write") ) token = "MP_WRITE";
				else if ( lastAcceptableLexeme.toLowerCase().equals("writeln") ) token = "MP_WRITELN";						
						
				
				return new Token(lastAcceptableLexeme,token,lineNum,colNum);
			}

			// Get the next char
			if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
			else t = State.RETURN;
			
			//System.out.println("Processing "+nextChar+", Entering state  "+t);
		}
		// Something went wrong, unable to parse a token.
		//return null;
	}
	
	

}
