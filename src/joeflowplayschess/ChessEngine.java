/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * The ChessEngine class computes all the decision making for the engine, which plays
 * as black exclusively. 
 * 
 * The architecture is based on bit-board game representation, which
 * allows for rapid computations of game information and efficient memory usage.
 * The move generation uses fairly common techniques, including magic-bitboards
 *  for the move generation of sliding pieces (rooks, bishops, queen). Move
 *  selection is made using a recursive search through the move tree with alpha
 *  beta pruning techniques. The board evaluation is a minimax score based 
 *  function which uses a material score calculation as the primary factor for
 *  board advantage, with heuristic evaluation factors as well if there is no
 *  material advantage found. Factors such as pawn structure, centre control,
 *  piece advancement, and piece mobility are looked at.
 * 
 * +
 */
public class ChessEngine {

	//declarations
	private int 	  nodeCount;
	private Constants Constants;
	private int 	  turn;
	
	//declarations + initializations
	private int WHITE = 			0;
	private int BLACK =				1;    
	    
	private int wPawn = 			0;
	private int wKnight = 			1;
	private int wBishop = 			2;
	private int wRook = 			3;
	private int wQueen = 			4;
	private int wKing = 			5;
	
	private int bPawn = 			6;
	private int bKnight = 			7;
	private int bBishop = 			8;
	private int bRook = 			9;
	private int bQueen = 			10;
	private int bKing = 			11;
	
	private int[] king = 			new int[]{wKing, bKing};
	private int[] initKingPos =		new int[]{4, 60};
	
	private int empty = 			0xE;
	private int defaultDepth = 		3;
	
	Random r = 						new Random();
	boolean debugMode = 			false;
	boolean initialized =			false;
	boolean firstgame = 			true;

public ChessEngine(){}

public void init(){
	
	if(!initialized){
		if(firstgame){
			Constants = new Constants();
			firstgame = false;
		}
		
		/*GAME FLAGS
		variable name: flags
		data type: byte
		
		bit 1: En Passant is possible, there was a pawn double pushed on the last turn
		bits 2-4: The file number (0-7) that a pawn was double pushed to on the last turn
		
		bit 5: Black Queen Side Castle possible (Rook on sqaure 56)
		bit 6: Black King Side Castle possible  (Rook on square 63)
		bit 7: White Queen Side Castle possible (Rook on square 0)
		bit 8: White King Side Castle possible  (Rook on square 7)
		  
		*/
		gameFlags = (byte) 0b11110000; //Initialize with all castling rights
		
		setUpInitialBoard(); //set up board for beginning of game
		initialized = true;
	}
}

/**
 * Utility function to set up the game board with the initial positions of all the pieces.
 * 
 * Game board information is stored in two distinct structures:
 * 	1. An array of 12 longs, that represent the bitboards for the 12 different types
 * 	of chess pieces found on a board
 * 
 *  2. An array of 64 integers, that represent the 64 squares on the board and the values
 *  correspond to the piece types located on each square, if any. The values are based off the
 *  fields declared in the class declarations. Note that an empty square is represented by the
 *  number 14, or E in hexidecimal (for empty)
 */
private void setUpInitialBoard(){
	
	gamePieceBoards = new long[12];
	gameBoard = new int[64];
	
	for(int sq = 0; sq < 64; sq++){
	    
	    //White Pieces
	    if(sq == 0 || sq == 7){
	        gamePieceBoards[wRook] = gamePieceBoards[wRook] | (1L << sq);
	        gameBoard[sq] = wRook;
	    }
	    if(sq == 1 || sq == 6){
	        gamePieceBoards[wKnight] = gamePieceBoards[wKnight] | (1L << sq);
	        gameBoard[sq] = wKnight;
	    }
	    if(sq == 2 || sq == 5){
	        gamePieceBoards[wBishop] = gamePieceBoards[wBishop] | (1L << sq);
	        gameBoard[sq] = wBishop;
	    }
	    if(sq == 3){
	        gamePieceBoards[wQueen] = gamePieceBoards[wQueen] | (1L << sq);
	        gameBoard[sq] = wQueen;
	    }
	    if(sq == 4){
	        gamePieceBoards[wKing] = gamePieceBoards[wKing] | (1L << sq);
	        gameBoard[sq] = wKing;
	    }
	    if(sq > 7 && sq < 16){
	        gamePieceBoards[wPawn] = gamePieceBoards[wPawn] | (1L << sq);
	        gameBoard[sq] = wPawn;
	    }
	    
	    if(sq > 15 && sq < 48){
	        gameBoard[sq] = empty;
	    }
	    
	    //Black pieces
	    if(sq == 56 || sq == 63){
	        gamePieceBoards[bRook] = gamePieceBoards[bRook] | (1L << sq);
	        gameBoard[sq] = bRook;
	    }
	    if(sq == 57 || sq == 62){
	        gamePieceBoards[bKnight] = gamePieceBoards[bKnight] | (1L << sq);
	        gameBoard[sq] = bKnight;
	    }
	    if(sq == 58 || sq == 61){
	        gamePieceBoards[bBishop] = gamePieceBoards[bBishop] | (1L << sq);
	        gameBoard[sq] = bBishop;
	    }
	    if(sq == 59){
	        gamePieceBoards[bQueen] = gamePieceBoards[bQueen] | (1L << sq);
	        gameBoard[sq] = bQueen;
	    }
	    if(sq == 60){
	        gamePieceBoards[bKing] = gamePieceBoards[bKing] | (1L << sq);
	        gameBoard[sq] = bKing;
	    }
	    if(sq > 47 && sq < 56){
	        gamePieceBoards[bPawn] = gamePieceBoards[bPawn] | (1L << sq);
	        gameBoard[sq] = bPawn;
	    }
	}
	turn = WHITE;
}



//--Decision Making Logic----------------------------------------------------------------------

/**
 * Top-level decision controller which initiates the move selection process for black.
 * 
 * This function is called by the main JoeFlowPlaysChess class when it is black's turn.
 * The function returns an int[] array which is the standard 32 bit move encoding, split into
 * a 5 or 6 element array. A standard move will return a 5 element array, which is just the move
 * information. If the move results in black winning, or a black stalemate, then the array returned
 * will have 6 elements.  The first 5 elements of the array are the move information, and the last 
 * element will be a flag signifying a black checkmate (0) or a stalemate caused by black's move (-1)
 * 
 * @param colour	Always BLACK, which is 1
 * @return 			array of ints containing move and game information
 */
public int[] selectMove(int colour, int searchDepth){

int[] bestMove, moveInfo;

nodeCount = 0;

bestMove = chooseBestMove(gameBoard, gamePieceBoards, gameFlags, colour, searchDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);

return checkForMateAndUpdateBoard(colour, searchDepth, bestMove);

}

public int[] selectMoveRestricted(String[] FENMoves){

int[] bestMove, moveInfo;

nodeCount = 0;

bestMove = chooseBestMoveRestricted(gameBoard, gamePieceBoards, gameFlags, 
									turn, defaultDepth, Integer.MIN_VALUE, 
									Integer.MAX_VALUE, 
									Stream.of(FENMoves).map(s -> convertFENtoMoveInt(s)).mapToInt(Integer::intValue).toArray());

return checkForMateAndUpdateBoard(turn, defaultDepth, bestMove);

}

private int[] checkForMateAndUpdateBoard(int colour, int searchDepth, int[] bestMove) {
	int[] moveInfo;
	moveInfo = parseMove(bestMove[0]);
	
	//Special cases
	if(bestMove[0] == -1){
	    //moving player has lost
	    return bestMove;
	}
	else if(bestMove[1] == (2*colour - 1)*(searchDepth-1)*1000000){
	    //moving player wins
	    return new int[]{moveInfo[0], moveInfo[1], moveInfo[2], moveInfo[3], moveInfo[4], 0};
	}
	
	else if(bestMove[1] == (2*colour - 1)*(searchDepth-1)*-1000000){
	    //moving player forcing draw
	    return new int[]{moveInfo[0], moveInfo[1], moveInfo[2], moveInfo[3], moveInfo[4], -1};
	}
	
	//Update game information now that the move has been completed
	gameFlags = updateGame(moveInfo, gameBoard, gamePieceBoards, gameFlags);
	
	//Print some relevant infomation to the console
	if(debugMode) {
		System.out.println("Nodes traversed: " + nodeCount);
		System.out.println("Best Move Score For Black:");
		System.out.println(bestMove[1]);
		System.out.println("Game Flags:");
		System.out.println(Integer.toBinaryString(Byte.toUnsignedInt(gameFlags)));
		printBoardArray(gameBoard);
	}
	
	return moveInfo;
}

/**
 * Move Selection function which uses recursion to search the possible move space to a specified depth
 * 
 * This is the primary decision making function for the engine. It calls the generateAllMoves() function
 * to generate a list of every possible move, and then for each possible move a copy of the game state is 
 * updated with the results of the move, and then this function is called again from the perspective of the
 * opposing player and the process is repeated. This is done until a specified depth is reached (determined 
 * by the value of class field 'searchdepth' and then the board at that state is evaluated using an evaluation
 * function. The results of the evaluation are returned through the recursion stack to the root function call
 * to decide the best move to make next.
 * 
 * An alpha and beta score are used to keep track of the most positive (alpha) and most negative (beta) scores encountered
 * from moves previously searched in the search tree, which allows for pruning of the tree if the better move for the
 * opposing player has already been determined and the tree currently being searched would not be selected by them.
 */
private int[] chooseBestMove(int[] currBoard, long[] pieceBoards, byte flags, int colour, int depth, int alpha, int beta){

int 	i;
int[] 	moves, moveScore, tempBoard, bestMove, parsedMove;
int[][] parsedMoves, parsedMoves1;
long[] 	tempPieceBoards;
byte 	tempFlags;

boolean check = false;
boolean legal = true;

ArrayList<int[]> bestMoves = new ArrayList();

moves = generateAllMoves(colour, currBoard, pieceBoards, flags);

if(inCheck(colour, currBoard, pieceBoards, flags)){
    check = true;
}

for(i = 0; i < moves.length; i++){
    
    nodeCount++;
    tempBoard = Arrays.copyOf(currBoard, 64);
    tempPieceBoards = Arrays.copyOf(pieceBoards, 12);
    tempFlags = flags;
    
    parsedMove = parseMove(moves[i]);
    
    tempFlags = updateGame(parsedMove, tempBoard, tempPieceBoards, tempFlags);
    
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] + 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, false); // king side castle
    }
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] - 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, true); // Queen side castle
    }
    
    
    if(legal && !inCheck(colour, tempBoard, tempPieceBoards, tempFlags)){
        
        if(depth > 0){
        	//Call chooseBestMove on the resulting board position after move is made, from the opposing players view	
            int[] theirMove = chooseBestMove(tempBoard, tempPieceBoards, tempFlags, 1-colour, depth - 1, alpha, beta);
            moveScore = new int[]{moves[i], theirMove[1]};   
        }
        else{
        	//Evaluate board position if at the terminal depth
            moveScore = new int[]{moves[i], evaluateGameScore(tempPieceBoards, tempBoard, tempFlags)};
        }
        
        
        if(bestMoves.isEmpty()){
                bestMoves.add(moveScore);        
        }
        else if(bestMoves.get(0)[1] == moveScore[1]){
                bestMoves.add(moveScore);
        }
        
        if(colour == BLACK){
            
        	//A higher move score is beneficial for BLACK
            if(bestMoves.get(0)[1] < moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);   
            }
            
            alpha = Math.max(alpha, bestMoves.get(0)[1]);
            
        }
        else{
            //A lower (possibly negative) game score is beneficial for WHITE
            if(bestMoves.get(0)[1] > moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);
            } 
            beta = Math.min(beta, bestMoves.get(0)[1]);
        }
        
        if(beta < alpha){
                break; //Beta or alpha cut-off
        }
    }
    
    legal = true;
}

if(bestMoves.size() > 1){
	//If more than one move results in the same board score, choose one randomly
    int randInt = r.nextInt(bestMoves.size());
    bestMove = bestMoves.get(randInt);
}
else if(bestMoves.size() == 1){
    bestMove = bestMoves.get(0);
}
else{
    //If bestMoves does not have any moves in it, this means no moves for black are legal and it is either in checkmate or it is a stalemate
    if(check){
        bestMove = new int[]{-1, (-2*colour + 1)*1000000*depth}; //checkmate
    }
    else{
        bestMove = new int[]{-1, (2*colour - 1)*1000000*depth}; //stalemate
    }
    
}

return bestMove;

}

private int[] chooseBestMoveRestricted(int[] currBoard, long[] pieceBoards, byte flags, int colour, int depth, int alpha, int beta, int[] movesToSearch){

int 	i;
int[] 	moves, moveScore, tempBoard, bestMove, parsedMove;
int[][] parsedMoves, parsedMoves1;
long[] 	tempPieceBoards;
byte 	tempFlags;

boolean check = false;
boolean legal = true;

ArrayList<int[]> bestMoves = new ArrayList();

moves = movesToSearch;

if(inCheck(colour, currBoard, pieceBoards, flags)){
    check = true;
}

for(i = 0; i < moves.length; i++){
    
    nodeCount++;
    tempBoard = Arrays.copyOf(currBoard, 64);
    tempPieceBoards = Arrays.copyOf(pieceBoards, 12);
    tempFlags = flags;
    
    parsedMove = parseMove(moves[i]);
    
    tempFlags = updateGame(parsedMove, tempBoard, tempPieceBoards, tempFlags);
    
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] + 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, false); // king side castle
    }
    if(parsedMove[0] == king[colour] && parsedMove[2] == initKingPos[colour] && parsedMove[3] == initKingPos[colour] - 2){
        legal = castleIsLegal(colour, tempBoard, tempPieceBoards, tempFlags, true); // Queen side castle
    }
    
    
    if(legal && !inCheck(colour, tempBoard, tempPieceBoards, tempFlags)){
        
        if(depth > 0){
        	//Call chooseBestMove on the resulting board position after move is made, from the opposing players view	
            int[] theirMove = chooseBestMove(tempBoard, tempPieceBoards, tempFlags, 1-colour, depth - 1, alpha, beta);
            moveScore = new int[]{moves[i], theirMove[1]};   
        }
        else{
        	//Evaluate board position if at the terminal depth
            moveScore = new int[]{moves[i], evaluateGameScore(tempPieceBoards, tempBoard, tempFlags)};
        }
        
        
        if(bestMoves.isEmpty()){
                bestMoves.add(moveScore);        
        }
        else if(bestMoves.get(0)[1] == moveScore[1]){
                bestMoves.add(moveScore);
        }
        
        if(colour == BLACK){
            
        	//A higher move score is beneficial for BLACK
            if(bestMoves.get(0)[1] < moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);   
            }
            
            alpha = Math.max(alpha, bestMoves.get(0)[1]);
            
        }
        else{
            //A lower (possibly negative) game score is beneficial for WHITE
            if(bestMoves.get(0)[1] > moveScore[1]){
                bestMoves.clear();
                bestMoves.add(moveScore);
            } 
            beta = Math.min(beta, bestMoves.get(0)[1]);
        }
        
        if(beta < alpha){
                break; //Beta or alpha cut-off
        }
    }
    
    legal = true;
}

if(bestMoves.size() > 1){
	//If more than one move results in the same board score, choose one randomly
    int randInt = r.nextInt(bestMoves.size());
    bestMove = bestMoves.get(randInt);
}
else if(bestMoves.size() == 1){
    bestMove = bestMoves.get(0);
}
else{
    //If bestMoves does not have any moves in it, this means no moves for black are legal and it is either in checkmate or it is a stalemate
    if(check){
        bestMove = new int[]{-1, (-2*colour + 1)*1000000*depth}; //checkmate
    }
    else{
        bestMove = new int[]{-1, (2*colour - 1)*1000000*depth}; //stalemate
    }
    
}

return bestMove;

}

/**
 * Evaluates a game board according to various metrics, most importantly using a weighted
 * material score calculation.
 * 
 */
public int evaluateGameScore(long[] PIECEBOARDS, int[] currBoard, byte flags){

int overallScore;
    
overallScore = 2*(numSet(PIECEBOARDS[bPawn]) - numSet(PIECEBOARDS[wPawn])) +
                    7*(numSet(PIECEBOARDS[bKnight]) - numSet(PIECEBOARDS[wKnight])) +
                    6*(numSet(PIECEBOARDS[bBishop]) - numSet(PIECEBOARDS[wBishop])) +   
                    10*(numSet(PIECEBOARDS[bRook]) - numSet(PIECEBOARDS[wRook])) +
                    18*(numSet(PIECEBOARDS[bQueen]) - numSet(PIECEBOARDS[wQueen]));  

overallScore = 2*(overallScore) + centreControlScore(PIECEBOARDS);

overallScore = 3*(overallScore) + 2*(pawnStructureScore(bPawn) - pawnStructureScore(wPawn));

overallScore = 2*(overallScore) + positionScore(BLACK, currBoard, PIECEBOARDS, flags) - 
                                  positionScore(WHITE, currBoard, PIECEBOARDS, flags);



overallScore = 4*(overallScore) + advancementScore(PIECEBOARDS, BLACK) - advancementScore(PIECEBOARDS, WHITE);

return overallScore;

}

//---------------------------------------------------------------------------------------------




//-- Board Evaluation Heuristics---------------------------------------------------------------

/**
 * Advancement score values pieces in ranks further away from their side. It is the smallest
 * weighted heuristic, but it tends to ensure black is aggressive and plays an offensive strategy
 */
public int advancementScore(long[] currPieceBoards, int colour){
    
    int[] rankScore = new int[8];
    int advancement = 0;
    
    for(int i = 0; i < 8; i++){
        for(int j = colour*6; j < colour*6+6; j++){
            rankScore[i] += numSet(currPieceBoards[j] & Constants.RANKS[i]);
        }
        
        advancement += rankScore[i]*(i + (7-i)*colour);
    }
    
    return advancement;
}

/**
 * Assigns a score to the number of pieces in the center of the board. Control of the center
 * squares is advantageous
 */
public int centreControlScore(long[] PIECEBOARDS){
    
    long blackCentre = 0, whiteCentre = 0;
    
    for(int i = 0; i < 6; i++){
        whiteCentre |= (PIECEBOARDS[i] & Constants.CENTER_4);
    }
    for(int j = 6; j < 12; j++){
        blackCentre |= (PIECEBOARDS[j] & Constants.CENTER_4);
    }
    
    return numSet(blackCentre) - numSet(whiteCentre);
}

/**
 * Assigns a score to the amount of pieces you are currently attacking (and conversely the amount
 * of your pieces being attacked by the opposing player) as well as how many moves you have available
 * to choose from at that board position. Mobility is advantageous, generally.
 */
public int positionScore(int colour, int[] currBoard, long[] currPieceBoards, byte currFlags){
    
    int[] yourMoves = generateAllMoves(colour, currBoard, currPieceBoards, currFlags);
    int piecesAttacked = 0;
    
    int[] tempBoard;
    long[] tempPieceBoards;
    byte tempFlags;
    
    for(int move : yourMoves){
        if(((move << 4) >>> 28) > 0){
            piecesAttacked++;
        }
    }
    
    return 2*(piecesAttacked) + yourMoves.length;
}

/**
 * Assigns a score to the amount of isolated and doubled pawns the player has in the current
 * board position. Valued negatively.
 */
public int pawnStructureScore(long pawns){
    
    long tPawns;
    int dubs = 0, isos = 0;
    int file;
    
    tPawns = pawns;
    while(tPawns > 0){
        int pawn = Long.numberOfTrailingZeros(tPawns);
        if((pawns & (1L << (pawn+8))) != 0){ dubs++;}
        tPawns &= (tPawns-1);
    }
    
    tPawns = pawns;
    while(tPawns > 0){
        int pawn = Long.numberOfTrailingZeros(tPawns);
        file = pawn%8;
        
        switch (file) {
            case 7:
                if((pawns & Constants.FILES[file-1]) == 0) { isos++;}
                break;
            case 0:
                if((pawns & Constants.FILES[file+1]) == 0) { isos++;}
                break;
            default:
                if(((pawns & Constants.FILES[file+1]) == 0) && ((pawns & Constants.FILES[file-1]) == 0)) { isos++;}
                break;
        }
        tPawns &= (tPawns-1);
    }
    
    return -1*(dubs+isos);
}

//---------------------------------------------------------------------------------------------



//--Tools for Decision Making------------------------------------------------------------------

/**
 * Splits an encoded integer representing a piece move into a 5 element array, according to the
 * defined move encoding scheme:
 * 
 * move (MSB --> LSB):
 * pieceMoving (4) | capturedPiece(4) | fromSq(8) | toSq(8) | flags(8)
 * 
 *  Move Flags:
 *  
 *  bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
 *  bit 5: promotion flag
 *  bit 6: en-passant capture flag
 *  bit 7: Queen Side Capture
 *  bit 8: King Side Capture
 *
 * 
 */
public int[] parseMove(int move){
    int piece =          move >>> 28;
    int capturedPiece = (move << 4) >>> 28;
    int fromSq =        (move << 8) >>> 24;
    int toSq =          (move << 16) >>> 24;
    byte moveFlags =    (byte) move;
    
    return new int[]{piece, capturedPiece, fromSq, toSq, moveFlags};
}

/**
 *  [Currently Unused] Method for sorting and pre-parsing all the moves for use in the
 *  move search function.
 *  
 *  Not producing any noticeable increase in efficiency of the
 *  alpha-beta pruning right now. Need to increase the heuristic evaluation accuracy
 */
public int[][] sortAndParseMoves(int colour, int[] moves){
    
    int[][] sortedParsedMoves = new int[moves.length][5];    
    int[][] parsedMoves = new int[moves.length][5];
    int[] moveScores = new int[moves.length];
    int[] bestMove, rowDel;
    int fromSq, toSq;

    for(int i = 0; i < moves.length; i++){
        
        parsedMoves[i] = parseMove(moves[i]);
        
        fromSq = parsedMoves[i][2];
        toSq = parsedMoves[i][3];
        
        rowDel = new int[]{(int)((toSq - toSq%8)/8) - (int)((fromSq - fromSq%8)/8), 
                           (int)((fromSq - fromSq%8)/8) - (int)((toSq - toSq%8)/8)};
        
        if(parsedMoves[i][1] != empty){
            moveScores[i] = 2*(4*(parsedMoves[i][1]%6) - (parsedMoves[i][0]%6));
        }
        
        moveScores[i] += rowDel[colour];
    }
    
    bestMove = new int[2];
    
    for(int j = 0; j < moves.length; j++){
        bestMove[0] = 0;
        bestMove[1] = moveScores[0];
        
        for(int k = 1; k < moves.length; k++){
            
            if(moveScores[k] > bestMove[1]){ 
                bestMove[0] = k;
                bestMove[1] = moveScores[k];
            }
        }
        sortedParsedMoves[j] = parsedMoves[bestMove[0]];
        moveScores[bestMove[0]] = Integer.MIN_VALUE;
    }
    
    return sortedParsedMoves;
    
}

/**
 * Accepts a move, and the object references to the current game information,
 * and updates the game information based off the move
 * 
 * The game information is contained within three objects - the 12 element array
 * of piece bitboards, a 64 element int array representing the piece types on each
 * of the 64 squares, and then a byte for game flags
 * 
 */
public byte updateGame(int[] move, int[] board, long[] PIECEBOARDS, byte flags){
    
    
    int piece =         move[0];
    int capturedPiece = move[1];
    int fromSq =        move[2];
    int toSq =          move[3];
    int moveFlags =     move[4];
    
    int colour = piece < 6 ? WHITE : BLACK;

    PIECEBOARDS[piece] &= (~(1L << fromSq)); //Remove moving piece from old square
    board[fromSq] = empty;
    
    if((moveFlags & Constants.moveFlagPromotion) != 0){ //Pawn Promotion
        
        PIECEBOARDS[moveFlags & Constants.moveFlagPromotedPiece] |= (1L << toSq); //Add promoted piece to new square
        board[toSq] = moveFlags & Constants.moveFlagPromotedPiece;
    }
    else if((moveFlags & Constants.moveFlagEnPassant) != 0){ //En Passant
        
        int[] otherPiece = new int[]{bPawn, wPawn};
        int[] sqDelta = new int[]{-8, 8};
        
        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[otherPiece[colour]] &= (~(1L << (toSq + sqDelta[colour]))); //remove captured piece
        board[toSq + sqDelta[colour]] = empty;
        board[toSq] = piece;
    }
    else if((moveFlags & Constants.moveFlagKingSideCastle) != 0){ //King-side Castle
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{5, 61};
        int[] oldSq = new int[]{7, 63};

        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        PIECEBOARDS[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else if((moveFlags & Constants.moveFlagQueenSideCastle) != 0){ //Queen-side Castle
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{3, 59};
        int[] oldSq = new int[]{0, 56};

        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        PIECEBOARDS[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        PIECEBOARDS[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else{
        PIECEBOARDS[piece] |= (1L << toSq); //add piece to new square
        board[toSq] = piece;
    }
    
    if(capturedPiece != empty) {
            PIECEBOARDS[capturedPiece] &= (~(1L << toSq)); //remove captured piece
    }
    
    if((flags & 0b11110000) != 0){ //If castles are still possible, check if move removes castle eligibility
        if(colour == WHITE){
            if(piece == wKing){
                flags &= 0b00111111;
            }
            else if(piece == wRook && fromSq == 0){
                flags &= 0b10111111;
            }
            else if(piece == wRook && fromSq == 7){
                flags &= 0b01111111;
            }
            
        }
        else{
            if(piece == bKing){
                flags &= 0b11001111;
            }
            else if(piece == bRook && fromSq == 56){
                flags &= 0b11101111;
            }
            else if(piece == bRook && fromSq == 63){
                flags &= 0b11011111;
            } 
        }
    }
    flags &= 0b11110000; // clear en-passant flags from last move
    
    if((piece == wPawn || piece == bPawn) && Math.abs(toSq - fromSq) == 16){ //en passant possible
        flags = (byte) (flags | (toSq%8 << 1) | 1);
    }
    
    return flags;
    
}

/**
 * Checks if given colour is in check currently. Returns true if in check
 * 
 * Generates all moves for the opposing colour, and checks if the King is a possible target for capture
 */
public boolean inCheck(int colour, int[] board, long[] PIECEBOARDS, byte flags){

int[] moves = generateAllMoves(1 - colour, board, PIECEBOARDS, flags);

for(int move : moves){
    if( ((move << 4) >>> 28) == (colour*6 + 5)) return true;
}

return false;

}

/**
 * For castling, needs to satisfy several conditions. Returns true if castle is possible for given colour/side.
 * 
 * Conditions:
 * 1. No pieces can be located on the squares between the castle and rook piece
 * 2. The king cannot pass over any potential checks, between its square and the destination square. It also
 * cannot be in check currently.
 * 
 * The third condition, that the king and rook have not moved yet, is kept track of in the game flags and is checked
 * in addition to the checks this function performs.
 */
public boolean castleIsLegal(int colour, int[] board, long[] PIECEBOARDS, byte flags, boolean queenSide){
    
if(inCheck(colour, board, PIECEBOARDS, flags)){ return false;}

int[] tempBoard = Arrays.copyOf(board, 64);
long[] tempPieceBoards = Arrays.copyOf(PIECEBOARDS, 12);

tempBoard[initKingPos[colour]] = empty;

if(queenSide){
    
    for(int i = 1; i <= 2; i++){
        tempBoard[initKingPos[colour]-i] = king[colour];
        tempPieceBoards[king[colour]] <<= 1;
        
        if(inCheck(colour, tempBoard, tempPieceBoards, flags)){ return false;}
        
        tempBoard[initKingPos[colour]-i] = empty;
    }
}
else{
    
    for(int i = 1; i <= 2; i++){
        tempBoard[initKingPos[colour]+i] = king[colour];
        tempPieceBoards[king[colour]] >>= 1;
        
        if(inCheck(colour, tempBoard, tempPieceBoards, flags)){ return false;}
        
        tempBoard[initKingPos[colour]+i] = empty;
    }
    
}
 
return true;
}

//--------------------------------------------------------------------------------------------






//--Move generation---------------------------------------------------------------------------

/**
 * Top level function for calling the individual move generation functions for each piece type. Returns
 * an array of ints representing all the possible moves.
 * 
 */
public int[] generateAllMoves(int colour, int[] board, long[] PIECEBOARDS, byte flags){

ArrayList<Integer> possibleMoves = new ArrayList();

if(colour == WHITE){

    generatePawnTargets(possibleMoves, colour, wPawn, PIECEBOARDS[wPawn], board, PIECEBOARDS, flags);
    generateKnightTargets(possibleMoves, colour, wKnight, PIECEBOARDS[wKnight], board, PIECEBOARDS);
    generateKingTargets(possibleMoves, colour, wKing, PIECEBOARDS[wKing],  board, PIECEBOARDS, flags);
    generateRookTargets(possibleMoves, colour, wRook, PIECEBOARDS[wRook],  board, PIECEBOARDS);
    generateBishopTargets(possibleMoves, colour, wBishop, PIECEBOARDS[wBishop],  board, PIECEBOARDS);
    generateQueenTargets(possibleMoves, colour, wQueen, PIECEBOARDS[wQueen],  board, PIECEBOARDS);

}
else{
    
    generatePawnTargets(possibleMoves, colour, bPawn, PIECEBOARDS[bPawn], board, PIECEBOARDS, flags);
    generateKnightTargets(possibleMoves, colour, bKnight, PIECEBOARDS[bKnight], board, PIECEBOARDS);
    generateKingTargets(possibleMoves, colour, bKing, PIECEBOARDS[bKing], board, PIECEBOARDS, flags);
    generateRookTargets(possibleMoves, colour, bRook, PIECEBOARDS[bRook], board, PIECEBOARDS);
    generateBishopTargets(possibleMoves, colour, bBishop, PIECEBOARDS[bBishop], board, PIECEBOARDS);
    generateQueenTargets(possibleMoves, colour, bQueen, PIECEBOARDS[bQueen], board, PIECEBOARDS);
 
}

int[] movesArray = new int[possibleMoves.size()];

for(int i = 0; i < movesArray.length; i++){
    movesArray[i] = possibleMoves.get(i);
}

return movesArray;

}

/**
 * Generates all possible pawn targets and invokes the move generation function to add the moves to the moves List, which is passed aa an input argument
 */
public void generatePawnTargets(ArrayList moves, int Colour, int pieceType, long pawns, int[] board, long[] PIECEBOARDS, byte flags){
    
    if(pawns == 0) return;
    
    long pawnPush, pawnDoublePush, promotions, attackTargets, attacks, epAttacks, promotionAttacks;
    
    int[] pushDiff = new int[]{8, 64 - 8};	//push one forward
    int[][] attackDiff = new int[][]{{7, 64-9}, {9,64-7}}; //push one forward and one left or right
   
    
    long[] fileMask = new long[]{~Constants.FILE_H, ~Constants.FILE_A};
    long[] promotionMask = new long[]{Constants.RANK_8, Constants.RANK_1};
    long[] doublePushMask = new long[]{Constants.RANK_3,Constants.RANK_6};
    long[] enPassantMask = new long[]{Constants.RANK_6, Constants.RANK_3};
    
    int diff = pushDiff[Colour];
    
    //Build a bitboard representing all free, unoccupied squares on the board
    long freeSquares = Constants.ALL_SET; 
    for(long piece : PIECEBOARDS){
        freeSquares &= (~piece);
    }
    
    //build a bitboard representing all enemy pieces on the board
    long enemyPieces = 0;
    for(int j = (pieceType + 6)%12; j < (pieceType + 6)%12 + 6; j++){
        enemyPieces |= PIECEBOARDS[j];
    }

    
    long enPassantTargetSquare = Constants.FILES[(flags & 0b00001110) >>> 1] & enPassantMask[Colour];
    
    //Single Pushes
    pawnPush = circularLeftShift(pawns, diff) & freeSquares;
    generatePawnMoves(pawnPush, diff, pieceType, moves, 0, board);
    
    //Promotions
    promotions = pawnPush & promotionMask[Colour];
    generatePawnPromotionMoves(promotions, diff, pieceType, moves, board);
    
    //Double Pushes
    pawnDoublePush = circularLeftShift(pawnPush & doublePushMask[Colour],diff) & freeSquares;
    generatePawnMoves(pawnDoublePush, diff+diff, pieceType, moves, 0, board);
    
    //Attacks
    for(int dir = 0; dir < 2; dir++){
        
        diff = attackDiff[dir][Colour];
        attackTargets = circularLeftShift(pawns, diff) & fileMask[dir];
        
        //Simple Attacks
        attacks = attackTargets & enemyPieces;
        generatePawnMoves(attacks, diff, pieceType, moves, 0, board);
        
        //En Passant Attacks
        epAttacks = attackTargets & enPassantTargetSquare;
        if((flags & 1) == 1){
            generatePawnMoves(epAttacks, diff, pieceType, moves, Constants.moveFlagEnPassant, board);
        }
        
        //Promotion Attacks
        promotionAttacks = attacks & promotionMask[Colour];
        generatePawnPromotionMoves(promotionAttacks, diff, pieceType, moves, board);
    }
    
}

/**
 * Generates Knight moves using the Constants.KnightMoves pre-generated bitboards
 * 
 */
public void generateKnightTargets(ArrayList moves, int Colour, int pieceType, long knights, int[] board, long[] PIECEBOARDS){
    
    if(knights == 0) return;
    
    long targets;
    
    long friendlyPieces = 0;
    for(int j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(knights > 0){
        int fromSq = Long.numberOfTrailingZeros(knights);
        targets = Constants.KnightMoves[fromSq] & ~friendlyPieces;
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        knights &= knights - 1;
    }
    
}

/**
 * Generates King moves using the Constants.KingMoves pre-generated bitboards. Checks for castling ability as well.
 * 
 */
public void generateKingTargets(ArrayList moves, int Colour, int pieceType, long king, int[] board, long[] PIECEBOARDS, byte flags){
    
    if(king == 0) return;
    
    long targets;
    
    long allPieces = 0;
    for(int i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }

    long friendlyPieces = 0;
    for(int j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    int fromSq = Long.numberOfTrailingZeros(king);
    targets = Constants.KingMoves[fromSq] & ~friendlyPieces;
    generateMoves(fromSq, targets, pieceType, moves, 0, board);
    
    if(Colour == WHITE){
        if((flags & (1 << 6)) != 0 && (allPieces & Constants.queenCastleSquares[WHITE]) == 0){
            generateMoves(fromSq, 0x4L, pieceType, moves, Constants.moveFlagQueenSideCastle, board);
        }
        if((flags & (1 << 7)) != 0 && (allPieces & Constants.kingCastleSquares[WHITE]) == 0){
            generateMoves(fromSq, 0x40L, pieceType, moves, Constants.moveFlagKingSideCastle, board);
        }  
    }
    else{
        if((flags & (1 << 4)) != 0 && (allPieces & Constants.queenCastleSquares[BLACK]) == 0){
            generateMoves(fromSq, 0x400000000000000L, pieceType, moves, Constants.moveFlagQueenSideCastle, board);
        }
        if((flags & (1 << 5)) != 0 && (allPieces & Constants.kingCastleSquares[BLACK]) == 0){
            generateMoves(fromSq, 0x4000000000000000L, pieceType, moves, Constants.moveFlagKingSideCastle, board);
        }
    }
}

/**
 * Generates rook moves using magic bitboards. See Constants class for more info.
 */
public void generateRookTargets(ArrayList moves, int Colour, int pieceType, long rooks, int[] board, long[] PIECEBOARDS){

    if(rooks == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(rooks != 0){
        fromSq = Long.numberOfTrailingZeros(rooks);

        index = (int) (((allPieces & Constants.RookMaskOnSquare[fromSq])*
                    Constants.magicNumberRook[fromSq]) >>>
                    Constants.magicShiftsRook[fromSq]);

        targets = Constants.magicMovesRook[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        rooks &= rooks -1;
    }
}

/**
 * Generates bishop moves using magic bitboards. See Constants class for more info.
 */
public void generateBishopTargets(ArrayList moves, int Colour, int pieceType, long bishops, int[] board, long[] PIECEBOARDS){

    if(bishops == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(bishops != 0){
        fromSq = Long.numberOfTrailingZeros(bishops);

        index = (int) (((allPieces & Constants.BishopMaskOnSquare[fromSq])*
                    Constants.magicNumberBishop[fromSq]) >>>
                    Constants.magicShiftsBishop[fromSq]);

        targets = Constants.magicMovesBishop[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        bishops &= bishops -1;
    }
}

/**
 * Generates queen moves using magic bitboards. Uses a combination of bishop magic bitboards and rook magic bitboardsd. 
 * See Constants class for more info.
 */
public void generateQueenTargets(ArrayList moves, int Colour, int pieceType, long queens, int[] board, long[] PIECEBOARDS){
    
    if(queens == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, rookIndex, bishopIndex;
    
    allPieces = 0;
    for(i = 0; i < 12; i++){
        allPieces |= PIECEBOARDS[i];
    }
    
    friendlyPieces = 0;
    for(j = Colour*6; j < Colour*6 + 6; j++){
        friendlyPieces |= PIECEBOARDS[j];
    }
    
    while(queens != 0){
        fromSq = Long.numberOfTrailingZeros(queens);

        rookIndex = (int) (((allPieces & Constants.RookMaskOnSquare[fromSq])*
                    Constants.magicNumberRook[fromSq]) >>>
                    Constants.magicShiftsRook[fromSq]);
        
        bishopIndex = (int) (((allPieces & Constants.BishopMaskOnSquare[fromSq])*
                    Constants.magicNumberBishop[fromSq]) >>>
                    Constants.magicShiftsBishop[fromSq]);

        targets = (Constants.magicMovesBishop[fromSq][bishopIndex] | Constants.magicMovesRook[fromSq][rookIndex])
                  & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, board);
        
        queens &= queens -1;
    }
}


/**
 * Receives a bitboard representing all the target (destination) squares for a given piece, and parses the bitboard one-by-one
 * to generate individual moves, encoded as integers, and adds them to a List of moves which is an input argument.
 */
public void generateMoves(int from, long Targets, int pieceType, ArrayList moveList, int flags, int[] board){
    
    while(Targets != 0){ //while bits are still set in the target bitboard
        
        int toSq = Long.numberOfTrailingZeros(Targets); //Get square index by computing the position of the the least significant bit set in the long
        int capture = board[toSq]; //Return piece occupying that square, if any. Will be empty (0xE) if no piece occupies the square.
        int move = pieceType << 28 | capture << 24 | from << 16 | toSq << 8 | flags; //Encode the move information in an int
        moveList.add(move); //Add to the ArrayList Moves
        Targets &= Targets - 1; //Mask out the least significant bit before repeating the loop again
        
        
    }
}

/**
 * Separate method for generating pawn moves. Similar to the generateMoves method but uses the restricted pawn move rules to generate the starting square dynamically
 */
public void generatePawnMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList, int flags, int[] board){
    
    while(Targets != 0){
        
        int toSq = Long.numberOfTrailingZeros(Targets);
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8 | flags;
        moveList.add(move);
        Targets &= Targets - 1;
        
    }
}

/**
 * Adds the four distinct options available when a pawn is promoted as possible moves to be considered
 * 
 */
public void generatePawnPromotionMoves(long Targets, int moveDiff, int pieceType, ArrayList moveList, int[] board){
    
    while(Targets != 0){
        int toSq = Long.numberOfTrailingZeros(Targets);
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8;
        
        if(pieceType >= bPawn){
            moveList.add(move | 0b10111); //Rook
            moveList.add(move | 0b11000); //Knight
            moveList.add(move | 0b11001); //Bishop
            moveList.add(move | 0b11010); //Queen
        }
        else{
            moveList.add(move | 0b10001); //Rook
            moveList.add(move | 0b10010); //Knight
            moveList.add(move | 0b10011); //Bishop
            moveList.add(move | 0b10100); //Queen
        }
        Targets &= Targets - 1;
        
    }
}

//-------------------------------------------------------------------------------------------




//--Tools for move generation----------------------------------------------------------------

/*Allows for pushing pawns either forward one rank, or backwards one rank, just by adjusting the shift
 * value
 */
public long circularLeftShift(long bitBoard, int shift){
    return bitBoard << shift | bitBoard >> (64 - shift);
}

/**
 * Returns the square index given a 2-element int array representing the row and column indices
 */
public int getIndex(int[] Position){
    int row = Position[0];
    int column = Position[1];
    return 8*row + column;
}

/*
 * Returns the number of bits set in a given long
 */
public int numSet(long l){
    return Long.bitCount(l);
}

//-------------------------------------------------------------------------------------------





//--Methods for JoeFlowPlaysChess Class-------------------------------------------------------

/**
 * Called by the user side class to update the chess engine game information once a user has made
 * a move
 */
public void makeMove(int[] oldPos, int[] newPos, int moveFlags){

int oldIndex = getIndex(oldPos);
int newIndex = getIndex(newPos);

int piece =          gameBoard[oldIndex];
int capturedPiece =  gameBoard[newIndex];
int fromSq =         oldIndex;
int toSq =           newIndex;

int move = piece << 28 | capturedPiece << 24 | fromSq << 16 | toSq << 8 | moveFlags;

int[] moveInfo = parseMove(move);

gameFlags = updateGame(moveInfo, gameBoard, gamePieceBoards, gameFlags);
 
}

/**
 * Generate a complete list of legal moves for white
 */
public int[] whiteLegalMoves(){
    
ArrayList<Integer> legalList = new ArrayList();
int[] whiteMoves, legalMoves, tempBoard;
long[] tempPieceBoards;
byte tempFlags;

whiteMoves = generateAllMoves(WHITE, gameBoard, gamePieceBoards, gameFlags);

for(int wmove : whiteMoves){
    
    tempBoard = Arrays.copyOf(gameBoard, 64);
    tempPieceBoards = Arrays.copyOf(gamePieceBoards, 12);
    tempFlags = gameFlags;
    
    tempFlags = updateGame(parseMove(wmove), tempBoard, tempPieceBoards, tempFlags);
    
    if(!inCheck(WHITE, tempBoard, tempPieceBoards, tempFlags)){ //If move doesn't leave white in check
        legalList.add(wmove);									//then Add move
    } 
}

//Convert List to array
legalMoves = new int[legalList.size()];
for(int i = 0; i < legalMoves.length; i++){
    legalMoves[i] = legalList.get(i);
}

return legalMoves;

}

/**
 * Check that castle is legal for white
 */
public boolean castleIsLegal(boolean queenSide){


return castleIsLegal(WHITE, gameBoard, gamePieceBoards, gameFlags, queenSide);

}

//-------------------------------------------------------------------------------------------

//--Methods for UCI Interface

public String bestMove() {
return "";	
}

public int convertFENtoMoveInt(String fen){
	
	byte flags = 0;
	String startPos = fen.substring(0, 2);
	String endPos = fen.substring(2, 4);
	
	
	int[] startSq = ANtoArrayIndex(Integer.parseInt(startPos.substring(1)), startPos.charAt(0));
	int[] endSq = ANtoArrayIndex(Integer.parseInt(endPos.substring(1)), endPos.charAt(0));
	
	int startInd = getIndex(startSq);
	int endInd = getIndex(endSq);
	
	if ((startInd == 4  && endInd == 6  && gameBoard[4]  == wKing) || 
		(startInd == 60 && endInd == 62 && gameBoard[60] == bKing)) {
		flags |= Constants.moveFlagKingSideCastle;
	}
	else if ((startInd == 4  && endInd == 2  && gameBoard[4]  == wKing) || 
			 (startInd == 60 && endInd == 58 && gameBoard[60] == bKing)) {
		flags |= Constants.moveFlagQueenSideCastle;
	}
	else if(gameBoard[startInd] == wPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 5 && endSq[1] == (gameFlags & Constants.gameFlagEnPassantMask)){
		flags |= Constants.moveFlagEnPassant;
    }
	else if(gameBoard[startInd] == bPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 2 && endSq[1] == (gameFlags & Constants.gameFlagEnPassantMask)){
		flags |= Constants.moveFlagEnPassant;
    }

	if(fen.length() > 4){
		String promotionType = fen.substring(4).toLowerCase();
		flags |= Constants.moveFlagPromotion;
		
		int promotionPiece;
		
		switch(promotionType){
		
		case "n":
		case "k":
			promotionPiece = wKnight;
			break;
		
		case "b":
			promotionPiece = wBishop;
			break;
			
		case "r":
			promotionPiece = wRook;
			break;
			
		default: //queen
			promotionPiece = wQueen;
			break;
		}
		
		if(gameBoard[startInd] > 5) { promotionPiece += 6;} //SWITCH TO BLACK
		
		flags |= promotionPiece;
	}

	int oldIndex = getIndex(startSq);
	int newIndex = getIndex(endSq);

	return gameBoard[oldIndex] << 28 | gameBoard[newIndex] << 24 |
		   oldIndex << 16 | newIndex << 8 | flags;
}

public void parseFENAndUpdate(String fen) {

	String[] fields = fen.split(" +");
	
	String pieces =						 fields[0];
	String colourToMove =				 fields[1];
	String castling = 					 fields[2];
	String enPassantSquare = 			 fields[3];
	String halfmovesSinceCaptureOrPawn = fields[4];
	String moveNumber = 				 fields[5];
	
	setBoardPositions(pieces);
	turn = colourToMove.equals("w") ? WHITE : BLACK;
	setCastlingRights(castling);
	setEnPassantFlag(enPassantSquare);
	
	
}

public void makeANMove(String move) {
	
    // piece(4) | capturedPiece{4} | fromSq(8) | toSq(8) | flags(8)
    /* flags : bits 1-4: promoted piece type (Knight, Rook, Bishop, Queen)
               bit 5: promotion flag
               bit 6: en-passant capture flag
               bit 7: Queen Side Castle
               bit 8: King Side Castle
    */
	byte flags = 0;
	String startPos = move.substring(0, 2);
	String endPos = move.substring(2, 4);
	
	
	int[] startSq = ANtoArrayIndex(Integer.parseInt(startPos.substring(1)), startPos.charAt(0));
	int[] endSq = ANtoArrayIndex(Integer.parseInt(endPos.substring(1)), endPos.charAt(0));
	
	int startInd = getIndex(startSq);
	int endInd = getIndex(endSq);
	
	if ((startInd == 4  && endInd == 6  && gameBoard[4]  == wKing) || 
		(startInd == 60 && endInd == 62 && gameBoard[60] == bKing)) {
		flags |= Constants.moveFlagKingSideCastle;
	}
	else if ((startInd == 4  && endInd == 2  && gameBoard[4]  == wKing) || 
			 (startInd == 60 && endInd == 58 && gameBoard[60] == bKing)) {
		flags |= Constants.moveFlagQueenSideCastle;
	}
	else if(gameBoard[startInd] == wPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 5 && endSq[1] == (gameFlags & Constants.gameFlagEnPassantMask)){
		flags |= Constants.moveFlagEnPassant;
    }
	else if(gameBoard[startInd] == bPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 2 && endSq[1] == (gameFlags & Constants.gameFlagEnPassantMask)){
		flags |= Constants.moveFlagEnPassant;
    }

	if(move.length() > 4){
		String promotionType = move.substring(4).toLowerCase();
		flags |= Constants.moveFlagPromotion;
		
		int promotionPiece;
		
		switch(promotionType){
		
		case "n":
		case "k":
			promotionPiece = wKnight;
			break;
		
		case "b":
			promotionPiece = wBishop;
			break;
			
		case "r":
			promotionPiece = wRook;
			break;
			
		default: //queen
			promotionPiece = wQueen;
			break;
		}
		
		if(gameBoard[startInd] > 5) { promotionPiece += 6;} //SWITCH TO BLACK
		
		flags |= promotionPiece;
	}
	
	makeMove(startSq, endSq, flags);
	
}

private void setBoardPositions(String fen) {
	int sq = 56;
	
	char c;
	int i = 0;
	int len = fen.length();
	while(i < len) {
		
		c = fen.charAt(i);
		
		if(Character.isLetter(c)){
			int piece = pieceNum(c);
	        gamePieceBoards[piece] = gamePieceBoards[piece] | (1L << sq);
	        gameBoard[sq] = piece;
	        sq ++;
		}
		else if(Character.isDigit(c)){
			sq += Integer.parseInt(String.valueOf(c));
		}
		else if(c == '/'){
			sq -= 15;
		}
		i++;
	}
	
}

private void setCastlingRights(String fen) {
	
	/*
	bit 5: Black Queen Side Castle possible (Rook on sqaure 56)
	bit 6: Black King Side Castle possible  (Rook on square 63)
	bit 7: White Queen Side Castle possible (Rook on square 0)
	bit 8: White King Side Castle possible  (Rook on square 7)  
	*/
	for(int i = 0; i < fen.length(); i++){
		switch(fen.charAt(i)){
		
		case '-':
			gameFlags &= 0b00001111;
			break;
			
		case 'K':
			gameFlags |= 0b10000000;
			break;
			
		case 'Q':
			gameFlags |= 0b01000000;	
			break;

		case 'k':
			gameFlags |= 0b00100000;	
			break;
			
		case 'q':
			gameFlags |= 0b00010000;	
			break;
		
		}
	}
}

private void setEnPassantFlag(String fen){
	/*
	bit 1: En Passant is possible, there was a pawn double pushed on the last turn
	bits 2-4: The file number (0-7) that a pawn was double pushed to on the last turn
	*/
	if(fen.equals("-")){
		gameFlags &= 0b11110000;
	}
	else {
		int file = fen.charAt(0) - 97;
		gameFlags = (byte) (gameFlags | (file << 1) | 1);
	}
	
}

private int[] ANtoArrayIndex(int Row, char Col){
    
    return new int[]{Row - 1, (int) Col - 97};
}

private int pieceNum(char c){
	
	char[] FENPieces = {'P', 'K', 'B', 'R', 'Q', 'K', 'p', 'k', 'b', 'r', 'q', 'k'};
	return Arrays.binarySearch(FENPieces, 0, 11, c);
	

}




//--Debugging Functions----------------------------------------------------------------------

public void printAsBitBoard(long l){
    
    String bits = Long.toBinaryString(l);
    
    for(int j = 0; j < Long.numberOfLeadingZeros(l); j++){
        bits = "0"+bits;
    }

    for(int i = 0; i < 8; i++){
        System.out.println(new StringBuilder(bits.substring(i*8, i*8+8)).reverse().toString());
        
    }
}

public void printBoardArray(int[] b){
    
    for(int i = 56; i > -1; i-=8){
        System.out.println(b[i] + "  " + b[i+1] + "  " +
                         b[i+2] + "  " + b[i+3] + "  " +
                         b[i+4] + "  " + b[i+5] + "  " +
                         b[i+6] + "  " + b[i+7] + "  ");  
    }
}

public void printMovesAsStrings(int[] moves){
    
    int piece, capturedPiece, fromSq, toSq, moveFlags, fromRow, fromCol, toRow, toCol;
    String[] pieceTypes = {"wPawn", "wKnight", "wBishop", "wRook", "wQueen", "wKing",
                           "bPawn", "bKnight", "bBishop", "bRook", "bQueen", "bKing", "", "", "None"};
    String[] rowNumber = {"1", "2", "3", "4", "5", "6", "7", "8"};
    String[] colNumber = {"a", "b", "c", "d", "e", "f", "g", "h"};
    
    String moveStr;
    
    for(int move : moves){
        piece =          move >>> 28;
        capturedPiece = (move << 4) >>> 28;
        fromSq =        (move << 8) >>> 24;
            fromRow = (int)(fromSq-fromSq%8)/8;
            fromCol = fromSq%8;
        toSq =          (move << 16) >>> 24;
            toRow = (int)(toSq-toSq%8)/8;
            toCol = toSq%8;
        moveFlags = (int)(byte) move;
        
        moveStr = "Piece: " + pieceTypes[piece] + ", Captured: " + pieceTypes[capturedPiece] +
                  ", From: " + colNumber[fromCol]+rowNumber[fromRow] + ", To: " +
                  colNumber[toCol]+rowNumber[toRow];
        if((moveFlags & Constants.moveFlagPromotion) != 0){
            moveStr += ", PROMOTION";
        }
        else if((moveFlags & Constants.moveFlagEnPassant) != 0){
            moveStr += ", EN PASSANT";
        }
        else if((moveFlags & Constants.moveFlagKingSideCastle) != 0){
            moveStr += ", KING-SIDE CASTLE";
        }
        else if((moveFlags & Constants.moveFlagQueenSideCastle) != 0){
            moveStr += ", QUEEN-SIDE CASTLE";
        }
        
        System.out.println(moveStr);
    }
}

public boolean isDebugMode() {
	return debugMode;
}

public void setDebugMode(boolean debugMode) {
	this.debugMode = debugMode;
}

public boolean isInitialized() {
	return initialized;
}

public void setInitialized(boolean initialized) {
	this.initialized = initialized;
}

//------------------------------------------------------------------------------------------


}