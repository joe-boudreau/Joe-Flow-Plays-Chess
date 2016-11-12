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
import java.awt.event.MouseListener;
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
    
    private Container pane;
    private JLabel mousep = new JLabel("0,0");
    private GameBoard gameState;
    private JPanel chessBoard;
    private chessPiece[] wPieces;
    private chessPiece[] bPieces;
    
    public JoeFlowPlaysChess(){
        initUI();
       
    }
    
    public void initUI(){
  
        pane = getContentPane();
        
        chessBoard = setUpChessBoard();
        pane.add(chessBoard);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000,1000);
        pack();
        pane.validate();
        
        //startGame();
        
        
    }    
    
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
        
        
    }
    
    public JPanel setUpChessBoard(){
        JPanel  chessPanel = new JPanel ();
        chessPanel.setLayout(new BorderLayout());
 
        JLayeredPane chessBoard = new JLayeredPane();
        chessBoard.setBorder(new LineBorder(Color.BLACK, 1));
        chessBoard.setPreferredSize(new Dimension(800, 1000));
        
        
        JLabel board = new JLabel(new ImageIcon(getClass().getResource("/resources/board.png")));
        board.setBounds(0, 100, 800, 800);
        
        /*
        For testing right now, can remove later
        */
        board.addMouseMotionListener(new MouseMotionListener(){
            
            @Override
            public void mouseDragged(MouseEvent e) { }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                
                mousep.setText(e.getX() + ", " + (e.getY()));
            }
        });
        
        wPieces = getPieces("w");
        bPieces = getPieces("b");
        
        chessBoard.add(board, 1);
        
        for (chessPiece cP : wPieces){
            chessBoard.add(cP, 0);
        }
        
        for (chessPiece cP : bPieces){
            chessBoard.add(cP, 0);
        }
        
        chessPanel.add(chessBoard,BorderLayout.CENTER);
        chessPanel.add(mousep, BorderLayout.SOUTH);
        
        gameState = new GameBoard(wPieces, bPieces);
        
        for(int j = 0; j <  16; j++){
            wPieces[j].updateGameBoard(gameState);
            bPieces[j].updateGameBoard(gameState);
        }
        
        gameState.printPiecePositions();
        
        return chessPanel;
    }
    
    public chessPiece[] getPieces(String colour){
        
        chessPiece[] pieces = new chessPiece[16];
        int pieceNum = 0;
        int yRow1, yRow2;
        int colourNum;
        if(colour.equals("w")){ 
            yRow1 = 1;
            yRow2 = 4;
            colourNum = 0;
            }
        else{
            yRow1 = 8;
            yRow2 = 7;
            colourNum = 1;
        }
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 1, "Rook", colourNum);

        pieces[pieceNum++]  = new chessPiece(yRow1, 2, "Knight", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 3, "Bishop", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 4, "Queen", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 5, "King", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 6, "Bishop", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 7, "Knight", colourNum);
        
        pieces[pieceNum++]  = new chessPiece(yRow1, 8, "Rook", colourNum);
        
        int xCol = 1;
        while(pieceNum < 16){
            
            pieces[pieceNum++]  = new chessPiece(yRow2, xCol++, "Pawn", colourNum);
        }
        
        return pieces;

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

}
