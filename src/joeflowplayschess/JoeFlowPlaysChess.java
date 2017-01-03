/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
/**
 *
 * @author jboudrea
 */
public class JoeFlowPlaysChess extends JFrame {
    
    //declarations
    private ChessEngine JoeFlow;
    private Container pane;
    private JLabel board;
    private JPanel chessBoard;
    private JPanel infoMsgPanel;
    private JPanel footerPanel;
    private JPanel promotionOptions;
    private chessPiece[] wPieces;
    private chessPiece[] bPieces;
    private BoardTile[][] boardSquares;
    private BoardTile[] validSquares;
    private chessPiece currPiece;
    private boolean whiteTurn;
    private int[] newPos;
    private int[] oldPos;
    private boolean castleFlag;
    
    //declarations + initializations
    private int WHITE = 0;
    private int BLACK = 1;
    
    private int numWTaken = 0;
    private int numBTaken = 0;
    
    private volatile int screenX = 0;
    private volatile int screenY = 0;
    private volatile int myX = 0;
    private volatile int myY = 0;
    
    public int moveFlagPromotedPiece =   0b00001111;
    public int moveFlagPromotion =       0b00010000;
    public int moveFlagEnPassant =       0b00100000;
    public int moveFlagQueenSideCastle = 0b01000000;
    public int moveFlagKingSideCastle =  0b10000000;
    
    private boolean checkmate = false;
    private boolean confirmNeeded = false;
    
    String[] pieceTypes = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King",
                           "Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};

    char[] columns = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    int[] rows = {1, 2, 3, 4, 5, 6, 7, 8};
    
    Object LOCK = new Object();
    
    
    public JoeFlowPlaysChess(){
        initUI();
       
    }
    
    public void initUI(){
  
        pane = getContentPane();
        JoeFlow = new ChessEngine();
        
        chessBoard = setUpChessBoard();
        setUpInfoMsgPanel();
        setUpFooterPanel();
        
        
        pane.add(chessBoard, BorderLayout.CENTER);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        pane.validate();
        
        Runnable game = new Runnable(){
                    @Override
                    public void run(){
                        startGame();
                    }
                };  
        new Thread(game).start(); 
        
    }    
    
    public void startGame(){
        
        whiteTurnListener();
        whiteTurn = true;
        long[] gamePosition = new long[12];
        int flags = 0x0000;
        
        while(!checkmate){

        synchronized(LOCK) {
            while(whiteTurn) {

                try { LOCK.wait(); }
                catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        makeBlackMove();
        whiteTurn = true;
        }
        
    }
    
    public void makeBlackMove(){
        
        int[] blackMove = JoeFlow.selectMove(BLACK);
        // piece(4) | capturedPiece{4} | fromSq(8) | toSq(8) | flags(8)
        /* flags : bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
                   bit 5: promotion flag
                   bit 6: en-passant capture flag
                   bit 7: Queen Side Capture
                   bit 8: King Side Capture
        */
        
        int capturedPiece = blackMove[1];
        int fromSq = blackMove[2];
            int fromRow = (int)(fromSq-fromSq%8)/8;
            int fromCol = fromSq%8;
        int toSq = blackMove[3];
            int toRow = (int)(toSq-toSq%8)/8;
            int toCol = toSq%8;
        int flags = blackMove[4];
        
        chessPiece pieceToMove = boardSquares[fromRow][fromCol].getPiece();
        boardSquares[fromRow][fromCol].setPiece(null);
        
        if((flags & moveFlagPromotion) != 0){
            
            int newPiece = flags & moveFlagPromotedPiece;
            pieceToMove.setType(pieceTypes[newPiece]);
        }
        
        else if((flags & moveFlagKingSideCastle) != 0){
            
            chessPiece rook = boardSquares[getIndex(8)][getIndex('h')].getPiece();
            boardSquares[getIndex(8)][getIndex('f')].setPiece(rook);
            boardSquares[getIndex(8)][getIndex('h')].setPiece(null);
            rook.setLocation(8, 'f');
        }
        
        else if((flags & moveFlagQueenSideCastle) != 0){
            
            chessPiece rook = boardSquares[getIndex(8)][getIndex('a')].getPiece();
            boardSquares[getIndex(8)][getIndex('d')].setPiece(rook);
            boardSquares[getIndex(8)][getIndex('a')].setPiece(null);
            rook.setLocation(8, 'd');
        }
        else if((flags & moveFlagEnPassant) != 0){
            
            chessPiece deadPiece = boardSquares[toRow+1][toCol].getPiece();
            addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
            deadPiece.setVisible(false);
            boardSquares[toRow+1][toCol].setPiece(null);
        }
        
        if(capturedPiece != 0xE){
            
            chessPiece deadPiece = boardSquares[toRow][toCol].getPiece();
            addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
            deadPiece.setVisible(false);
        }
        
        boardSquares[toRow][toCol].setPiece(pieceToMove);
        pieceToMove.setLocation(getRow(toRow), getColumn(toCol));
        
        
    }
    
    public void whiteTurnListener(){
        
        board.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) { }

            @Override
            public void mousePressed(MouseEvent e) {
              
                if(!confirmNeeded){  
                    screenX = e.getXOnScreen();
                    screenY = e.getYOnScreen();

                    myX = e.getX();
                    myY = e.getY();

                    int rowIndex = (int)(800-myY)/100;
                    int colIndex = (int)(myX)/100;

                    currPiece = boardSquares[rowIndex][colIndex].getPiece();
                    if(currPiece != null && currPiece.getColour() == WHITE){

                      myX = currPiece.getX();
                      myY = currPiece.getY();
                      currPiece.printInfo();
                      validSquares = generateValidMoves(currPiece);

                      /*
                      for(BoardTile bT : validSquares){
                          bT.lightUp();
                          //chessBoard.repaint();
                      }
                      */

                    }

                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(currPiece != null  && currPiece.getColour() == WHITE){
                    newPos = returnNewLocation(currPiece);
                    oldPos = getPosition(currPiece);
                    BoardTile newSquare = boardSquares[newPos[0]][newPos[1]];
                    boolean valid = false;
                    for(BoardTile sq : validSquares){
                        if(sq.equals(newSquare)){
                            valid = true;
                        }
                    }
                    if(valid){
                        
                        if(!newSquare.isEmpty()){
                            boardSquares[newPos[0]][newPos[1]].getPiece().setVisible(false);
                        }
                        
                        if(currPiece.getType().equals("King")){
                            
                            if(oldPos[1] == getIndex('e') && newPos[1] == getIndex('g')){
                                boardSquares[getIndex(1)][getIndex('h')].getPiece().setLocation(1, 'f');
                                castleFlag = true;
                            }
                            else if(oldPos[1] == getIndex('e') && newPos[1] == getIndex('c')){
                                boardSquares[getIndex(1)][getIndex('a')].getPiece().setLocation(1, 'd');
                                castleFlag = true;
                            }
                        }
                        
                        
                        getComponentInContainer(infoMsgPanel, "Yes").setVisible(true);
                        getComponentInContainer(infoMsgPanel, "No").setVisible(true);
                        getComponentInContainer(infoMsgPanel, "confirmText").setVisible(true);
                        confirmNeeded = true;
                        
                    }
                    else{
                        
                        currPiece.setLocation(getRow(oldPos[0]), getColumn(oldPos[1]));
                        
                    }
                    /*
                    for(BoardTile bT : validSquares){
                        bT.lightDown();
                    }
                    */
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) { }

            @Override
            public void mouseExited(MouseEvent e) { }


        });
        
        board.addMouseMotionListener(new MouseMotionListener(){
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if(!confirmNeeded && currPiece != null && currPiece.getColour() == WHITE){
                    int deltaX = e.getXOnScreen() - screenX;
                    int deltaY = e.getYOnScreen() - screenY;

                    currPiece.setLocation(myX + deltaX, myY + deltaY);  
                }    
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {}
        });
    }
    
    public BoardTile[] generateValidMoves(chessPiece cP){
        
        int[] piecePos = getPosition(cP);
        int pieceRow = piecePos[0];
        int pieceCol = piecePos[1];
        
        int nextRow, nextCol, prevRow, prevCol;
        int[] rowDelta, colDelta;
        
        if(pieceIsPinned(cP)){
            return new BoardTile[]{};
        }
        
        ArrayList<BoardTile> validBoardList = new ArrayList();
        
        switch(cP.getType()){
            
            case "Pawn":
                if(boardSquares[pieceRow+1][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol]);
                }
                if(!cP.hasMoved() && boardSquares[pieceRow+2][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow+2][pieceCol]);
                }
                if(pieceCol<7 && !boardSquares[pieceRow+1][pieceCol+1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol+1].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol+1]);
                }
                if(pieceCol>0 && !boardSquares[pieceRow+1][pieceCol-1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol-1].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol-1]);
                }
            break;
                
            case "Rook":
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][pieceCol]);
                }
                if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][pieceCol]);
                }
                
                while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][nextCol++]);
                }
                if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][nextCol]);
                }
                
                while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][pieceCol]);
                }
                if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][pieceCol]);
                }
                
                while(prevCol > -1 && boardSquares[pieceRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][prevCol--]);
                }
                if(prevCol > -1 && boardSquares[pieceRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][prevCol]);
                }
            break;
                
            case "Knight":
                rowDelta = new int[]{-2, -2, -1, -1, 1, 1, 2, 2};
                colDelta = new int[]{-1, 1, -2, 2, -2, 2, -1, 1};
                
                for(int j = 0; j < 8; j++){
                    if(pieceRow+rowDelta[j] <= 7 &&
                       pieceRow+rowDelta[j] >= 0 &&
                       pieceCol+colDelta[j] <= 7 &&
                       pieceCol+colDelta[j] >= 0){
                        
                        if(boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]].isEmpty() ||
                           boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]].getPiece().getColour() == BLACK){
                            validBoardList.add(boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]]);
                        }
                        
                    }
                }
            break;
            
            case "Bishop":
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][nextCol++]);
                }
                if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][nextCol]);
                }
                
                while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][prevCol--]);
                }
                if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][prevCol]);
                }
                
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][prevCol--]);
                }
                if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][prevCol]);
                }
                
                while(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][nextCol++]);
                }
                if(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][nextCol]);
                }
            break;
            
            case "Queen":
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][pieceCol]);
                }
                if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][pieceCol]);
                }
                
                while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][nextCol++]);
                }
                if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][nextCol]);
                }
                
                while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][pieceCol]);
                }
                if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][pieceCol]);
                }
                
                while(prevCol > -1 && boardSquares[pieceRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][prevCol--]);
                }
                if(prevCol > -1 && boardSquares[pieceRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][prevCol]);
                }
                
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][nextCol++]);
                }
                if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][nextCol]);
                }
                
                while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][prevCol--]);
                }
                if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][prevCol]);
                }
                
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][prevCol--]);
                }
                if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][prevCol]);
                }
                
                while(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][nextCol++]);
                }
                if(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][nextCol]);
                }
                
            break;
            
            case "King":
                rowDelta = new int[]{-1, -1, -1, 0, 0, 1, 1, 1};
                colDelta = new int[]{-1, 0, 1, -1, 1, -1, 0, 1};
                
                for(int j = 0; j < 8; j++){
                    if(pieceRow+rowDelta[j] <= 7 &&
                       pieceRow+rowDelta[j] >= 0 &&
                       pieceCol+colDelta[j] <= 7 &&
                       pieceCol+colDelta[j] >= 0){
                        
                        if(boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]].isEmpty() ||
                           boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]].getPiece().getColour() == BLACK){
                            validBoardList.add(boardSquares[pieceRow+rowDelta[j]][pieceCol+colDelta[j]]);
                        }
                        
                    }
                }
                if(!cP.hasMoved() && boardSquares[getIndex(1)][getIndex('a')].getPiece().getType().equals("Rook") &&
                                     !boardSquares[getIndex(1)][getIndex('a')].getPiece().hasMoved() &&
                                     boardSquares[getIndex(1)][getIndex('b')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('c')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('d')].isEmpty()){
                    validBoardList.add(boardSquares[getIndex(1)][getIndex('c')]);
                }
                if(!cP.hasMoved() && boardSquares[getIndex(1)][getIndex('h')].getPiece().getType().equals("Rook") &&
                                     !boardSquares[getIndex(1)][getIndex('h')].getPiece().hasMoved() &&
                                     boardSquares[getIndex(1)][getIndex('f')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('g')].isEmpty()){
                    validBoardList.add(boardSquares[getIndex(1)][getIndex('g')]);
                }
                
            break;
            
        }

        BoardTile[] vbt = new BoardTile[validBoardList.size()];
        validBoardList.toArray(vbt);
        return vbt;
    }
    
    public boolean pieceIsPinned(chessPiece cP){
        
        chessPiece[] blackPieces = getPiecesOnBoard(BLACK);

        int nextRow, nextCol, prevRow, prevCol;
        
        for (chessPiece bP : blackPieces){
            
            int[] piecePos = getPosition(bP);
            int pieceRow = piecePos[0];
            int pieceCol = piecePos[1];
            
            if(bP.getType().equals("Rook") || bP.getType().equals("Queen")){
                
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                    nextRow++;
                }
                if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().equals(cP)){
                    nextRow++;
                    while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                        nextRow++;
                    }
                    if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().getType().equals("King") &&
                                      boardSquares[nextRow][pieceCol].getPiece().getColour() == WHITE) {
                        return true;
                    }
                }
                
                while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                    nextCol++;
                }
                if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().equals(cP)){
                    nextCol++;
                    while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                        nextCol++;
                    }
                    if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().getType().equals("King") &&
                                      boardSquares[pieceRow][nextCol].getPiece().getColour() == WHITE) {
                        return true;
                    }
                }
                
                while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                    prevRow--;
                }
                if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().equals(cP)){
                    prevRow--;
                    while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                        prevRow--;
                    }
                    if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().getType().equals("King") &&
                                      boardSquares[prevRow][pieceCol].getPiece().getColour() == WHITE) {
                        return true;
                    }
                }
                
                while(prevCol > -1 && boardSquares[pieceRow][prevCol].isEmpty()){
                    prevCol--;
                }
                if(prevCol > -1 && boardSquares[pieceRow][prevCol].getPiece().equals(cP)){
                    prevCol--;
                    while(prevCol > -1 && boardSquares[pieceRow][prevCol].isEmpty()){
                        prevCol--;
                    }
                    if(prevCol > -1 && boardSquares[pieceRow][prevCol].getPiece().getType().equals("King") &&
                                      boardSquares[pieceRow][prevCol].getPiece().getColour() == WHITE) {
                        return true;
                    }
                }
                
            }
                
            if(bP.getType().equals("Bishop") || bP.getType().equals("Queen")){ 
                
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                                
                while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                    nextRow++; nextCol++;
                }
                if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().equals(cP)){
                    nextRow++; nextCol++;
                    while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                        nextRow++; nextCol++;
                    }
                    if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().getType().equals("King") &&
                                                     boardSquares[nextRow][nextCol].getPiece().getColour() == WHITE){
                        return true;
                    }
                }
                
                while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                    prevRow--; prevCol--;
                }
                if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().equals(cP)){
                    prevRow--; prevCol--;

                    while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                        prevRow--; prevCol--;
                    }
                    if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().getType().equals("King") &&
                                                     boardSquares[prevRow][prevCol].getPiece().getColour() == WHITE){
                        return true;
                    }
                }

                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                    nextRow++; prevCol--;
                }
                if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().equals(cP)){
                    nextRow++; prevCol--;
                    while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                        nextRow++; prevCol--;
                    }
                    if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().getType().equals("King") &&
                                                     boardSquares[nextRow][prevCol].getPiece().getColour() == WHITE){
                        return true;
                    }
                }
                
                while(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].isEmpty()){
                    prevRow--; nextCol++;
                }
                if(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].getPiece().equals(cP)){
                    prevRow--; nextCol++;
                    while(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].isEmpty()){
                        prevRow--; nextCol++;
                    }
                    if(prevRow > -1 && nextCol < 8 && boardSquares[prevRow][nextCol].getPiece().getType().equals("King") &&
                                                     boardSquares[prevRow][nextCol].getPiece().getColour() == WHITE){
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public int[] getPosition(chessPiece cP){
        for(int r : rows){
            for(char c : columns){
                if(!boardSquares[getIndex(r)][getIndex(c)].isEmpty() &&
                    boardSquares[getIndex(r)][getIndex(c)].getPiece().equals(cP)){
                        return new int[]{getIndex(r),getIndex(c)};
                }
            }
        }
        return new int[]{0,0};
    }
    
    public JPanel setUpChessBoard(){
        
        JPanel  chessPanel = new JPanel ();
        
        chessPanel.setLayout(new BorderLayout());
        
        JLayeredPane chessBoard = new JLayeredPane();
        
        chessBoard.setBorder(new LineBorder(Color.BLACK, 1));
        chessBoard.setPreferredSize(new Dimension(800, 1000));
        
        infoMsgPanel = new JPanel();
        infoMsgPanel.setLayout(new BoxLayout(infoMsgPanel, BoxLayout.Y_AXIS));
        infoMsgPanel.setBounds(0, 0, 800, 80);
        infoMsgPanel.setBorder(new LineBorder(Color.BLACK));
        
        footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        //footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
        footerPanel.setBounds(0, 880, 800, 120);
        
        board = new JLabel(new ImageIcon(getClass().getResource("/resources/board.png")));
        
        board.setBounds(0, 80, 800, 800);
        
        boardSquares = setUpTiles();
        
        wPieces = setUpPieces(0);
        bPieces = setUpPieces(1);
        
        chessBoard.add(infoMsgPanel,3);
        chessBoard.add(footerPanel, 3);
        
        chessBoard.add(board, 2);
        
        for(int row : rows){
            for(char col : columns){
                chessBoard.add(boardSquares[getIndex(row)][getIndex(col)],1);                
            }
        }

        for (chessPiece cP : wPieces){
            chessBoard.add(cP, 0);
        }
        
        for (chessPiece cP : bPieces){
            chessBoard.add(cP, 0);
        }
        
        promotionOptions = new JPanel();
        promotionOptions.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        promotionOptions.setBackground(Color.WHITE);
        
        Border blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder title = BorderFactory.createTitledBorder(
                       blackline, "PROMOTE:");
        title.setTitleJustification(TitledBorder.CENTER);
        title.setTitleFont(new Font("Arial", Font.BOLD, 25));
        
        promotionOptions.setBorder(title);
        promotionOptions.setAlignmentX(CENTER_ALIGNMENT);
        promotionOptions.setBounds(310, 450, 180, 75);
        
        JButton queen = new JButton();
        queen.setName("queen");
        JButton knight = new JButton();
        knight.setName("knight");
        JButton bishop = new JButton();
        bishop.setName("bishop");
        JButton rook = new JButton();
        rook.setName("rook");
        
        ButtonAction BA = new ButtonAction();
        
        queen.addActionListener(BA);
        knight.addActionListener(BA);
        bishop.addActionListener(BA);
        rook.addActionListener(BA);
        
        
        
        makeCustomButton(queen, "/resources/wqueenSmall.png", "/resources/wqueenSmallPressed.png");
        makeCustomButton(knight, "/resources/wknightSmall.png", "/resources/wknightSmallPressed.png");
        makeCustomButton(bishop, "/resources/wbishopSmall.png", "/resources/wbishopSmallPressed.png");
        makeCustomButton(rook, "/resources/wrookSmall.png", "/resources/wrookSmallPressed.png");
        
        promotionOptions.add(queen);
        promotionOptions.add(knight);
        promotionOptions.add(bishop);
        promotionOptions.add(rook);
        
        promotionOptions.setVisible(false);
        
        chessBoard.add(promotionOptions, 0);
        
        chessPanel.add(chessBoard,BorderLayout.CENTER);
        
        return chessPanel;
    }
    
    public void setUpInfoMsgPanel(){
        
        JButton yConfirm = new JButton("Yes");
        yConfirm.setName("Yes");
        yConfirm.addActionListener(new ButtonAction());
        
        JButton nConfirm = new JButton("No");
        nConfirm.setName("No");
        nConfirm.addActionListener(new ButtonAction());
        
        JPanel buttPanel = new JPanel();
        buttPanel.setLayout(new BoxLayout(buttPanel, BoxLayout.X_AXIS));
        
        buttPanel.add(Box.createHorizontalStrut(150));
        buttPanel.add(yConfirm);
        buttPanel.add(Box.createHorizontalGlue());
        buttPanel.add(nConfirm);
        buttPanel.add(Box.createHorizontalStrut(150));
        
        JLabel confirmText = new JLabel("Are you sure?");
        confirmText.setName("confirmText");
        confirmText.setAlignmentX(CENTER_ALIGNMENT);
        confirmText.setFont(new Font("Arial", Font.BOLD, 25));
        
        infoMsgPanel.add(Box.createVerticalStrut(5));
        infoMsgPanel.add(confirmText);
        infoMsgPanel.add(Box.createVerticalGlue());
        infoMsgPanel.add(buttPanel);
        
        
        yConfirm.setVisible(false);
        nConfirm.setVisible(false);
        confirmText.setVisible(false);
        
    }
    
    public void setUpFooterPanel(){
        
        JPanel whiteTaken = new JPanel();
        whiteTaken.setBackground(Color.WHITE);
        whiteTaken.setAlignmentY(LEFT_ALIGNMENT);
        
        JPanel blackTaken = new JPanel();
        blackTaken.setBackground(Color.WHITE);
        blackTaken.setAlignmentY(LEFT_ALIGNMENT);

        JLabel[] whitePiecesTaken = new JLabel[15];
        JLabel[] blackPiecesTaken = new JLabel[15];
        
        for(int i = 0; i < 15; i++){
            whitePiecesTaken[i] = new JLabel(new ImageIcon(getClass().getResource("/resources/blank.png")));
            blackPiecesTaken[i] = new JLabel(new ImageIcon(getClass().getResource("/resources/blank.png")));
            
            whiteTaken.add(whitePiecesTaken[i]);
            blackTaken.add(blackPiecesTaken[i]);
        }
        
        whiteTaken.setBorder(new LineBorder(Color.BLACK));
        blackTaken.setBorder(new LineBorder(Color.BLACK));

        footerPanel.add(whiteTaken);
        
        footerPanel.add(blackTaken);
        
    }
    
    public int[] returnNewLocation(chessPiece cP){
        
        int x = cP.getLocation().x;
        int y = cP.getLocation().y;
        
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
        int colStartY = 780;
        
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
        
        cP.setLocation(x,y);
        return new int[]{(int)(780-y)/100, (int)(x)/100};
        /*
        newRow = (int)(800-y)/100 + 1;
        newCol = (int)(x)/100 + 1;
        
        String move = moveType(colour, type, newRow, newCol, r, c);
        
        if(move.equals("INVALID")){
            movePiece(r, c);
        }
        else{
            System.out.println("VALID");
            hasMoved = true;
            movePiece(newRow, newCol);
            isACapture = move.equals("CAPTURE") ? true : false;   
            }
        */
    }
    
    public chessPiece[] setUpPieces(int colour){
        
        chessPiece[] pieces = new chessPiece[16];
        int pieceNum = 0;
        int row = colour == WHITE ? 1 : 8;
        int pawnRow = colour == WHITE ? 2 : 7;
        
        pieces[pieceNum++]  = new chessPiece("Rook", colour, row, 'a');
        boardSquares[getIndex(row)][getIndex('a')].setPiece(pieces[pieceNum-1]);
                
        pieces[pieceNum++]  = new chessPiece("Knight", colour, row, 'b');
        boardSquares[getIndex(row)][getIndex('b')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("Bishop", colour, row, 'c');
        boardSquares[getIndex(row)][getIndex('c')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("Queen", colour, row, 'd');
        boardSquares[getIndex(row)][getIndex('d')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("King", colour, row, 'e');
        boardSquares[getIndex(row)][getIndex('e')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("Bishop", colour, row, 'f');
        boardSquares[getIndex(row)][getIndex('f')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("Knight", colour, row, 'g');
        boardSquares[getIndex(row)][getIndex('g')].setPiece(pieces[pieceNum-1]);
        
        pieces[pieceNum++]  = new chessPiece("Rook", colour, row, 'h');
        boardSquares[getIndex(row)][getIndex('h')].setPiece(pieces[pieceNum-1]);
        
        char pawnCol = 'a';
        while(pieceNum < 16){
            
            pieces[pieceNum++]  = new chessPiece("Pawn", colour, pawnRow, pawnCol);
            boardSquares[getIndex(pawnRow)][getIndex(pawnCol)].setPiece(pieces[pieceNum-1]);
            
            pawnCol = (char) ((int) pawnCol + 1);
        }
        
        return pieces;

    }
    
    public chessPiece[] getPiecesOnBoard(int colour){
        ArrayList<chessPiece> piecesLst = new ArrayList();
        
        BoardTile bt;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                bt = boardSquares[i][j];
                if(!bt.isEmpty() && bt.getPiece().getColour() == colour){
                    piecesLst.add(bt.getPiece());
                }
            }
        }
        
        chessPiece[] pieces = new chessPiece[piecesLst.size()];
        piecesLst.toArray(pieces);
        return pieces;
    }
    
    public BoardTile[][] setUpTiles(){
        
        BoardTile[][] bT = new BoardTile[8][8];
        
        for(int row : rows){
            for(char c : columns){
                bT[getIndex(row)][getIndex(c)] = new BoardTile(null, row, c);
            }
        }
        
        return bT;
    }
    
    public void addToTakenPieces(int colour, String type){
    
        JPanel takenPanel;
        JLabel newDeadPiece;
        if(colour == WHITE){
            
            takenPanel = (JPanel) footerPanel.getComponent(0);
            newDeadPiece = (JLabel) takenPanel.getComponent(numWTaken);
            newDeadPiece.setIcon(new ImageIcon(getClass().getResource("/resources/w" + type + "small.png")));
            numWTaken++;
        }
        else{
            
            takenPanel = (JPanel) footerPanel.getComponent(1);
            newDeadPiece = (JLabel) takenPanel.getComponent(numBTaken);
            newDeadPiece.setIcon(new ImageIcon(getClass().getResource("/resources/b" + type + "small.png")));
            numBTaken++;
        }
    }
    
    public static int getIndex(int row){
        return row - 1;
    }
    
    public static int getIndex(char col){
        return ((int) col) - 97;
    }
    
    public static int getRow(int rowIndex){
        return rowIndex + 1;
    }
    
    public static char getColumn(int columnIndex){
        return (char) (columnIndex + 97);
    }
    
    public class ButtonAction extends AbstractAction{
        
        
        @Override
        public void actionPerformed(ActionEvent e){

            JButton buttonPressed = (JButton) e.getSource();
            String buttonName = buttonPressed.getName();   //Get the name of the button pressed
            int moveFlags = 0;
            
            if(null != buttonName)switch (buttonName) {
                case "Yes":
                    currPiece.setMoved();
                    confirmNeeded = false;
                    
                    if(!boardSquares[newPos[0]][newPos[1]].isEmpty()){
                        
                        chessPiece deadPiece = boardSquares[newPos[0]][newPos[1]].getPiece();
                        addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
                        
                    }
                    
                    boardSquares[newPos[0]][newPos[1]].setPiece(currPiece);
                    boardSquares[oldPos[0]][oldPos[1]].setPiece(null);
                    
                    if(castleFlag){
                        if(getColumn(newPos[1]) == 'g'){
                            boardSquares[getIndex(1)][getIndex('f')].setPiece(boardSquares[getIndex(1)][getIndex('h')].getPiece());
                            boardSquares[getIndex(1)][getIndex('h')].setPiece(null);
                            moveFlags = moveFlagKingSideCastle;
                        }
                        if(getColumn(newPos[1]) == 'c'){
                            boardSquares[getIndex(1)][getIndex('d')].setPiece(boardSquares[getIndex(1)][getIndex('a')].getPiece());
                            boardSquares[getIndex(1)][getIndex('a')].setPiece(null);
                            moveFlags = moveFlagQueenSideCastle;
                        }
                        castleFlag = false;
                    }
                    
                    if(currPiece.getType().equals("Pawn") && newPos[0] == getIndex(8)){
                            promotionOptions.setVisible(true);
                            confirmNeeded = true;
                            
                            break;
                    }
                    else{
                        JoeFlow.makeMove(oldPos, newPos, moveFlags);
                        
                        whiteTurn = false;
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }

                        break;
                    }
                    
                    
                    
                case "No":
                    confirmNeeded = false;
                    
                    if(!boardSquares[newPos[0]][newPos[1]].isEmpty()){
                        boardSquares[newPos[0]][newPos[1]].getPiece().setVisible(true);
                    }
                    
                    if(castleFlag){
                        if(getColumn(newPos[1]) == 'g'){
                            boardSquares[getIndex(1)][getIndex('h')].getPiece().setLocation(1, 'h');
                        }
                        if(getColumn(newPos[1]) == 'c'){
                             boardSquares[getIndex(1)][getIndex('a')].getPiece().setLocation(1, 'a');
                        }  
                        castleFlag = false;
                    }
                    
                    currPiece.setLocation(getRow(oldPos[0]), getColumn(oldPos[1]));
                    break;
                    
                case "queen":
                    currPiece.setType("Queen");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 4;
                    
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }

                    break;
                    
                case "knight":
                    currPiece.setType("Knight");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 2;
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                case "bishop":
                    currPiece.setType("Bishop");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 3;
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                case "rook":
                    currPiece.setType("Rook");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 1;
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                default:
                    break;
            }
            
            getComponentInContainer(infoMsgPanel, "Yes").setVisible(false);
            getComponentInContainer(infoMsgPanel, "No").setVisible(false);
            getComponentInContainer(infoMsgPanel, "confirmText").setVisible(false);
        }
    }
    
    public static JComponent getComponentInContainer(Container c, String name){
        int num = c.getComponentCount();

        Component jC = null;
        JComponent returnedComp = null;
        for(int i = 0; i < num; i++){
            jC = c.getComponent(i);
            
            if(c.getClass().isInstance(jC)){
                returnedComp = getComponentInContainer((Container)jC, name);
            }
            else if(jC.getName() != null && jC.getName().equals(name)){
                returnedComp = (JComponent) jC;
                break;
            }
        }
        return returnedComp;
    }
    
    public void makeCustomButton(JButton butt, String unpressed, String pressed){
        butt.setIcon(new ImageIcon(getClass().getResource(unpressed)));
        butt.setPressedIcon(new ImageIcon(getClass().getResource(pressed)));
        butt.setDisabledIcon(new ImageIcon(getClass().getResource(unpressed)));
        butt.setOpaque(false);              //let unpainted areas of button show
                                            //the image below it
        butt.setContentAreaFilled(false);   //do not paint the entire JButton background
        butt.setBorderPainted(false);
        butt.setFocusPainted(false);
        butt.setMargin(new Insets(0, 0, 0, 0));
        
    }
    
    public static void main(String[] args) {        
        
        SwingUtilities.invokeLater(new Runnable() {
           
            @Override
            public void run() {
                JoeFlowPlaysChess jfpc = new JoeFlowPlaysChess();
                jfpc.setVisible(true);
            }
        });
    }
    
}
