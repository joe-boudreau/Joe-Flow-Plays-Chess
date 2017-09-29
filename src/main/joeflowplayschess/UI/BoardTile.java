/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess.UI;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

/**
 * Object which represents each of the 64 squares on the board. Contains a field
 * pieceOnTile which contains a pointer to the ChessPiece object which occupies it.
 * If it is empty, it points to null. 
 * 
 * No visual component on the GUI unless its border is "lit" up after a move is made
 * to or from it. Lights up Cyan for White and Magenta for Black. I thought it looked
 * cool. Anyways, the rest of the painting for the square is handled by the lower layer
 * in the JLayeredPane, the chessboard image.
 *
 * @author jboudrea
 */
public class BoardTile extends JLabel{
    
    private ChessPiece pieceOnTile;
    private int R;
    private char C;
    
    
    public BoardTile(ChessPiece cP, int Row, char Col){
        
        R = Row; // attempt to make more readable code by storing under
        C = Col; // their common notation, a.k.a a5 , g1, h2
        pieceOnTile = cP; //initialized to null in usage
        
        int[] rowCol = ANtoArrayIndex(Row, Col);
        //set location of the actual Object on the square
        setBounds(100*(rowCol[1]), 780 - 100*(rowCol[0]), 100, 100);
        

    }
    
    private int[] ANtoArrayIndex(int Row, char Col){
        
        return new int[]{Row - 1, (int) Col - 97};
    }
    
    public int getSquareIndex(){
        
        int[] rowCol = ANtoArrayIndex(R, C);
        
        return rowCol[0]*8 + rowCol[1];
    }
    
    public void setPiece(ChessPiece cP){
        pieceOnTile = cP;
    }
    
    public ChessPiece getPiece(){
        return pieceOnTile;
    }
    
    public boolean isEmpty(){
        return getPiece() == null;
    }
    
    
    public void lightUp(int colour){
        
        if(colour == 0){ //white
            setBorder(new LineBorder(Color.cyan, 2));
        }
        else{ //black
            setBorder(new LineBorder(Color.MAGENTA, 2));
        }
    }
    
    public void lightDown(){
        setBorder(null);
    }
}
