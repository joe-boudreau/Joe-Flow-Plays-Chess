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
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingConstants;
/**
 *
 * @author jboudrea
 */
public class JoeFlowPlaysChess extends JFrame {
    
    private volatile int screenX = 0;
    private volatile int screenY = 0;
    private volatile int myX = 0;
    private volatile int myY = 0;
    
    private Container pane;
    private JLabel board;
    private JPanel chessBoard;
    private chessPiece[] wPieces;
    private chessPiece[] bPieces;
    private BoardTile[][] boardSquares;
    private BoardTile[] validSquares;
    private chessPiece currPiece;
    
    char[] columns = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    int[] rows = {1, 2, 3, 4, 5, 6, 7, 8};
    
    public JoeFlowPlaysChess(){
        initUI();
       
    }
    
    public void initUI(){
  
        pane = getContentPane();
        
        chessBoard = setUpChessBoard();
        whiteTurnListener();
        pane.add(chessBoard);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000,1000);
        pack();
        pane.validate();
        
        //startGame();
        
        
    }    
    /*
    public void startGame(){
        
        boolean whiteTurn = true;

        while(whiteTurn){
            for(int i = 0; i <  16; i++){
                if(wPieces[i].hasMoved()){
                    int[] newPos = wPieces[i].getNewPosition();
                    if(wPieces[i].isACapture()){ gameState.getPieceAt(newPos[0], newPos[1]).makeDead();}
                    gameState.move(wPieces[i].getPosition(), newPos);
                    wPieces[i].clearFlagsAndUpdatePosition();
                    for(int j = 0; j <  wPieces.length; j++){
                        wPieces[j].updateGameBoard(gameState);
                        bPieces[j].updateGameBoard(gameState);
                    }
                }
            }
        }
        
        
    }*/
    
    public void whiteTurnListener(){
        
        board.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) { }

            @Override
            public void mousePressed(MouseEvent e) {
              screenX = e.getXOnScreen();
              screenY = e.getYOnScreen();

              myX = e.getX();
              myY = e.getY();
              
              int rowIndex = (int)(800-myY)/100;
              int colIndex = (int)(myX)/100;
              
              currPiece = boardSquares[rowIndex][colIndex].getPiece();
              if(currPiece != null){
                myX = currPiece.getX();
                myY = currPiece.getY();
                //setComponentZOrder(currPiece,getComponentCount());
                validSquares = generateValidMoves(currPiece);
                

                for(BoardTile bT : validSquares){
                    bT.lightUp();
                }
                
              }
              
              
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(currPiece != null){
                    int[] newPos = returnNewLocation(currPiece);
                    //System.out.println(newPos[0] + " " +newPos[1]);
                    if(!boardSquares[newPos[0]][newPos[1]].isEmpty() && boardSquares[newPos[0]][newPos[1]].getPiece().getColour() == 0){
                        //invalid move
                        int[] oldPos = getPosition(currPiece);
                        currPiece.setLocation(new Point(100*(oldPos[1]),800 - 100*(oldPos[0])));
                    }
                    
                    
                    
                    for(BoardTile bT : validSquares){
                        bT.lightDown();
                    }
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
                if(currPiece != null){
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
        
        ArrayList<BoardTile> validBoardList = new ArrayList();
        
        switch(cP.getType()){
            
            case "Pawn":
                if(boardSquares[pieceRow+1][pieceCol].isEmpty()){
                    System.out.println(true);
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol]);
                }
                if(!cP.hasMoved() && boardSquares[pieceRow+2][pieceCol].isEmpty()){
                    System.out.println(true);
                    validBoardList.add(boardSquares[pieceRow+2][pieceCol]);
                }
                if(pieceCol<7 && !boardSquares[pieceRow+1][pieceCol+1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol+1].getPiece().getColour() == 1){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol+1]);
                }
                if(pieceCol>0 && !boardSquares[pieceRow+1][pieceCol-1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol-1].getPiece().getColour() == 1){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol-1]);
                }
            
        }
        BoardTile[] vbt = new BoardTile[validBoardList.size()];
        validBoardList.toArray(vbt);
        return vbt;
    }
    
    public int[] getPosition(chessPiece cP){
        for(int r : rows){
            for(char c : columns){
                if(boardSquares[getIndex(r)][getIndex(c)].getPiece().equals(currPiece)){
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
        
        board = new JLabel(new ImageIcon(getClass().getResource("/resources/board.png")));
        board.setBounds(0, 100, 800, 800);
        
        boardSquares = getTiles();
        
        wPieces = getPieces(0);
        bPieces = getPieces(1);
        
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
        
        chessPanel.add(chessBoard,BorderLayout.CENTER);
        
        return chessPanel;
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
        
        cP.setLocation(x,y);
        
        return new int[]{(int)(800-y)/100, (int)(x)/100};
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
    
    public chessPiece[] getPieces(int colour){
        
        chessPiece[] pieces = new chessPiece[16];
        int pieceNum = 0;
        int row = colour == 0 ? 1 : 8;
        int pawnRow = colour == 0 ? 2 : 7;
        
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
    
    public BoardTile[][] getTiles(){
        
        BoardTile[][] bT = new BoardTile[8][8];
        
        for(int row : rows){
            for(char c : columns){
                bT[getIndex(row)][getIndex(c)] = new BoardTile(null, row, c);
                //bT[getIndex(row)][getIndex(c)].setOpaque(true);
            }
        }
        
        return bT;
    }
    
    public static void main(String[] args) {        
        
        EventQueue.invokeLater(new Runnable() {
           
            @Override
            public void run() {
                JoeFlowPlaysChess jfpc = new JoeFlowPlaysChess();
                jfpc.setVisible(true);
            }
        });
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

    
    

}
