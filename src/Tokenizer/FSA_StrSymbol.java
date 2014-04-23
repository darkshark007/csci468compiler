package Tokenizer;

public class FSA_StrSymbol extends Tokenizer {

	public FSA_StrSymbol(MicroPascalScanner in) {
		super(in);
	}

	public Token getToken() {
		Token retTok = new Token();
		String lex = "";
		int line = myScanner.getLineNum();
		int col = myScanner.getColNum();

		char nextChar = myScanner.getNextChar();

		boolean foundToken = false;

			switch (nextChar) {
			case ':':
				lex += nextChar;
				nextChar = myScanner.getNextChar();
				if (nextChar == '=') {
					lex += nextChar;
					retTok = new Token(lex, "MP_ASSIGN", line, col);
				} else {
					retTok = new Token(lex, "MP_COLON", line, col);
				}
				foundToken = true;
				break;


			case ',':
				lex += nextChar;
				retTok = new Token(lex, "MP_COMMA", line, col);
				foundToken = true;
				break;


			case '=':
				lex += nextChar;
				retTok = new Token(lex, "MP_EQUAL", line, col);
				foundToken = true;
				break;


			case '/':
				lex += nextChar;
				retTok = new Token(lex, "MP_FLOAT_DIVIDE", line, col);
				foundToken = true;
				break;


			case '>':
				lex += nextChar;
				nextChar = myScanner.getNextChar();
				if (nextChar == '=') {
					lex += nextChar;
					retTok = new Token(lex, "MP_GEQUAL", line, col);
				} else {
					retTok = new Token(lex, "MP_GTHAN", line, col);
				}
				foundToken = true;
				break;


			case '<':
				lex += nextChar;
				nextChar = myScanner.getNextChar();
				if (nextChar == '=') {
					lex += nextChar;
					retTok = new Token(lex, "MP_LEQUAL", line, col);
				} else if (nextChar == '>') {
					lex += nextChar;
					retTok = new Token(lex, "MP_NEQUAL", line, col);
				} else {
					retTok = new Token(lex, "MP_LTHAN", line, col);
				}
				foundToken = true;
				break;


			case '.':
				lex += nextChar;
				retTok = new Token(lex, "MP_PERIOD", line, col);
				foundToken = true;
				break;


			case '+':
				lex += nextChar;
				retTok = new Token(lex, "MP_PLUS", line, col);
				foundToken = true;
				break;


			// **Stephen**: Added this case for the Minus symbol, it got left out somehow
			case '-':
				lex += nextChar;
				retTok = new Token(lex, "MP_MINUS", line, col);
				foundToken = true;
				break;


			case '(':
				lex += nextChar;
				retTok = new Token(lex, "MP_LPAREN", line, col);
				foundToken = true;
				break;


			case ')':
				lex += nextChar;
				retTok = new Token(lex, "MP_RPAREN", line, col);
				foundToken = true;
				break;


			case ';':
				lex += nextChar;
				retTok = new Token(lex, "MP_SCOLON", line, col);
				foundToken = true;
				break;


			case '*':
				lex += nextChar;
				retTok = new Token(lex, "MP_TIMES", line, col);
				foundToken = true;
				break;


			case'\'':
				lex+= nextChar;
				try{
					nextChar = myScanner.getNextChar();
					while(nextChar != '\''){
						if (nextChar == '\r' || nextChar == '\n' ) {
							throw new IndexOutOfBoundsException();
						}
						lex+=nextChar;
						nextChar = myScanner.getNextChar();
					}
					lex+= nextChar;
					retTok = new Token(lex, "MP_STRING_LIT", line, col);
					foundToken = true;
				}
				catch(IndexOutOfBoundsException e){//if hit end of line while looking for string
					retTok = new Token(lex, "MP_RUN_STRING", line, col);
					foundToken = true;
				}
				break;

			default:
				System.out.println("StrSymFSA asked to scan for item that was not string or symbol:\t"+nextChar+" == "+(int)nextChar);
				foundToken = true;
				retTok = new Token("","MP_ERROR", line, col);
			}//end switch
		return retTok;
	}
}