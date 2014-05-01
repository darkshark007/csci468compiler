package Tokenizer;

/**
 *
 * @author Jon Koenes
 */
public class Token {
    
    public String lexeme;
    protected String token;
    protected int lineNum;
    protected int colNum;
    
    public Token(String in_lex, String in_token, int lnum, int colnum){
        lexeme = in_lex;
        token = in_token;
        lineNum = lnum;
        colNum = colnum;
    }
    
    public Token(){
        lexeme = "";
        token = "EMPTY_TOKEN";
        lineNum = 0;
        colNum = 0;
    }
    
    public String getLexeme(){
        return lexeme;
    }
    
    public String getToken(){
        return token;
    }
    
    public int getLineNumber(){
        return lineNum;
    }
    
    public int getColumnNumber(){
        return colNum;
    }
}
