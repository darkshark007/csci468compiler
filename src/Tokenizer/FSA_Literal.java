package csci468compiler;

public class FSA_Literal extends Tokenizer {
	

	/* INHERITED VARIABLES FROM TOKENIZER
	private Token token;
	private Scanner myScanner;
	*/
	
	public FSA_Identifier(MicroPascalScanner in) {
		super(in);
	}
	
	public enum State {
		firstDigitState,
		secondDigitState,
		nextChar,
		not,
		
		RETURN,
		ERROR
	}
	
	public Token getToken()
	{
		String lexeme = "";
		String lastAcceptableLexeme = "";
		String token = "Not yet determined";
		String errorMsg = "";
		State t = State.firstDigitState;
		int lineNum = myScanner.getLineNum();
		int colNum = myScanner.getColNum();
		
		char nextChar = '0';
		if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
		else t = State.RETURN;
		
		while(true)
		{
			switch(t)
			{
			case firstDigitState:
				
				if (Character.isDigit(nextChar)) 
				{	//if the next char is a digit, record it and change states.
					lexeme = lexeme + nextChar;
					t = State.secondDigitState;
					break;
				}
				else 
				{
					t = State.RETURN;
					// Invalid character
					break;
				}
			case secondDigitState:
				// This state is a valid accept state;
				lastAcceptableLexeme = lexeme;
				
				if (Character.isDigit(nextChar)) 
				{	//if the next char is a digit, record it and stay in this state.
					lexeme = lexeme + nextChar;
					t = State.secondDigitState;
					break;
				}
				else if ()
				{
					
				}
			}
			
			if ( t == State.ERROR ) 
			{
				myScanner.throwError(errorMsg);
				t = State.RETURN;
			}

			if ( t == State.RETURN ) 
			{
				return new Token(lastAcceptableLexeme,token,lineNum,colNum);
			}
			
			if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
			else t = State.RETURN;
		}
		
	}
//	markReturnSpot();
//	Token returnToken;
//	char currentChar = 'd';
//	if (Character.isDigit(currentChar))
//	{
//		lexeme.concat(Character.toString(currentChar)); 
//		returnToken = Token.MP_INTEGER_LIT; 			
//	}
//	else
//	{
//		try
//		{
//		fileReader.reset();	
//		} catch (IOException e) 
//		{
//			System.out.print("The fileReader is confused about jumping back to the mark.");
//		}
//		nextCharacter();
//		return Token.MP_ERROR;
//	}
//	nextCharacter();
//	while (Character.isDigit(currentChar))
//	{
//		lexeme.concat(Character.toString(currentChar));
//		nextCharacter();
//	}
//	if (currentChar == '.')
//	{
//		
//		nextCharacter();
//		if (Character.isDigit(currentChar))
//		{
//			lexeme.concat("."); 
//			lexeme.concat(Character.toString(currentChar)); 
//			returnToken = Token.MP_INTEGER_LIT; 			
//		}
//		else
//		{
//			//Stuff and things.  Sleep time now.
//		}
//	}
//	
//	return returnToken;
}
