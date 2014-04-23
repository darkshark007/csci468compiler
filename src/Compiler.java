import java.io.File;
import java.util.LinkedList;
import java.util.Scanner;

import Parser.NonTerminals;
import Parser.NonTerminalsCompiler;
import Tokenizer.MicroPascalScanner;
import Tokenizer.Token;


public class Compiler {

	public static void main(String[] args) throws java.io.IOException
	{
		File testFile = new File("TestPrograms\\derpFile.derp");
		Scanner reader = new Scanner(System.in);
		int userChoice = 0;
		
		if( args.length == 0 )
		{
			File folder = new File("TestPrograms");
			File[] listOfFiles = folder.listFiles();
		    
			System.out.println("No input file given, ALL IS LOST. ABANDON SHIP!!!!!!!!!!!!!!!!");
			System.out.println("Actually, lets use a local test file.  Let's see...");
			
			
			System.out.println("Try picking from this list:");
			for (int i = 0; i < listOfFiles.length; i++) 
		    {
		    	if (listOfFiles[i].isFile()) {
		    		System.out.println((i+1) + ": " + listOfFiles[i].getName());
		    	}
		    }
			
			boolean goodInput = false;
			while(!goodInput)
			{
				try
				{
					System.out.print ("\nEnter an integer:\n> ");
					userChoice = reader.nextInt();
					testFile = new File("TestPrograms\\" + listOfFiles[userChoice-1].getName());
					goodInput = true;
				}
				catch (java.util.InputMismatchException e)
				{
					System.out.println("That's not an int.  Please enter an integer from the list.");
					reader.next();
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					System.out.println("That's not in the list.  Please enter an integer from the list.");
				}
			}			
			
		}
		else
		{
			testFile = new File(args[0]);
		}
		
		
		MicroPascalScanner sc = new MicroPascalScanner(testFile);
		System.out.println("\n\nCompiling using File: "+testFile);
		
		LinkedList<Token> list = new LinkedList<Token>();
		list = sc.getAllTokens();

		System.out.println("\nScanning...\n\n");

		System.out.println(
				"Token List:\n"+
				"  Loc [Line, Col]         Type                     Lexeme\n"+
				"--------------------------------------------------------------------------------------");
		for ( Token current : list ) {
			//System.out.print("  >>  "+"Ln:   "+current.getLineNumber()+"\tCl: "+current.getColumnNumber()+"\t\tType:  "+current.getToken()+"\t\tLex:  "+current.getLexeme());
			System.out.printf("    %1$-14s     %2$-20s     "+current.getLexeme()+"\n","["+current.getLineNumber()+", "+current.getColumnNumber()+"]",current.getToken());
			
		}
		
		System.out.println("\n\n\nParsing...\n\n");
		NonTerminals.start(list);

		String fOut = (testFile.getAbsolutePath())+".il";
		File compFile = new File(fOut);
		NonTerminalsCompiler.compile(list,compFile);

	}

}
