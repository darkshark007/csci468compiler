package Tokenizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;


public class MicroPascalScanner implements I_Tokenizer {
	
	private File input;
	private int colNum = 0;
	private int tempColNum = 0;
	private int lineNum = 0;
	private String buffer = null;
	private int numLeftParens = 0;
	private boolean hasNextChar = true;
	private FileReader fr;
	LinkedList<Token> tokenList = new LinkedList<Token>();
	Tokenizer iden = new FSA_Identifier(this);
	Tokenizer strSym = new FSA_StrSymbol(this);
	Tokenizer lit = new FSA_Literal(this);
	
	public MicroPascalScanner(File in) {
		input = in;
		try {
			fr = new FileReader(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public LinkedList<Token> getAllTokens() {
		if ( hasNextToken() ) {
			scanAllTokens();
		}
		
		return tokenList;
	}
	
	private void scanAllTokens() {
		while ( hasNextToken() ) 
		{
			getToken();
			//System.out.println("Token>>  "+"Ln:   "+current.getLineNumber()+"\tCl: "+current.getColumnNumber()+"\t\tType:  "+current.getToken()+"\t\tLex:  "+current.getLexeme());
			//tokenList.add(current);
		}
	}

	
	@Override
	public Token getToken() {
		
		// Set up the Line Buffer
		if ( buffer == null ) buffer = getNextLine();
		
		
		
		// Dispatch the token FSA's
		
		char nextChar = peekNextChar();
		
		
		Token tok;
		

		// Update the Line buffer.
		do {
			 if ( buffer.length() == 0 || colNum+tempColNum >= buffer.length() ) {
				 buffer = getNextLine();
			 }
			 if ( buffer == null ) {
				 hasNextChar = false;
			 }

			 if ( !hasNextToken() ) break;
			 nextChar = peekNextChar();
			// Eat up whitespace
			 if ( nextChar == ' ' ) {
				 colNum++;
			 }
			 else if ( nextChar == '	' ) {
				 colNum ++;
			 }
			 else if ( nextChar == '\r' ) {
				 colNum++;
			 }
			 else if ( nextChar == '\n' ) {
				 colNum++;
			 }
			 else if ( nextChar == '{' ) {
				 int commentStart = colNum;
					int commentLine = lineNum;
				 do {
						 if ( buffer.length() == 0 || colNum+tempColNum >= buffer.length() ) {
							 buffer = getNextLine();
						 }
						 if ( buffer == null ) {
							 hasNextChar = false;
						 }

						 if ( !hasNextToken() ) {
							 System.out.println("Run on comment Error Line:" + commentLine + " col:" + commentStart);
							 System.exit(1);
							 break;
						 }
						 nextChar = peekNextChar();
						 colNum++;
						 if(nextChar == '{'){
							 numLeftParens++;
						 }
						 else if(nextChar == '}'){
							 numLeftParens--;
						 }
					} while (numLeftParens > 0);

			 }
			 else break;



		} while (true);
		
		
		
		
		
		if ( Character.isLetter(nextChar) || nextChar == '_' ) 
		{
			tok = iden.getToken();
		}
		else if ( Character.isDigit(nextChar) ) 
		{
			tok = lit.getToken();
		}
		else 
		{
			tok = strSym.getToken();
		}
		
		
		
		
		
		
		// Update the column numbers
		colNum += tok.getLexeme().length();
		if ( tok.getLexeme().length() == 0 ) colNum += 1;
		tempColNum = 0;
		
		
		// Add the token to the list of already found tokens
		tokenList.add(tok);

		return tok;
	}
	
	
	public int getLineNum() { return lineNum; }
	public int getColNum() { return 1+colNum+tempColNum; }
	
	public boolean hasNextToken() {
		return hasNextChar;
	}
	
	public char getNextChar() {
		if ( buffer == null || colNum+tempColNum == buffer.length() ) { return '\n'; }
		char nextChar = buffer.charAt(colNum+tempColNum);
		tempColNum++;
		return nextChar;
	}
	
	public char peekNextChar() {
		if ( colNum+tempColNum == buffer.length() ) { return (char)(-1); }
		char nextChar = buffer.charAt(colNum+tempColNum);
		return nextChar;
	}
	
	private String getNextLine() {
		String result = "";
		lineNum++;
		tempColNum = 0;
		colNum = 0;
		
		int nextChar = 0;
		while (true) {
			try {
				nextChar = fr.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			// If it is a carriage return or a new line char, return.
			if ( nextChar == 10 ) break;

			// If it is an End-Of-File char, do stuff.
			if ( nextChar == -1 ) {
				// If the running string is empty, this is the end of the file.
				if ( result.length() == 0 ) return null;
				
				// Otherwise, the end-of-file character has been reached, but there is still some data to parse.
				break;
			}
			
			// Otherwise, append it to the string
			result += (char)nextChar;
		}
		
		// If this line is empty, for example if there are multiple line breaks, then recursively return the next non-empty line.
		if ( result.length() == 0 ) return getNextLine(); 
		
		return result;
	}
	
	public void throwError(String errorMsg) {
		System.out.println("Scanning Error on line "+lineNum+", col "+colNum+"\r\n  >> "+errorMsg);		
	}
}
