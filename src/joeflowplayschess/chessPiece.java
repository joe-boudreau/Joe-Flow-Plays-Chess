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

  private volatile int screenX = 0;
  private volatile int screenY = 0;
  private volatile int myX = 0;
  private volatile int myY = 0;
  
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
    setBounds(100*(rowCol[1]), 800 - 100*(rowCol[0]), 100, 100);
    
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

    private String moveType(int colour, String type, int newR, int newC, int R, int C){
            
        switch(type){
            
            case "Rook":
                if(!(newR == R || newC == C)){
                    return "INVALID";
                }
                if(newR == R){
                    for(int col = C; col < newC; col++){
                        if(gameState.getPieceAt(R,col+1) != null){
                            return "INVALID";
                        }
                    }
                }
                else{
                    for(int row = R; row < newR; row++){
                        if(gameState.getPieceAt(row+1,C) != null){
                            return "INVALID";
                        }
                    }
                }
                if(gameState.getPieceAt(newR, newC) != null && colour == 1 && gameState.getPieceAt(newR, newC).getColour().equals("WHITE")){
                    return "CAPTURE";
                }
                else if(gameState.getPieceAt(newR, newC) != null && colour == 0 && gameState.getPieceAt(newR, newC).getColour().equals("BLACK")){
                    return "CAPTURE";
                }
                return "VALID";
                
        
        }
        return "VALID";
    }
    
    
    private int[] ANtoArrayIndex(int Row, char Col){
        
        return new int[]{Row - 1, (int) Col - 97};
    }
    
    
}
