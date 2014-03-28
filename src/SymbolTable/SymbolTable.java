package SymbolTable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * @author Jon Koenes
 */
public class SymbolTable {

    LinkedList<Row> items = new LinkedList<>();
    LinkedList<SymbolTable> children = new LinkedList<>();
    SymbolTable parent;
    int nestLevel;
    int size = 1;
    String tableName;

    //main constructor needs a parent, the nesting level, and the name of the table
    private SymbolTable(SymbolTable in_parent, int in_nest, String name) {
        parent = in_parent;
        nestLevel = in_nest;
        tableName = name;
    }

    public SymbolTable(String in_name) {
        parent = null;
        nestLevel = 0;
        tableName = in_name;
    }

    public String getName() {
        return tableName;
    }
    
    public int getNestingLevel() {
        return nestLevel;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public int getSize() {
        return size;
    }

    public void setName(String name) {
        tableName = name;
    }

    public void setNestingLevel(int level) {
        nestLevel = level;
    }

    public void setParent(SymbolTable newParent) {
        parent = newParent;
    }

    /*
     Every variable has size 1, so size is increased by one each time we make a new row, 
     with 1 being the starting size to save room for the display register value later
     */
    public void addRow(String ID, String kind, String type, String returnValues, String inputParameters) {
        Row newRow = new Row(ID, kind, type, size++, 1, returnValues, inputParameters);
        items.add(newRow);
    }

    /*
     This method will find and return the row for information regarding an ID, following 
     the scope hierarchy. If no row with that particular ID is found, then null is returned
     */
    public Row findVariable(String id) {

        Row returnRow;
        ListIterator<Row> it = items.listIterator();

        while (it.hasNext()) {
            returnRow = it.next();
            if (returnRow.getID() == id) {
                return returnRow;
            }
        }
        //the row we're looking for isn't in our list, we'll check our parent
        if (parent == null) { //have no parent, so variable doesn't exist
            return null;
        } else {
            return parent.findVariable(id);
        }
    }

    public SymbolTable makeNewTable(String name) {
        SymbolTable newTable = new SymbolTable(this, nestLevel + 1, name);
        children.add(newTable);
        return newTable;
    }
    
    /*
     * Finds all the items of kind "Parameter" and returns the list as a string
     * This function is used in getting a list of parameter for inserting functions and parameters into the symbol tables
     */
    public String getParameters(){
    	 Row current;
         ListIterator<Row> myitems = items.listIterator();

         String retString = "";
         while (myitems.hasNext()) {//prints items of current table
             current = myitems.next();
             if(current.getKind() == "param"){
            	 retString += current.getType() + " ";
             }
         }
         
         return retString;
    }

    /*
     method will print from current table down, including all children, but no parent
     */
    public void printTableFromHere() {

        Row current;
        ListIterator<Row> myitems = items.listIterator();

        while (myitems.hasNext()) {//prints items of current table
            current = myitems.next();
            current.printRow();
        }

        SymbolTable currentTable;
        ListIterator<SymbolTable> childs = children.listIterator();
        while (childs.hasNext()) {//tell the children to print

        	currentTable = childs.next();
        	currentTable.printTableFromHere();
        }
    }

    /*
     prints entire symbol table tree
     */
    public void printTableFromTop() {

        if (parent != null) { //keeps traveling up the tree until we reach root parent
            parent.printTableFromTop();
        } 
        
        else { //we've are at the "root" table
            
            Row current;
            ListIterator<Row> myitems = items.listIterator();
            
            while (myitems.hasNext()) {//prints items of current table
                current = myitems.next();
                current.printRow();
            }

            SymbolTable currentTable;
            ListIterator<SymbolTable> childs = children.listIterator();
            while (childs.hasNext()) {//tell the children to print
            	currentTable = childs.next();
            	currentTable.printTableFromHere();
            }
        }

    }

    /*
     prints only this symbol table
     */
    public void print() {
        Row current;
        ListIterator<Row> myitems = items.listIterator();

        while (myitems.hasNext()) {//prints items of current table
            current = myitems.next();
            current.printRow();
        }
    }

}
