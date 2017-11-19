/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess.UI;

import java.awt.Color;
import java.awt.Point;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Represents a chess piece on the board and contains information about its type and colour.
 * Extends JLabel so it can handle visual representation of the piece.
 * 
 * The method setLocation() calls JLabel's setBounds method in order to position the piece
 * within the window. However ChessPiece objects do not store location information about the
 * piece. The locations are kept track of via the BoardSquare 8x8 array object within the
 * main JoeFlowPlaysChess class.
 * 
 * @author thejoeflow
 */
public class ChessPiece extends JLabel {
  
  private boolean   hasMoved;
  private int       colour;
  private String    type;
 
  
  public ChessPiece(String typePiece, int colourPiece, int Row, char Col) {
    
    type =      typePiece;
    colour =    colourPiece;
    hasMoved =  false;
	
    System.out.println(getClass().getClassLoader().getResource("graphics/"
                                + (colourPiece == 0 ? "w" : "b") 
                                + typePiece + ".png"));
	
      
    setIcon(new ImageIcon(getClass().getResource("/graphics/"
                                + (colourPiece == 0 ? "w" : "b") 
                                + typePiece + ".png")));

    
    int[] rowCol = ANtoArrayIndex(Row, Col);
    setBounds(100*(rowCol[1]), 780 - 100*(rowCol[0]), 100, 100);
    setBackground(new Color(0, 0, 0, 0));
    
  }
    
  
    public int getColour(){
        return colour;
    }
    
    public String getType(){
        return type;
    }
    
    public boolean hasMoved(){
        return hasMoved;
    }
    
    public void setMoved(){
        hasMoved = true;
    }
    
    /**
     * Changes the chess piece type. Needed for pawn promotions.
     * 
     * 
     * @param newType   Promoted piece type
     */
    public void setType(String newType){
          
        String colourStr = colour == 0 ? "w" : "b"; 
        setIcon(new ImageIcon(getClass().getResource("/graphics/" + colourStr + newType + ".png")));
        type = newType;
    }
    
    public void printInfo(){
        System.out.println("Type: " + type + ", Colour: " + colour);
    }
    
    /**
     * Moves the chess piece to a given square, specified via its row and column
     * 
     * @param Row
     * @param Col 
     */
    public void setLocation(int Row, char Col){
        this.setLocation(new Point(100*((int) Col - 97),780 - 100*(Row - 1)));
    }
    
    /**
     * Algebraic Notation to Array Indices function. Converts the algebraic
     * notational representation of ranks and files (1..8 , a..h) to the indices
     * used by data structures within the program (0..7, 0..7)
     * 
     * @param Row   Rank to convert (1..8)
     * @param Col   File to convert (a..h)
     * @return      2-element int array representing [row index, column index]
     */
    private int[] ANtoArrayIndex(int Row, char Col){
        
        return new int[]{Row - 1, (int) Col - 97};
    }
    
    
}
