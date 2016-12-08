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
    
    private int WHITE = 0;
    private int BLACK = 1;
    
    private volatile int screenX = 0;
    private volatile int screenY = 0;
    private volatile int myX = 0;
    private volatile int myY = 0;
    
    private Container pane;
    private JLabel board;
    private JPanel chessBoard;
    private JPanel infoMsgPanel;
    private JPanel footerPanel;
    private chessPiece[] wPieces;
    private chessPiece[] bPieces;
    private BoardTile[][] boardSquares;
    private BoardTile[] validSquares;
    private chessPiece currPiece;
    private boolean whiteTurn;
    private int[] newPos;
    private int[] oldPos;
    private boolean castleFlag;
    
    char[] columns = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    int[] rows = {1, 2, 3, 4, 5, 6, 7, 8};
    
    Object LOCK = new Object();
    
    
    public JoeFlowPlaysChess(){
        initUI();
       
    }
    
    public void initUI(){
  
        pane = getContentPane();
        
        chessBoard = setUpChessBoard();
        setUpInfoMsgPanel();
        
        pane.add(chessBoard, BorderLayout.CENTER);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800,1000);
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

        while(whiteTurn){
            

        }
        
        
    }
    
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
                if(currPiece != null && currPiece.getColour() == WHITE){
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
        infoMsgPanel.setBounds(0, 0, 800, 100);
        infoMsgPanel.setBorder(new LineBorder(Color.BLACK));
        
        footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
        footerPanel.setBounds(0, 900, 800, 100);
        footerPanel.setBorder(new LineBorder(Color.BLACK));
        
        board = new JLabel(new ImageIcon(getClass().getResource("/resources/board.png")));
        
        board.setBounds(0, 100, 800, 800);
        
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
        
        infoMsgPanel.add(Box.createVerticalStrut(20));
        infoMsgPanel.add(confirmText);
        infoMsgPanel.add(Box.createVerticalGlue());
        infoMsgPanel.add(buttPanel);
        
        
        yConfirm.setVisible(false);
        nConfirm.setVisible(false);
        confirmText.setVisible(false);
        
    }
    
    public void setUpFooterPanel(){
        
        JPanel promotionOptions = new JPanel();
        
        Border blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder title = BorderFactory.createTitledBorder(
                       blackline, "PROMOTE:");
        title.setTitleJustification(TitledBorder.CENTER);
        
        promotionOptions.setBorder(title);
        
        JButton queen = new JButton(new ImageIcon(getClass().getResource("/resources/wqueenSmall.png")));
        
        
        
        
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
            
            if(null != buttonName)switch (buttonName) {
                case "Yes":
                    currPiece.setMoved();
                    boardSquares[newPos[0]][newPos[1]].setPiece(currPiece);
                    boardSquares[oldPos[0]][oldPos[1]].setPiece(null);
                    
                    if(castleFlag){
                        if(getColumn(newPos[1]) == 'g'){
                            boardSquares[getIndex(1)][getIndex('f')].setPiece(boardSquares[getIndex(1)][getIndex('h')].getPiece());
                            boardSquares[getIndex(1)][getIndex('h')].setPiece(null);
                        }
                        if(getColumn(newPos[1]) == 'c'){
                            boardSquares[getIndex(1)][getIndex('d')].setPiece(boardSquares[getIndex(1)][getIndex('a')].getPiece());
                            boardSquares[getIndex(1)][getIndex('a')].setPiece(null);
                        }
                        castleFlag = false;
                    }
                    
                    break;
                    
                case "No":
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
