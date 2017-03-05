/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

/*
|   Joe Flow Plays Chess
|   This package contains chess engine along with an interface to play against
|   the engine, named "Joe Flow". Developed over the course of November 2016 to
|   March 2017. Features a game board with drag-and-drop pieces, move legality
|   checking for both user and computer, and a visual piece capture history table.
|   User always plays as white, and therefore always has first move.
|   
|   The main java file, JoeFlowPlaysChess.java, handles all the user interactions
|   and GUI updating. It also does the user-side move legality checking, with a
|   little help from some methods available from the engine class instance. In 
|   retrospect, I should built all the game decisions into the engine class, 
|   including the user move legality checking. This could be something I could
|   fix in the future as right now there is some duplicated game logic between the
|   two java classes.
|   
|   The other main file, ChessEngine.java, is the engine which decides the moves
|   for black. The architecture is based on bit-board game representation, which
|   allows for rapid computations of game information and efficient memory usage.
|   The move generation uses fairly common techniques, including magic-bitboards
|   for the move generation of sliding pieces (rooks, bishops, queen). Move
|   selection is made using a recursive search through the move tree with alpha
|   beta pruning techniques. The board evaluation is a minimax score based 
|   function which uses a material score calculation as the primary factor for
|   board advantage, with heuristic evaluation factors as well if there is no
|   material advantage found. Factors such as pawn structure, centre control,
|   piece advancement, and piece mobility are looked at.
|   
|   The main goal of this project was to make a chess engine smart enough to beat
|   its creator. This goal was realized.   
|   
|   Version: 1.0
|   Date: 04/03/17
|   
*/


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
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
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
    private ChessEngine     JoeFlow;
    private Container       pane;
    private JLabel          board;
    private JPanel          chessBoard;
    private JPanel          infoMsgPanel;
    private JPanel          footerPanel;
    private JPanel          promotionOptions;
    private chessPiece[]    wPieces;
    private chessPiece[]    bPieces;
    private BoardTile[][]   boardSquares;
    private BoardTile[]     validSquares;
    private chessPiece      currPiece;
    private boolean         whiteTurn;
    private int[]           newPos;
    private int[]           oldPos;
    private boolean         castleFlag;
    
    //declarations + initializations
    private int WHITE =                  0;
    private int BLACK =                  1;
    
    private int numWTaken =              0;
    private int numBTaken =              0;
    
    private volatile int screenX =       0;
    private volatile int screenY =       0;
    private volatile int myX =           0;
    private volatile int myY =           0;
    
    public int moveFlagPromotedPiece =   0b00001111;
    public int moveFlagPromotion =       0b00010000;
    public int moveFlagEnPassant =       0b00100000;
    public int moveFlagQueenSideCastle = 0b01000000;
    public int moveFlagKingSideCastle =  0b10000000;
    
    public boolean enPassantFlag =       false;
    public int enPassantColumn =         0;
    
    private boolean whiteCheckmate =     false;
    private boolean blackCheckmate =     false;    
    private boolean draw =               false;
    private boolean confirmNeeded =      false;
    
    String[] pieceTypes = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King",
                           "Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};

    char[] columns =      {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    int[] rows =          {1, 2, 3, 4, 5, 6, 7, 8};
    
    Object LOCK = new Object();
    
    
    public JoeFlowPlaysChess(){
        initUI();
    }
    
    /**
     * Initializing function for GUI and game
     * 
     * Calls an instance of the ChessEngine class, sets up the chessboard and the
     * associated pieces. Adds the chessboard to the main content pane. Starts a 
     * new thread to handle the game flow instructions.
     * 
    */
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
    
    /**
     * Main game controller. Alternates white and black turns and checks for
     * checkmate or draw after each move
     * 
     */
    public void startGame(){
        
        whiteTurnListener();    //enable event listeners and associated action logic
        whiteTurn = true;
        
        while(!(whiteCheckmate | blackCheckmate | draw)){

        synchronized(LOCK) {
            while(whiteTurn) {

                try { LOCK.wait(); }    //LOCK is notified when a move is confirmed by user
                catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        //clear en Passant flag for white after every move
        enPassantFlag = false;
        
        makeBlackMove();
        whiteTurn = true;
        
        }
        
        if(whiteCheckmate){
            JOptionPane.showMessageDialog(this, "YOU WIN!");
        }
        else if(blackCheckmate){
            JOptionPane.showMessageDialog(this, "JOE FLOW WINS! LONG LIVE OUR ROBOT OVERLORDS");
    
        }
        else{
            JOptionPane.showMessageDialog(this, "YOU TIED THE COMPUTER! THE SINGULARITY HAS BEEN REACHED!");

        }
    }
    
    /**
     * Calls the selectMove method of the chess Engine, which chooses black's next
     * move and returns the move as a 32-bit encoded int. The move encoding is as
     * follows:
     * 
     * move (MSB --> LSB):
     * pieceMoving (4) | capturedPiece(4) | fromSq(8) | toSq(8) | flags(8)
     * 
     * flags (MSB --> LSB):
     * King Side Castle (1) | Queen Side Castle (1) | en-passant Capture (1) | promotion flag (1) | promoted piece (4)
     * 
     * The returned move is then parsed and the game information and GUI are
     * updated accordingly
     * 
     * 
     */
    public void makeBlackMove(){
        
        int[] blackMove = JoeFlow.selectMove(BLACK);
        // piece(4) | capturedPiece{4} | fromSq(8) | toSq(8) | flags(8)
        /* flags : bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
                   bit 5: promotion flag
                   bit 6: en-passant capture flag
                   bit 7: Queen Side Castle
                   bit 8: King Side Castle
        */
        
        if(blackMove[0] == -1){
            //White checkmate or stalemate
            if(blackMove[1] < 0){
                whiteCheckmate = true;
            }
            else{
                draw = true;
            }
            return; //Black has no move; exit function
        }
        
        if(blackMove.length > 5){
            //Black checkmate or stalemate
            if(blackMove[5] == 0){
                //black wins
                blackCheckmate = true;
            }
            else{
                draw = true;
            }
        }
        
        
        int capturedPiece = blackMove[1];
        int fromSq = blackMove[2];
            int fromRow = (int)(fromSq-fromSq%8)/8;
            int fromCol = fromSq%8;
        int toSq = blackMove[3];
            int toRow = (int)(toSq-toSq%8)/8;
            int toCol = toSq%8;
        int flags = blackMove[4];
        
        chessPiece pieceToMove = boardSquares[fromRow][fromCol].getPiece();
        boardSquares[fromRow][fromCol].setPiece(null); //remove piece from old board Square
        
        if((flags & moveFlagPromotion) != 0){ //pawn promoted
            
            int newPiece = flags & moveFlagPromotedPiece;
            pieceToMove.setType(pieceTypes[newPiece]);
        }
        
        else if((flags & moveFlagKingSideCastle) != 0){ //King side castle
            
            chessPiece rook = boardSquares[getIndex(8)][getIndex('h')].getPiece();
            boardSquares[getIndex(8)][getIndex('f')].setPiece(rook);
            boardSquares[getIndex(8)][getIndex('h')].setPiece(null);
            rook.setLocation(8, 'f');
        }
        
        else if((flags & moveFlagQueenSideCastle) != 0){ //Queen side castle
            
            chessPiece rook = boardSquares[getIndex(8)][getIndex('a')].getPiece();
            boardSquares[getIndex(8)][getIndex('d')].setPiece(rook);
            boardSquares[getIndex(8)][getIndex('a')].setPiece(null);
            rook.setLocation(8, 'd');
        }
        else if((flags & moveFlagEnPassant) != 0){ //En passant capture
            
            chessPiece deadPiece = boardSquares[toRow+1][toCol].getPiece();
            addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
            deadPiece.setVisible(false);
            boardSquares[toRow+1][toCol].setPiece(null);
        }
        
        if(capturedPiece != 0xE){ //0xE == EMTPY (No piece)
            
            chessPiece deadPiece = boardSquares[toRow][toCol].getPiece();
            addToTakenPieces(deadPiece.getColour(), deadPiece.getType()); //Add to piece capture history row
            deadPiece.setVisible(false);
        }
        
        boardSquares[toRow][toCol].setPiece(pieceToMove); //update game board with new piece position
        pieceToMove.setLocation(getRow(toRow), getColumn(toCol)); //update GUI
        
        if(pieceToMove.getType() == "Pawn" && (fromRow - toRow) == 2){
            //Set en-passant possibility for white if it was a double pawn push
            enPassantFlag = true;
            enPassantColumn = toCol;
        }
        
        
        //turn off last move visual indicators
        for(BoardTile[] bTs : boardSquares){
            for(BoardTile bT : bTs){
                bT.lightDown();
            }
        }
        
        //turn on this move visual indicators
        boardSquares[fromRow][fromCol].lightUp(BLACK);
        boardSquares[toRow][toCol].lightUp(BLACK);
            
    
    }

    /**
     * Assigns event listeners to white's pieces and defines their event instructions
     * 
     * From a high level, if a valid white piece is pressed then the mousePressed()
     * event will determine its current coordinates and also call the valid moves
     * function to calculate the legal moves for that piece.
     * 
     * The mouseDragged() event under the mouseMotionListener will take care of updating
     * the piece's position as it is dragged across the board.
     * 
     * Finally, the mouseReleased() event will trigger when the dragged piece is dropped 
     * somewhere. It will automatically adjust and "lock" the pieces location to the nearest
     * square it was dropped on. Once the new square is determined it will be checked for
     * legality in multiple stages. 
     * 
     * The first check is if the square is in the list of valid moves calculated
     * during the mouseClicked() event.
     * The next check calls the whiteLegalMoves() method of the chess engine, which
     * considers if white is in check or if the move would put it in check, and
     * the returned list is checked against the proposed move.
     * The final check is in the case white is trying to castle, in which case
     * the chess engine is consulted by calling the castleIsLegal() method, to
     * make sure white would not be skipping over or landing on any potential checks.
     * 
     * After these legality checks are performed, if the move is valid then
     * the GUI is updated with the move and a confirm dialog is displayed. If the
     * move is not legal, the piece is automatically returned to its initial square.
     * 
     */
    public void whiteTurnListener(){
        
        board.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) { }

            @Override
            public void mousePressed(MouseEvent e) {
              
                if(!confirmNeeded && whiteTurn){  
                    screenX = e.getXOnScreen();
                    screenY = e.getYOnScreen();

                    myX = e.getX();
                    myY = e.getY();

                    /*
                    Each square is 100x100 px. The rows are from y=[800,0] and the
                    columns are from x=[0,800]
                    */
                    int rowIndex = (int)(800-myY)/100; 
                    int colIndex = (int)(myX)/100;

                    currPiece = boardSquares[rowIndex][colIndex].getPiece();
                    if(currPiece != null && currPiece.getColour() == WHITE){
                      
                      myX = currPiece.getX();
                      myY = currPiece.getY();
                      currPiece.printInfo();
                      validSquares = generateValidMoves(currPiece);

                    }

                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                
                if(currPiece != null  && currPiece.getColour() == WHITE && whiteTurn){
                    newPos = returnNewLocation(currPiece); //adjusts and locks piece to a particular square
                    oldPos = getPosition(currPiece);
                    BoardTile newSquare = boardSquares[newPos[0]][newPos[1]];
                    boolean valid = false;
                    
                    for(BoardTile sq : validSquares){
                        if(sq.equals(newSquare)){
                            valid = true;
                        }
                    }
                    
                    int oldSquareIndex = oldPos[0]*8 + oldPos[1];
                    int newSquareIndex = newPos[0]*8 + newPos[1];
                    
                    if(valid){
                        
                        int[] legalMoves = JoeFlow.whiteLegalMoves();
                            
                            for(int legalMove : legalMoves){
                                
                                if((oldSquareIndex == ((legalMove << 8) >>> 24)) &&
                                    newSquareIndex == ((legalMove << 16) >>> 24)){
                                    valid = true;
                                    break;
                                }
                                valid = false;
                            }
                        }
                    
                    if(valid){
                          
                        if(currPiece.getType().equals("King") && oldSquareIndex == 4 && newSquareIndex == 6){
                            valid = JoeFlow.castleIsLegal(false); // king side castle
                        }
                        else if(currPiece.getType().equals("King") && oldSquareIndex == 4 && newSquareIndex == 2){
                            valid = JoeFlow.castleIsLegal(true); // Queen side castle
                        }
                    }
                    
                    if(valid){
                        
                        if(!newSquare.isEmpty()){ //capturing a piece
                            boardSquares[newPos[0]][newPos[1]].getPiece().setVisible(false);
                        }
                        else if(currPiece.getType().equals("King")){
                            
                            if(oldPos[1] == getIndex('e') && newPos[1] == getIndex('g')){
                                //King side castle
                                boardSquares[getIndex(1)][getIndex('h')].getPiece().setLocation(1, 'f');
                                castleFlag = true;
                            }
                            else if(oldPos[1] == getIndex('e') && newPos[1] == getIndex('c')){
                                //Queen side castle
                                boardSquares[getIndex(1)][getIndex('a')].getPiece().setLocation(1, 'd');
                                castleFlag = true;
                            }
                        }
                        else if(currPiece.getType().equals("Pawn") && enPassantFlag && newPos[0] == 5 
                                && newPos[1] == enPassantColumn){
                            //en passant capture
                            boardSquares[4][enPassantColumn].getPiece().setVisible(false);
                        }
                        
                        
                        
                        //Display confirmation dialogs
                        getComponentInContainer(infoMsgPanel, "Yes").setVisible(true);
                        getComponentInContainer(infoMsgPanel, "No").setVisible(true);
                        getComponentInContainer(infoMsgPanel, "confirmText").setVisible(true);
                        confirmNeeded = true;
                    }
                    else{
                        //Return piece to old square
                        currPiece.setLocation(getRow(oldPos[0]), getColumn(oldPos[1]));
                        
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
                
                if(!confirmNeeded && whiteTurn && currPiece != null && currPiece.getColour() == WHITE){
                    int deltaX = e.getXOnScreen() - screenX;
                    int deltaY = e.getYOnScreen() - screenY;
                    //update piece location continously as this event is called
                    currPiece.setLocation(myX + deltaX, myY + deltaY);  
                }    
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {}
        });
    }
    
    /**
     * Calculates the quasi-legal moves for a chessPiece based off the current board
     * and game flags, without considering checks
     * 
     * 
     * @param cP    the piece to calculate valid moves for
     * @return      an array of valid BoardTiles to land on
     */
    public BoardTile[] generateValidMoves(chessPiece cP){
        
        int[] piecePos = getPosition(cP);
        int pieceRow = piecePos[0];
        int pieceCol = piecePos[1];
        
        int nextRow, nextCol, prevRow, prevCol;
        int[] rowDelta, colDelta;
        
        ArrayList<BoardTile> validBoardList = new ArrayList();
       
        
        switch(cP.getType()){
            
            case "Pawn":
                if(boardSquares[pieceRow+1][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol]); //pawn push
                    
                    if(!cP.hasMoved() && boardSquares[pieceRow+2][pieceCol].isEmpty()){
                        validBoardList.add(boardSquares[pieceRow+2][pieceCol]); //double pawn push
                    }
                }
                
                if(pieceCol<7 && !boardSquares[pieceRow+1][pieceCol+1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol+1].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol+1]); //pawn capture right
                }
                if(pieceCol>0 && !boardSquares[pieceRow+1][pieceCol-1].isEmpty() 
                              && boardSquares[pieceRow+1][pieceCol-1].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow+1][pieceCol-1]); //pawn capture left
                }
                if(enPassantFlag && pieceRow == 4){
                    if(pieceRow == 4 && (pieceCol == enPassantColumn+1 || pieceCol == enPassantColumn-1)){
                        validBoardList.add(boardSquares[pieceRow+1][enPassantColumn]); //en passant capture
                    }
                }
            break;
                
            case "Rook":
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                //Sliding up
                while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][pieceCol]);
                }
                if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][pieceCol]);
                }
                
                //Sliding right
                while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][nextCol++]);
                }
                if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][nextCol]);
                }
                
                //Sliding down
                while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][pieceCol]);
                }
                if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][pieceCol]);
                }
                
                //Sliding left
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
                
                for(int j = 0; j < 8; j++){ //eight possible landing sites to check
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
                
                //Sliding up-right
                while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][nextCol++]);
                }
                if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][nextCol]);
                }
                
                //Sliding down-left
                while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][prevCol--]);
                }
                if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][prevCol]);
                }
                
                //reset variables
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                //Sliding up-left
                while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][prevCol--]);
                }
                if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][prevCol]);
                }
                
                //Sliding down-right
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
                
                //Sliding up
                while(nextRow < 8 && boardSquares[nextRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][pieceCol]);
                }
                if(nextRow < 8 && boardSquares[nextRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][pieceCol]);
                }
                
                //Sliding right
                while(nextCol < 8 && boardSquares[pieceRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][nextCol++]);
                }
                if(nextCol < 8 && boardSquares[pieceRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][nextCol]);
                }
                
                //Sliding down
                while(prevRow > -1 && boardSquares[prevRow][pieceCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][pieceCol]);
                }
                if(prevRow > -1 && boardSquares[prevRow][pieceCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][pieceCol]);
                }
                
                //Sliding left
                while(prevCol > -1 && boardSquares[pieceRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[pieceRow][prevCol--]);
                }
                if(prevCol > -1 && boardSquares[pieceRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[pieceRow][prevCol]);
                }
                
                //reset variables
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                //Sliding up-right
                while(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][nextCol++]);
                }
                if(nextRow < 8 && nextCol < 8 && boardSquares[nextRow][nextCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][nextCol]);
                }
                
                //Sliding down-left
                while(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[prevRow--][prevCol--]);
                }
                if(prevRow > -1 && prevCol > -1 && boardSquares[prevRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[prevRow][prevCol]);
                }
                
                //reset variables
                nextRow = pieceRow+1;
                nextCol = pieceCol+1;
                prevRow = pieceRow-1;
                prevCol = pieceCol-1;
                
                //Sliding up-left
                while(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].isEmpty()){
                    validBoardList.add(boardSquares[nextRow++][prevCol--]);
                }
                if(nextRow < 8 && prevCol > -1 && boardSquares[nextRow][prevCol].getPiece().getColour() == BLACK){
                    validBoardList.add(boardSquares[nextRow][prevCol]);
                }
                
                //Sliding down-right
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
                
                for(int j = 0; j < 8; j++){ //eight possible squares to check
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
                if(!cP.hasMoved() && boardSquares[getIndex(1)][getIndex('a')].getPiece() != null &&
                                     boardSquares[getIndex(1)][getIndex('a')].getPiece().getType().equals("Rook") &&
                                     !boardSquares[getIndex(1)][getIndex('a')].getPiece().hasMoved() &&
                                     boardSquares[getIndex(1)][getIndex('b')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('c')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('d')].isEmpty()){
                    //Queen side castle is legal
                    validBoardList.add(boardSquares[getIndex(1)][getIndex('c')]);
                }
                if(!cP.hasMoved() && boardSquares[getIndex(1)][getIndex('h')].getPiece() != null &&
                                     boardSquares[getIndex(1)][getIndex('h')].getPiece().getType().equals("Rook") &&
                                     !boardSquares[getIndex(1)][getIndex('h')].getPiece().hasMoved() &&
                                     boardSquares[getIndex(1)][getIndex('f')].isEmpty() &&
                                     boardSquares[getIndex(1)][getIndex('g')].isEmpty()){
                    //King side castle is legal
                    validBoardList.add(boardSquares[getIndex(1)][getIndex('g')]);
                }   
            break;
        }

        BoardTile[] vbt = new BoardTile[validBoardList.size()];
        validBoardList.toArray(vbt);
        return vbt;
    }
    
    
    /**
     * Scans the 64-squares of the chessboard until the square which holds the
     * piece in question is found, and then returns the location
     * 
     * 
     * @param cP    the piece whose location is returned
     * @return      an int array with 2 elements, the row and the column index
     */
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
    
    /**
     * Initializes the layout of the chess board and all the associated elements
     * and returns a JPanel container which consists of the entire GUI
     * 
     * @return  the game panel to display within the JFrame window
     */
    public JPanel setUpChessBoard(){
        
        JPanel chessPanel = new JPanel();
        
        chessPanel.setLayout(new BorderLayout());
        
        JLayeredPane chessBoard = new JLayeredPane();
        
        chessBoard.setBorder(new LineBorder(Color.BLACK, 1));
        chessBoard.setPreferredSize(new Dimension(800, 1000));
        
        infoMsgPanel = new JPanel();
        infoMsgPanel.setLayout(new BoxLayout(infoMsgPanel, BoxLayout.Y_AXIS));
        infoMsgPanel.setBounds(0, 0, 800, 80);
        infoMsgPanel.setBorder(new LineBorder(Color.BLACK));
        
        footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
    
    /**
     * Configures the JPanel which sits above the chess board and contains the
     * confirm dialog for moves. Adds keyboard shortcuts for the confirm buttons
     */
    public void setUpInfoMsgPanel(){
        
        Action buttListen = new ButtonAction();
        
        //Yes button
        JButton yConfirm = new JButton("Yes");
        
        yConfirm.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke('y'), "Anything1");      //Map to y button
        yConfirm.getActionMap().put("Anything1", buttListen);
        
        yConfirm.setName("Yes");
        yConfirm.addActionListener(buttListen);
        
        //No button
        JButton nConfirm = new JButton("No");
        
        nConfirm.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke('n'), "Anything2");      //Map to y button
        nConfirm.getActionMap().put("Anything2", buttListen);

        nConfirm.setName("No");
        nConfirm.addActionListener(buttListen);
        
        JPanel buttPanel = new JPanel();
        buttPanel.setLayout(new BoxLayout(buttPanel, BoxLayout.X_AXIS));
        
        buttPanel.add(Box.createHorizontalStrut(150));
        buttPanel.add(yConfirm);
        buttPanel.add(Box.createHorizontalGlue());
        buttPanel.add(nConfirm);
        buttPanel.add(Box.createHorizontalStrut(150));
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));
        
        JLabel confirmText = new JLabel("Are you sure?");
        confirmText.setName("confirmText");
        confirmText.setAlignmentX(CENTER_ALIGNMENT);
        confirmText.setFont(new Font("Arial", Font.BOLD, 25));
        
        infoPanel.add(confirmText);
        
        infoMsgPanel.add(Box.createVerticalStrut(5));
        infoMsgPanel.add(infoPanel);
        infoMsgPanel.add(Box.createVerticalGlue());
        infoMsgPanel.add(buttPanel);
        
        //Set to non-visible
        yConfirm.setVisible(false);
        nConfirm.setVisible(false);
        confirmText.setVisible(false);
        
    }
    
    /**
     * Configures the footer JPanel, which sits below the chessboard and contains
     * the captured piece "graveyards" for both players
     */
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
    
    /**
     * Adjusts the location  of a chessPiece after it is dragged by the user so
     * it resides directly over a board square
     * 
     * @param cP    the chessPiece whose location needs to be adjusted
     * @return      an int array with 2 elements: the row index and column index
     */
    public int[] returnNewLocation(chessPiece cP){
        
        int x = cP.getLocation().x;
        int y = cP.getLocation().y;
        
        boolean colFound = false;
        int colStartX = 700;
        
        //Scan through columns until column pixel range contains the current x location
        while(!colFound){
            if(x+50 >= colStartX){
                    x = colStartX;
                    colFound = true;
                    break;
            }
            colStartX = colStartX - 100;
            if(colStartX == 0){
                x = colStartX;
                colFound = true;
            }
        }
        
        boolean rowFound = false;
        int rowStartY = 780;
        
        //Scan through rows until row pixel range contains the current y location
        while(!rowFound){
            if(y+50 >= rowStartY){
                    y = rowStartY;
                    rowFound = true;
                    break;
            }
            rowStartY = rowStartY - 100;
            if(rowStartY == 100){
                y = rowStartY;
                rowFound = true;
            }
        }
        
        cP.setLocation(x,y);
        return new int[]{(int)(780-y)/100, (int)(x)/100};
    }
    
    /**
     * Configures the initial chess piece set up for the start of the game
     * 
     * @param colour    Colour to set-up (WHITE=0, BLACK=1)
     * @return          A 16-element array containing the configured chess pieces 
     */
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
    
    /**
     * Returns an array containing all the active pieces of a particular colour
     * 
     * @param colour    the colour of piece to return
     * @return          an array containing all the active chess pieces
     */
    public chessPiece[] getPiecesOnBoard(int colour){
        ArrayList<chessPiece> piecesLst = new ArrayList();
        
        BoardTile bt;
        //Scan through every BoardTile on the board
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
    
    /**
     * Configures the 64 BoardTile instances which cover the 64 squares on the board
     * 
     * @return      an 8x8 matrix array of the 64 BoardTiles
     */
    public BoardTile[][] setUpTiles(){
        
        BoardTile[][] bT = new BoardTile[8][8];
        
        for(int row : rows){
            for(char c : columns){
                bT[getIndex(row)][getIndex(c)] = new BoardTile(null, row, c);
            }
        }
        
        return bT;
    }
    
    /**
     * Adds a captured piece to the "graveyard" in the footerPanel
     * 
     * @param colour    the colour of the piece to be added
     * @param type      the type of the piece, represented by a capitalized name (e.g "Knight" or "Pawn")
     */
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
    
    /**
     * Returns the index of the row (0-7) relative to the standard board representation of the rows (1-8)
     * @param row   the row in question
     * @return      the index of the row
     */
    public static int getIndex(int row){
        return row - 1;
    }
    
    /**
     * Returns the index of the column (0-7) relative to the standard board representation of the columns (a-h)
     * @param col   the column in question
     * @return      the index of the column
     */
    public static int getIndex(char col){
        return ((int) col) - 97;
    }
    
    /**
     * Returns the standard board representation of a row (1-8) relative to its in-game index(0-7)
     * @param rowIndex  the row in question
     * @return          the row number
     */
    public static int getRow(int rowIndex){
        return rowIndex + 1;
    }
    
    /**
     * Returns the standard board representation of a column (a-h) relative to its in-game index(0-7)
     * @param columnIndex   the column in question
     * @return              the column (a-h)
     */
    public static char getColumn(int columnIndex){
        return (char) (columnIndex + 97);
    }
    
    /**
     * Defines the instructions to take when a button is pressed. The buttons
     * defined in this game are:
     * 
     * Confirm Options:
     * "Yes"    - User confirms move
     * "No"     - User rescinds moves
     * 
     * Promotion Options:
     * "Queen"
     * "Bishop"
     * "Rook"
     * "Knight"
     */
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
                        //Piece is captured by move
                        chessPiece deadPiece = boardSquares[newPos[0]][newPos[1]].getPiece();
                        addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
                        
                    }
                    
                    boardSquares[newPos[0]][newPos[1]].setPiece(currPiece); //move piece to new square
                    boardSquares[oldPos[0]][oldPos[1]].setPiece(null);      //remove piece from old square
                    
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
                    
                    if(currPiece.getType().equals("Pawn") && enPassantFlag && newPos[0] == 5 && newPos[1] == enPassantColumn){
                        chessPiece deadPiece = boardSquares[4][newPos[1]].getPiece();
                        addToTakenPieces(deadPiece.getColour(), deadPiece.getType());
                        boardSquares[4][newPos[1]].setPiece(null);
                        moveFlags = moveFlagEnPassant;
                        System.out.println("en passant");
                    }
                    
                    if(currPiece.getType().equals("Pawn") && newPos[0] == getIndex(8)){
                            //Pawn promoted - display promotion options modal box for user to choose piece
                            promotionOptions.setVisible(true);
                            confirmNeeded = true;
                            break;
                    }
                    else{
                        //Update the chess engine with the move information
                        JoeFlow.makeMove(oldPos, newPos, moveFlags);
                        
                        whiteTurn = false;
                        //Notify the wait() lock in the main game controller (see: JoeFlowPlaysChess.startGame())
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }

                        break;
                    }
                    
                    
                    
                case "No":
                    confirmNeeded = false;

                    if(!boardSquares[newPos[0]][newPos[1]].isEmpty()){
                        //undo captured piece disappearing
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
                    if(currPiece.getType().equals("Pawn") && enPassantFlag && newPos[0] == 5 && newPos[1] == enPassantColumn){
                        boardSquares[4][enPassantColumn].getPiece().setVisible(true);
                    }
                    
                    //return to old position
                    currPiece.setLocation(getRow(oldPos[0]), getColumn(oldPos[1]));
                    break;
                    
                case "queen":
                    currPiece.setType("Queen");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 4;
                    
                    //Update the chess engine with the move information
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                    //Notify the wait() lock in the main game controller (see: JoeFlowPlaysChess.startGame())
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }

                    break;
                    
                case "knight":
                    currPiece.setType("Knight");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 2;
                    
                    //Update the chess engine with the move information
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                    //Notify the wait() lock in the main game controller (see: JoeFlowPlaysChess.startGame())
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                case "bishop":
                    currPiece.setType("Bishop");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 3;
                    
                    //Update the chess engine with the move information
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                    //Notify the wait() lock in the main game controller (see: JoeFlowPlaysChess.startGame())
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                case "rook":
                    currPiece.setType("Rook");
                    promotionOptions.setVisible(false);
                    confirmNeeded = false;
                    moveFlags = moveFlagPromotion | 1;
                    
                    //Update the chess engine with the move information
                    JoeFlow.makeMove(oldPos, newPos, moveFlags);
                    
                    whiteTurn = false;
                    //Notify the wait() lock in the main game controller (see: JoeFlowPlaysChess.startGame())
                        synchronized(LOCK){
                            LOCK.notifyAll();
                        }
                    break;
                    
                default:
                    break;
            }
            
            if(buttonName != "No"){
                
                //turn off last move visual indicators
                for(BoardTile[] bTs : boardSquares){
                    for(BoardTile bT : bTs){
                        bT.lightDown();
                    }
                }
                //turn on this move's visual indicators
                boardSquares[oldPos[0]][oldPos[1]].lightUp(WHITE);
                boardSquares[newPos[0]][newPos[1]].lightUp(WHITE);
            }
            
            //Stop displaying the confirm dialog
            getComponentInContainer(infoMsgPanel, "Yes").setVisible(false);
            getComponentInContainer(infoMsgPanel, "No").setVisible(false);
            getComponentInContainer(infoMsgPanel, "confirmText").setVisible(false);
        }
    }
    
    /**
     * Recursively searches a Container, and all its nested Containers for a
     * component based off the name provided
     * 
     * @param c     the Container to search
     * @param name  the name of the Component to find
     * @return      the found component, casted to a JComponent object
     */
    public static JComponent getComponentInContainer(Container c, String name){
        int num = c.getComponentCount();

        Component jC = null;
        JComponent returnedComp = null;
        for(int i = 0; i < num; i++){
            jC = c.getComponent(i);
            
            if(c.getClass().isInstance(jC)){                                 //if the component c is a container itself
                returnedComp = getComponentInContainer((Container)jC, name); //search that container
                if(returnedComp != null){
                    break;
                }
            }
            else if(jC.getName() != null && jC.getName().equals(name)){
                //found it
                returnedComp = (JComponent) jC;
                break;
            }
        }
        return returnedComp;
    }
    
    /**
     * Configures the custom look and feel of a JButton object
     * 
     * @param butt          the JButton to customize
     * @param unpressed     URI pathname of the icon to represent the unpressed state of the button
     * @param pressed       URI pathname of the icon to represent button when it is pressed
     */
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
    
    /**
     * Start an instance of the JoeFlowPlaysChess class, which is an extension
     * of the JFrame class. Set it to visible.
     * 
     * @param args Not used
     */
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
