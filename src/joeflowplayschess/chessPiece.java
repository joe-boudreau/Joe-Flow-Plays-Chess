/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

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
  
  private int r, c, colour, temp_r, temp_c;
  private String type;
  private GameBoard gameState;
  private boolean hasMoved;
  private boolean isCapture;
  
  public chessPiece(int row, int col, String typePiece, int colourPiece) {
      
    String colourStr = colourPiece == 0 ? "w" : "b";  
    setIcon(new ImageIcon(getClass().getResource("/resources/" + colourStr + typePiece + ".png")));
    r = row;
    c = col;
    type = typePiece;
    colour = colourPiece;
    
    setBounds(100*(c-1), 800 - 100*(r-1), 100, 100);
    
    isCapture = false;
    hasMoved = false;

    addMouseListener(new MouseListener() {

      @Override
      public void mouseClicked(MouseEvent e) { }

      @Override
      public void mousePressed(MouseEvent e) {
        screenX = e.getXOnScreen();
        screenY = e.getYOnScreen();

        myX = getX();
        myY = getY();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
          
        System.out.println(getLocation().x + ", " + getLocation().y);

        correctLocation();
      }
      
      @Override
      public void mouseEntered(MouseEvent e) { }

      @Override
      public void mouseExited(MouseEvent e) { }


    });
    addMouseMotionListener(new MouseMotionListener() {

      @Override
      public void mouseDragged(MouseEvent e) {
        int deltaX = e.getXOnScreen() - screenX;
        int deltaY = e.getYOnScreen() - screenY;

        setLocation(myX + deltaX, myY + deltaY);
       
      }

      @Override
      public void mouseMoved(MouseEvent e) { }

    });
    
  }
    
    public void updateGameBoard(GameBoard gb){
        gameState = gb;
    }
    public String getColour(){
        return colour == 0 ? "White" : "Black";
    }
    
    public int[] getPosition(){
        return new int[]{r, c};
    }
    
    public String getType(){
        return type;
    }
    
    public void printPieceInfo(){
        int[] pos = getPosition();
        System.out.println(getColour() + " " + getType() + " at (" + pos[0] + ", " + pos[1] + ")");
    }
    
  
    private void correctLocation(){
        int x = getLocation().x;
        int y = getLocation().y;
        
        boolean rowFound = false;
        int rowStartX = 700;
        
        while(!rowFound){
            if(x+50 >= rowStartX){
                    x = rowStartX;
                    rowFound = true;
                    break;
            }
            rowStartX = rowStartX - 100;
            if(rowStartX == 0){
                x = rowStartX;
                rowFound = true;
            }
        }
        
        boolean colFound = false;
        int colStartY = 800;
        
        while(!colFound){
            if(y+50 >= colStartY){
                    y = colStartY;
                    colFound = true;
                    break;
            }
            colStartY = colStartY - 100;
            if(colStartY == 100){
                y = colStartY;
                colFound = true;
            }
        }
        int newRow = (int)(800-y)/100 + 1;
        int newCol = (int)(x)/100 + 1;
        String move = moveType(colour, type, newRow, newCol, r, c);
        
        if(move.equals("INVALID")){
            movePiece(r, c);
        }
        else{
            hasMoved = true;
            setLocation(new Point(x, y));
            isCapture = move.equals("CAPTURE") ? true : false;
            temp_r = r;
            temp_c = c;
            r = newRow;
            c = newCol;    
            }
        }
    
    public boolean hasMoved(){
        return hasMoved;
    }
    
    public boolean isCapture(){
        return isCapture;
    }
    
    public int[] newPosition(){
        hasMoved = false;
        isCapture = false;
        return new int[]{r, c};
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
                if(colour == 1 && gameState.getPieceAt(newR, newC).getColour().equals("WHITE")){
                    return "CAPTURE";
                }
                else if(colour == 0 && gameState.getPieceAt(newR, newC).getColour().equals("BLACK")){
                    return "CAPTURE";
                }
                return "INVALID";
                
        
        }
        return "VALID";
    }
    
    private void movePiece(int row, int col){
        setLocation(new Point(100*(col-1),800 - 100*(row-1)));
    }
    
    
}
