/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

/**
 *
 * @author jboudrea
 */
public class BoardTile extends JLabel{
    
    chessPiece pieceOnTile;
    int R;
    char C;
    private Color moveSq;
    
    public BoardTile(chessPiece cP, int Row, char Col){
        
        R = Row;
        C = Col;
        pieceOnTile = cP;
        
        
        moveSq = new Color(100, 10, 15, 50);
        
        int[] rowCol = ANtoArrayIndex(Row, Col);
        setBounds(100*(rowCol[1]), 780 - 100*(rowCol[0]), 100, 100);
        
        //setBorder(new LineBorder(Color.BLACK, 2));


    }
    
    private int[] ANtoArrayIndex(int Row, char Col){
        
        return new int[]{Row - 1, (int) Col - 97};
    }
    
    public int getSquareIndex(){
        
        int[] rowCol = ANtoArrayIndex(R, C);
        
        return rowCol[0]*8 + rowCol[1];
    }
    
    public void setPiece(chessPiece cP){
        pieceOnTile = cP;
    }
    
    public chessPiece getPiece(){
        return pieceOnTile;
    }
    
    public boolean isEmpty(){
        return getPiece() == null;
    }
    
    
    public void lightUp(int colour){
        
        if(colour == 0){
            setBorder(new LineBorder(Color.cyan, 2));
        }
        else{
            setBorder(new LineBorder(Color.MAGENTA, 2));
        }
    }
    
    public void lightDown(){
        //setBorder(new LineBorder(Color.BLACK, 2));

        setBorder(null);
    }
    
    /*
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(validSq);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
    */    
    
}
