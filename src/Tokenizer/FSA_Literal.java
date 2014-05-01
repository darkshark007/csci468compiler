package Tokenizer;

public class FSA_Literal extends Tokenizer {


	/* INHERITED VARIABLES FROM TOKENIZER
	private Token token;
	private Scanner myScanner;
	 */

	public FSA_Literal(MicroPascalScanner in) {
		super(in);
	}

	public enum State {
		firstDigit,
		secondAndOnDigit,
		justPassedDecimal,
		fixedOrFloat,
		justPassedExponent,
		justPassedPlusOrMinus,
		finalDigits,

		RETURN,
		ERROR
	}

	public Token getToken()
	{
		String lexeme = "";
		String lastAcceptableLexeme = "";
		String token = "Not yet determined";
		String lastAcceptableType = "Not yet determined";
		String errorMsg = "";
		State t = State.firstDigit;
		int lineNum = myScanner.getLineNum();
		int colNum = myScanner.getColNum();

		char nextChar = '0';
		if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
		else t = State.RETURN;

		while(true)
		{
			switch(t)
			{
			case firstDigit:
				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					token = "MP_INTEGER_LIT";
					t = State.secondAndOnDigit;
					break;
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}



			case secondAndOnDigit:
				// This state is a valid accept state;
				lastAcceptableType = token;
				lastAcceptableLexeme = lexeme;

				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.secondAndOnDigit;
					break;
				}
				else if ( nextChar == '.' ) {
					lexeme = lexeme + nextChar;
					t = State.justPassedDecimal;
					token = "MP_FIXED_LIT";
					break;					
				}
				else if ( nextChar == 'e' || nextChar == 'E' ) {
					lexeme = lexeme + nextChar;
					t = State.justPassedExponent;
					token = "MP_FLOAT_LIT";
					break;					
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}


			case justPassedDecimal:
				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.fixedOrFloat;
					break;
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}

			case fixedOrFloat:
				// This state is a valid accept state;
				lastAcceptableType = token;
				lastAcceptableLexeme = lexeme;

				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.fixedOrFloat;
					break;
				}
				else if ( nextChar == 'e' || nextChar == 'E' ) {
					lexeme = lexeme + nextChar;
					t = State.justPassedExponent;
					token = "MP_FLOAT_LIT";
					break;					
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}

			case finalDigits:
				// This state is a valid accept state;
				lastAcceptableType = token;
				lastAcceptableLexeme = lexeme;

				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.finalDigits;
					break;
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}

			case justPassedExponent:
				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.finalDigits;
					break;
				}
				else if ( nextChar == '+' || nextChar == '-' ) {
					lexeme = lexeme + nextChar;
					t = State.justPassedPlusOrMinus;
					break;					
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}


			case justPassedPlusOrMinus:
				if ( Character.isDigit(nextChar) ) {
					lexeme = lexeme + nextChar;
					t = State.finalDigits;
					break;
				}
				else {
					// Invalid character
					t = State.RETURN;
					break;
				}


			default:
				break;
			}

			if ( t == State.ERROR ) 
			{
				myScanner.throwError(errorMsg);
				t = State.RETURN;
			}

			if ( t == State.RETURN ) 
			{
				return new Token(lastAcceptableLexeme,lastAcceptableType,lineNum,colNum);
			}

			if ( myScanner.hasNextToken() ) nextChar = myScanner.getNextChar();
			else t = State.RETURN;
		}
	}
}
