/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

/**
 *
 * @author jboudrea
 */
public class GameBoard {
    
    chessPiece[][] gameBoard;
    boolean wCastleQ, wCastleK, bCastleQ, bCastleK;
    int piecesLeft;
    
    public GameBoard(chessPiece[] wPieces, chessPiece[] bPieces){
            piecesLeft = 32;
            setUpStart(wPieces, bPieces);
    }
    
    public GameBoard(){
        
    }
    
    private void setUpStart(chessPiece[] wPieces, chessPiece[] bPieces){
        gameBoard = new chessPiece[8][8];
        
        for(int col = 1; col < 9; col++){
            gameBoard[0][col-1] = wPieces[col-1];
            gameBoard[1][col-1] = wPieces[col-1+8];
            
            gameBoard[7][col-1] = bPieces[col-1];
            gameBoard[6][col-1] = bPieces[col-1+8];
            
        }
    }
    
    private chessPiece[] getActivePieces(){
        chessPiece[] activeCP = new chessPiece[piecesLeft];
        int index = 0;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(gameBoard[i][j] != null){
                    activeCP[index++] = gameBoard[i][j];
                }
            }
        }
        return activeCP;
    }
    
    public void printPiecePositions(){
        for(chessPiece cp : getActivePieces()){
            cp.printPieceInfo();
        } 
    }
    
    public chessPiece getPieceAt(int row, int col){
        return gameBoard[row-1][col-1];
    }
}
