/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;


public class chessPiece
    extends JLabel {
  
  private boolean hasMoved;
  private int colour;
  private String type;
  private int row;
  private char col;
  
  public chessPiece(String typePiece, int colourPiece, int Row, char Col) {
      
    String colourStr = colourPiece == 0 ? "w" : "b";  
    setIcon(new ImageIcon(getClass().getResource("/resources/" + colourStr + typePiece + ".png")));
    type = typePiece;
    colour = colourPiece;
    hasMoved = false;
    
    int[] rowCol = ANtoArrayIndex(Row, Col);
    setBounds(100*(rowCol[1]), 780 - 100*(rowCol[0]), 100, 100);
    
    
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
    
    public void setType(String newType){
          
        String colourStr = colour == 0 ? "w" : "b"; 
        setIcon(new ImageIcon(getClass().getResource("/resources/" + colourStr + newType + ".png")));
        type = newType;
    }
    
    public void printInfo(){
        System.out.println("Type: " + type + ", Colour: " + colour);
    }
    
    
    public void setLocation(int Row, char Col){
        this.setLocation(new Point(100*((int) Col - 97),780 - 100*(Row - 1)));
    }
    
    private int[] ANtoArrayIndex(int Row, char Col){
        
        return new int[]{Row - 1, (int) Col - 97};
    }
    
    
}
