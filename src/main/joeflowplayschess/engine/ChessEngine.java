/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess.engine;

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
	private GameState gameState;
	private int 	  nodeCount;
	private Constants Constants;
	private int 	  turn;
	
	//declarations + initializations
	private static int WHITE = 			0;
	private static int BLACK =			1;    
	    
	private static int wPawn = 			0;
	private static int wKnight = 		1;
	private static int wBishop = 		2;
	private static int wRook = 			3;
	private static int wQueen = 		4;
	private static int wKing = 			5;
	
	private static int bPawn = 			6;
	private static int bKnight =		7;
	private static int bBishop =		8;
	private static int bRook = 			9;
	private static int bQueen = 		10;
	private static int bKing = 			11;
	
	private static int empty = 			0xE;
	
	private static int[] king = 		new int[]{wKing, bKing};
	
	private static int defaultDepth = 	3;
	
	Random r = 						new Random();
	boolean debugMode = 			true;
	boolean initialized =			false;
	boolean firstgame = 			true;

public ChessEngine(){}

public void init(){
	
	if(!initialized){
		if(firstgame){
			setConstants(new Constants());
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
		turn = WHITE;
		gameState = new GameState(true, getConstants());  //set up board for beginning of game
		initialized = true;
	}
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
	
	bestMove = chooseBestMove(gameState, colour, searchDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);
	
	return checkForMateAndUpdateBoard(colour, searchDepth, bestMove);

}

public int[] selectMoveRestricted(String[] FENMoves){

	int[] bestMove, moveInfo;
	
	nodeCount = 0;
	
	bestMove = chooseBestMoveRestricted(gameState, 
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
	updateGame(moveInfo, gameState);
	
	//Print some relevant infomation to the console
	if(debugMode) {
		System.out.println("Nodes traversed: " + nodeCount);
		System.out.println("Best Move Score For Black:");
		System.out.println(bestMove[1]);
		System.out.println("Game Flags:");
		System.out.println(Integer.toBinaryString(Byte.toUnsignedInt(gameState.getFlags())));
		printBoardArray(gameState.getBoard());
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
private int[] chooseBestMove(GameState gState, int colour, int depth, int alpha, int beta){

	int 	i;
	int[] 	moves, moveScore, bestMove, parsedMove;
	int[][] parsedMoves, parsedMoves1;
	
	boolean check = false;
	boolean legal = true;
	
	ArrayList<int[]> bestMoves = new ArrayList();
	
	moves = generateAllMoves(colour, gState);
	
	if(inCheck(colour, gState)){
	    check = true;
	}
	
	for(i = 0; i < moves.length; i++){
	    
	    nodeCount++;
	    GameState tState = gState.copy();
	    
	    parsedMove = parseMove(moves[i]);
	    
	    updateGame(parsedMove,tState);
	    
	    if(!check && isCastleMove(parsedMove, colour)){
	        legal = castleIsLegal(colour, tState, parsedMove[3] == getConstants().queenSideCastleDestinationSquare[colour]);
	    }
	    
	    
	    if(legal && !inCheck(colour, tState)){
	        
	        if(depth > 0){
	        	//Call chooseBestMove on the resulting board position after move is made, from the opposing players view	
	            int[] theirMove = chooseBestMove(tState, 1-colour, depth - 1, alpha, beta);
	            moveScore = new int[]{moves[i], theirMove[1]};   
	        }
	        else{
	        	//Evaluate board position if at the terminal depth
	            moveScore = new int[]{moves[i], evaluateGameScore(tState)};
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

private int[] chooseBestMoveRestricted(GameState gState, int colour, int depth, int alpha, int beta, int[] movesToSearch){

	int 	i;
	int[] 	moves, moveScore, bestMove, parsedMove;
	int[][] parsedMoves, parsedMoves1;
	
	boolean check = false;
	boolean legal = true;
	
	ArrayList<int[]> bestMoves = new ArrayList();
	
	moves = movesToSearch;
	
	if(inCheck(colour, gState)){
	    check = true;
	}
	
	for(i = 0; i < moves.length; i++){
	    
	    nodeCount++;
	    GameState tState = gState.copy();
	    
	    parsedMove = parseMove(moves[i]);
	    
	    updateGame(parsedMove,tState);
	    
	    if(!check && isCastleMove(parsedMove, colour)){
	        legal = castleIsLegal(colour, tState, parsedMove[3] == getConstants().queenSideCastleDestinationSquare[colour]);
	    }
	    
	    if(legal && !inCheck(colour, tState)){
	        
	        if(depth > 0){
	        	//Call chooseBestMove on the resulting board position after move is made, from the opposing players view	
	            int[] theirMove = chooseBestMove(tState, 1-colour, depth - 1, alpha, beta);
	            moveScore = new int[]{moves[i], theirMove[1]};   
	        }
	        else{
	        	//Evaluate board position if at the terminal depth
	            moveScore = new int[]{moves[i], evaluateGameScore(tState)};
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
public int evaluateGameScore(GameState state){

	int overallScore;
	
	long[] pieceBoards = state.getPieceBoards();
	
	
	overallScore = 2*(numSet(pieceBoards[bPawn]) - numSet(pieceBoards[wPawn])) +
	                    7*(numSet(pieceBoards[bKnight]) - numSet(pieceBoards[wKnight])) +
	                    6*(numSet(pieceBoards[bBishop]) - numSet(pieceBoards[wBishop])) +   
	                    10*(numSet(pieceBoards[bRook]) - numSet(pieceBoards[wRook])) +
	                    18*(numSet(pieceBoards[bQueen]) - numSet(pieceBoards[wQueen]));  
	
	overallScore = 2*(overallScore) + centreControlScore(pieceBoards);
	
	overallScore = 3*(overallScore) + 2*(pawnStructureScore(bPawn) - pawnStructureScore(wPawn));
	
	overallScore = 2*(overallScore) + positionScore(BLACK, state) - positionScore(WHITE, state);
	
	return overallScore;

}

//---------------------------------------------------------------------------------------------




//-- Board Evaluation Heuristics---------------------------------------------------------------

/**
 * Advancement score values pieces in ranks further away from their side. It is the smallest
 * weighted heuristic, but it tends to ensure black is aggressive and plays an offensive strategy
 * 
 * Not used currently
 */

public int advancementScore(long[] currpieceBoards, int colour){
    
    int[] rankScore = new int[8];
    int advancement = 0;
    
    for(int i = 0; i < 8; i++){
        for(int j = colour*6; j < colour*6+6; j++){
            rankScore[i] += numSet(currpieceBoards[j] & getConstants().RANKS[i]);
        }
        
        advancement += rankScore[i]*(i + (7-i)*colour);
    }
    
    return advancement;
}

/**
 * Assigns a score to the number of pieces in the center of the board. Control of the center
 * squares is advantageous
 */
public int centreControlScore(long[] pieceBoards){
    
    long blackCentre = 0, whiteCentre = 0;
    
    for(int i = 0; i < 6; i++){
        whiteCentre |= (pieceBoards[i] & getConstants().CENTER_4);
    }
    for(int j = 6; j < 12; j++){
        blackCentre |= (pieceBoards[j] & getConstants().CENTER_4);
    }
    
    return numSet(blackCentre) - numSet(whiteCentre);
}

/**
 * Assigns a score to the amount of pieces you are currently attacking (and conversely the amount
 * of your pieces being attacked by the opposing player) as well as how many moves you have available
 * to choose from at that board position. Mobility is advantageous, generally.
 */
public int positionScore(int colour, GameState state){
    
    int[] yourMoves = generateAllMoves(colour, state);
    
    int piecesAttacked = (int) Arrays.stream(yourMoves).filter(m -> ((m << 4) >>> 28) > 0).count();
    
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
                if((pawns & getConstants().FILES[file-1]) == 0) { isos++;}
                break;
            case 0:
                if((pawns & getConstants().FILES[file+1]) == 0) { isos++;}
                break;
            default:
                if(((pawns & getConstants().FILES[file+1]) == 0) && ((pawns & getConstants().FILES[file-1]) == 0)) { isos++;}
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
 * Accepts a move, and the object reference to the current game information,
 * and updates the game information based off the move
 * 
 * The game information is contained within three objects - the 12 element array
 * of piece bitboards, a 64 element int array representing the piece types on each
 * of the 64 squares, and then a byte for game flags
 * 
 */
public void updateGame(int[] move, GameState state){
    
    
    int piece =         move[0];
    int capturedPiece = move[1];
    int fromSq =        move[2];
    int toSq =          move[3];
    int moveFlags =     move[4];
    
    long[] pieceBoards = state.getPieceBoards();
    int[] board = state.getBoard();
    byte flags = state.getFlags();
    
    int colour = piece < 6 ? WHITE : BLACK;

    pieceBoards[piece] &= (~(1L << fromSq)); //Remove moving piece from old square
    board[fromSq] = empty;
    
    if((moveFlags & getConstants().moveFlagPromotion) != 0){ //Pawn Promotion
        
        pieceBoards[moveFlags & getConstants().moveFlagPromotedPiece] |= (1L << toSq); //Add promoted piece to new square
        board[toSq] = moveFlags & getConstants().moveFlagPromotedPiece;
    }
    else if((moveFlags & getConstants().moveFlagEnPassant) != 0){ //En Passant
        
        int[] otherPiece = new int[]{bPawn, wPawn};
        int[] sqDelta = new int[]{-8, 8};
        
        pieceBoards[piece] |= (1L << toSq); //add piece to new square
        pieceBoards[otherPiece[colour]] &= (~(1L << (toSq + sqDelta[colour]))); //remove captured piece
        board[toSq + sqDelta[colour]] = empty;
        board[toSq] = piece;
    }
    else if((moveFlags & getConstants().moveFlagKingSideCastle) != 0){ //King-side Castle
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{5, 61};
        int[] oldSq = new int[]{7, 63};

        pieceBoards[piece] |= (1L << toSq); //add piece to new square
        pieceBoards[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        pieceBoards[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else if((moveFlags & getConstants().moveFlagQueenSideCastle) != 0){ //Queen-side Castle
        
        int[] rook = new int[]{wRook, bRook};
        int[] newSq = new int[]{3, 59};
        int[] oldSq = new int[]{0, 56};

        pieceBoards[piece] |= (1L << toSq); //add piece to new square
        pieceBoards[rook[colour]] |= (1L << newSq[colour]); // Add rook to new square
        pieceBoards[rook[colour]] &= (~(1L << oldSq[colour])); // Remove rook from old square
        board[toSq] = piece;
        board[oldSq[colour]] = empty;
        board[newSq[colour]] = rook[colour];
    }
    else{
        pieceBoards[piece] |= (1L << toSq); //add piece to new square
        board[toSq] = piece;
    }
    
    if(capturedPiece != empty) {
            pieceBoards[capturedPiece] &= (~(1L << toSq)); //remove captured piece
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
    
    state.setFlags(flags);
    
}

/**
 * Checks if given colour is in check currently. Returns true if in check
 * 
 * Generates all moves for the opposing colour, and checks if the King is a possible target for capture
 */
public boolean inCheck(int colour, GameState state){

int[] moves = generateAllMoves(1 - colour, state);

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

public boolean castleIsLegal(int colour, GameState state, boolean queenSide){

GameState tState = state.copy();

tState.getBoard()[getConstants().initKingPos[colour]] = empty;

if(queenSide){
	tState.getBoard()[getConstants().initKingPos[colour]-1] = king[colour];
    tState.getPieceBoards()[king[colour]] <<= 1;
    if(inCheck(colour, tState)){ return false;}
}
else{
	tState.getBoard()[getConstants().initKingPos[colour]+1] = king[colour];
	tState.getPieceBoards()[king[colour]] >>= 1;
    if(inCheck(colour, tState)){ return false;}
}

return true;

}

public boolean isCastleMove(int[] parsedMove, int colour) {
	return parsedMove[0] == king[colour] &&
		   parsedMove[2] == getConstants().initKingPos[colour] &&
		  (parsedMove[3] == getConstants().queenSideCastleDestinationSquare[colour] || parsedMove[3] == getConstants().queenSideCastleDestinationSquare[colour]);
}

//--------------------------------------------------------------------------------------------






//--Move generation---------------------------------------------------------------------------

/**
 * Top level function for calling the individual move generation functions for each piece type. Returns
 * an array of ints representing all the possible moves.
 * 
 */
public int[] generateAllMoves(int colour, GameState state){

ArrayList<Integer> possibleMoves = new ArrayList();

generatePawnTargets(possibleMoves, colour, wPawn + colour*6, state);
generateKnightTargets(possibleMoves, colour, wKnight + colour*6, state);
generateKingTargets(possibleMoves, colour, wKing + colour*6, state);
generateRookTargets(possibleMoves, colour, wRook + colour*6, state);
generateBishopTargets(possibleMoves, colour, wBishop + colour*6, state);
generateQueenTargets(possibleMoves, colour, wQueen + colour*6, state);


int[] movesArray = new int[possibleMoves.size()];

for(int i = 0; i < movesArray.length; i++){
    movesArray[i] = possibleMoves.get(i);
}

return movesArray;

}

/**
 * Generates all possible pawn targets and invokes the move generation function to add the moves to the moves List, which is passed aa an input argument
 */
public void generatePawnTargets(ArrayList moves, int colour, long pawns, GameState state){
    
    if(pawns == 0) return;
    
    long pawnPush, pawnDoublePush, promotions, attackTargets, attacks, epAttacks, promotionAttacks;
    
    int[] pushDiff = new int[]{8, 64 - 8};	//push one forward
    int[][] attackDiff = new int[][]{{7, 64-9}, {9,64-7}}; //push one forward and one left or right
   
    
    long[] fileMask = new long[]{~getConstants().FILE_H, ~getConstants().FILE_A};
    long[] promotionMask = new long[]{getConstants().RANK_8, getConstants().RANK_1};
    long[] doublePushMask = new long[]{getConstants().RANK_3, getConstants().RANK_6};
    long[] enPassantMask = new long[]{getConstants().RANK_6, getConstants().RANK_3};
    
    int diff = pushDiff[colour];
    
    //Build a bitboard representing all free, unoccupied squares on the board
    long emptySquares = state.getEmptySquares();
    
    //build a bitboard representing all enemy pieces on the board
    long enemyPieces = state.getEnemyPieces(colour);

    
    long enPassantTargetSquare = getConstants().FILES[(state.getFlags() & 0b00001110) >>> 1] & enPassantMask[colour];
    
    //Single Pushes
    pawnPush = circularLeftShift(pawns, diff) & emptySquares;
    generatePawnMoves(pawnPush, diff, colour, moves, 0, state.getBoard());
    
    //Promotions
    promotions = pawnPush & promotionMask[colour];
    generatePawnPromotionMoves(promotions, diff, colour, moves, state.getBoard());
    
    //Double Pushes
    pawnDoublePush = circularLeftShift(pawnPush & doublePushMask[colour],diff) & emptySquares;
    generatePawnMoves(pawnDoublePush, diff+diff, colour, moves, 0, state.getBoard());
    
    //Attacks
    for(int dir = 0; dir < 2; dir++){
        
        diff = attackDiff[dir][colour];
        attackTargets = circularLeftShift(pawns, diff) & fileMask[dir];
        
        //Simple Attacks
        attacks = attackTargets & enemyPieces;
        generatePawnMoves(attacks, diff, colour, moves, 0, state.getBoard());
        
        //En Passant Attacks
        epAttacks = attackTargets & enPassantTargetSquare;
        if((state.getFlags() & 1) == 1){
            generatePawnMoves(epAttacks, diff, colour, moves, getConstants().moveFlagEnPassant, state.getBoard());
        }
        
        //Promotion Attacks
        promotionAttacks = attacks & promotionMask[colour];
        generatePawnPromotionMoves(promotionAttacks, diff, colour, moves, state.getBoard());
    }
    
}

/**
 * Generates Knight moves using the Constants.KnightMoves pre-generated bitboards
 * 
 */
public void generateKnightTargets(ArrayList moves, int colour, long knights, GameState state){
    
    if(knights == 0) return;
    
    int pieceType = wKnight + (colour * 6);
    long friendlyPieces = state.getFriendlyPieces(colour);
    
    long targets;
    while(knights > 0){
        int fromSq = Long.numberOfTrailingZeros(knights);
        targets = getConstants().KnightMoves[fromSq] & ~friendlyPieces;
        generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());
        knights &= knights - 1;
    }
    
}

/**
 * Generates King moves using the Constants.KingMoves pre-generated bitboards. Checks for castling ability as well.
 * 
 */
public void generateKingTargets(ArrayList moves, int colour, long king, GameState state){
    
    if(king == 0) return;
    
    int pieceType = wKing + (colour * 6);
    
    long allPieces = state.getAllPieces();
    long friendlyPieces = state.getFriendlyPieces(colour);
    
    int fromSq = Long.numberOfTrailingZeros(king);
    
    byte flags = state.getFlags();
    int[] board = state.getBoard();
    
    long targets = getConstants().KingMoves[fromSq] & ~friendlyPieces;
    generateMoves(fromSq, targets, pieceType, moves, 0, board);

    if((flags & (1 << 6)) != 0 && (allPieces & getConstants().queenCastleSquares[colour]) == 0){
        generateMoves(fromSq, getConstants().queenSideCastleDestinationSquare[colour], pieceType, moves, getConstants().moveFlagQueenSideCastle, board);
    }
    if((flags & (1 << 7)) != 0 && (allPieces & getConstants().kingCastleSquares[colour]) == 0){
        generateMoves(fromSq, getConstants().kingSideCastleDestinationSquare[colour], pieceType, moves, getConstants().moveFlagKingSideCastle, board);
    }  
}

/**
 * Generates rook moves using magic bitboards. See Constants class for more info.
 */
public void generateRookTargets(ArrayList moves, int colour, long rooks, GameState state){

    if(rooks == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    int pieceType = wRook + (colour * 6);
    
    allPieces = state.getAllPieces();
    
    friendlyPieces = state.getFriendlyPieces(colour);
    
    while(rooks != 0){
        fromSq = Long.numberOfTrailingZeros(rooks);

        index = (int) (((allPieces & getConstants().RookMaskOnSquare[fromSq])*
                    getConstants().magicNumberRook[fromSq]) >>>
                    getConstants().magicShiftsRook[fromSq]);

        targets = getConstants().magicMovesRook[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());
        
        rooks &= rooks -1;
    }
}

/**
 * Generates bishop moves using magic bitboards. See Constants class for more info.
 */
public void generateBishopTargets(ArrayList moves, int colour, long bishops, GameState state){

    if(bishops == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, index;
    int pieceType = wBishop + (colour * 6);
    
    allPieces = state.getAllPieces();
    
    friendlyPieces = state.getFriendlyPieces(colour);
    
    while(bishops != 0){
        fromSq = Long.numberOfTrailingZeros(bishops);

        index = (int) (((allPieces & getConstants().BishopMaskOnSquare[fromSq])*
                    getConstants().magicNumberBishop[fromSq]) >>>
                    getConstants().magicShiftsBishop[fromSq]);

        targets = getConstants().magicMovesBishop[fromSq][index] & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());
        
        bishops &= bishops -1;
    }
}

/**
 * Generates queen moves using magic bitboards. Uses a combination of bishop magic bitboards and rook magic bitboardsd. 
 * See Constants class for more info.
 */
public void generateQueenTargets(ArrayList moves, int colour, long queens, GameState state){
    
    if(queens == 0) return;
    
    long allPieces, friendlyPieces, targets;
    int i, j, fromSq, rookIndex, bishopIndex;
    int pieceType = wQueen + (colour * 6);
    
    allPieces = state.getAllPieces();
    
    friendlyPieces = state.getFriendlyPieces(colour);
    
    while(queens != 0){
        fromSq = Long.numberOfTrailingZeros(queens);

        rookIndex = (int) (((allPieces & getConstants().RookMaskOnSquare[fromSq])*
                    getConstants().magicNumberRook[fromSq]) >>>
                    getConstants().magicShiftsRook[fromSq]);
        
        bishopIndex = (int) (((allPieces & getConstants().BishopMaskOnSquare[fromSq])*
                    getConstants().magicNumberBishop[fromSq]) >>>
                    getConstants().magicShiftsBishop[fromSq]);

        targets = (getConstants().magicMovesBishop[fromSq][bishopIndex] | getConstants().magicMovesRook[fromSq][rookIndex])
                  & (~friendlyPieces);
        generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());
        
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
public void generatePawnMoves(long Targets, int moveDiff, int colour, ArrayList moveList, int flags, int[] board){
    
	int pieceType = wPawn + (colour * 6);
	
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
public void generatePawnPromotionMoves(long Targets, int moveDiff, int colour, ArrayList moveList, int[] board){
    
	int pieceType = wPawn + (colour * 6);
	
    while(Targets != 0){
        int toSq = Long.numberOfTrailingZeros(Targets);
        int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
        int capture = board[toSq];
        int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8;

        moveList.add(move | getConstants().moveFlagPromotion | wRook + (colour * 6));   //Rook
        moveList.add(move | getConstants().moveFlagPromotion | wKnight + (colour * 6)); //Knight
        moveList.add(move | getConstants().moveFlagPromotion | wBishop + (colour * 6)); //Bishop
        moveList.add(move | getConstants().moveFlagPromotion | wQueen + (colour * 6));  //Queen
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

makeMove(oldIndex, newIndex, moveFlags);
 
}

public void makeMove(int oldIndex, int newIndex, int moveFlags){

int piece =          gameState.getBoard()[oldIndex];
int capturedPiece =  gameState.getBoard()[newIndex];
int fromSq =         oldIndex;
int toSq =           newIndex;

int move = piece << 28 | capturedPiece << 24 | fromSq << 16 | toSq << 8 | moveFlags;

int[] moveInfo = parseMove(move);

updateGame(moveInfo, gameState);
 
}

/**
 * Generate a complete list of legal moves for white
 */
public int[] whiteLegalMoves(){
    
ArrayList<Integer> legalList = new ArrayList();
int[] whiteMoves, legalMoves, tempBoard;

whiteMoves = generateAllMoves(WHITE, gameState);

for(int wmove : whiteMoves){
    
	GameState tGameState = gameState.copy();
    
    updateGame(parseMove(wmove), tGameState);
    
    if(!inCheck(WHITE, tGameState)){ //If move doesn't leave white in check
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
	return castleIsLegal(WHITE, gameState, queenSide);
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
	int[] gameBoard = gameState.getBoard();
	byte gameFlags = gameState.getFlags();
	
	if ((startInd == 4  && endInd == 6  && gameBoard[4]  == wKing) || 
		(startInd == 60 && endInd == 62 && gameBoard[60] == bKing)) {
		flags |= getConstants().moveFlagKingSideCastle;
	}
	else if ((startInd == 4  && endInd == 2  && gameBoard[4]  == wKing) || 
			 (startInd == 60 && endInd == 58 && gameBoard[60] == bKing)) {
		flags |= getConstants().moveFlagQueenSideCastle;
	}
	else if(gameBoard[startInd] == wPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 5 && endSq[1] == (gameFlags & getConstants().gameFlagEnPassantMask)){
		flags |= getConstants().moveFlagEnPassant;
    }
	else if(gameBoard[startInd] == bPawn && ((gameFlags & 1) == 1) &&
			endSq[0] == 2 && endSq[1] == (gameFlags & getConstants().gameFlagEnPassantMask)){
		flags |= getConstants().moveFlagEnPassant;
    }

	if(fen.length() > 4){
		String promotionType = fen.substring(4).toLowerCase();
		flags |= getConstants().moveFlagPromotion;
		
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

	return gameBoard[startInd] << 28 | gameBoard[endInd] << 24 |
			startInd << 16 | endInd << 8 | flags;
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

public void makeANMove(String ANmove) {
	int[] move = parseMove(convertFENtoMoveInt(ANmove));
	int startSq = move[2];
	int endSq = move[3];
	int flags = move[4];
	makeMove(startSq, endSq, flags);
	
}

private void setBoardPositions(String fen) {
	int sq = 56;
	
	char c;
	int i = 0;
	int len = fen.length();
	long[] pieceBoards = gameState.getPieceBoards();
	int[] board = gameState.getBoard();
	
	while(i < len) {
		
		c = fen.charAt(i);
		
		if(Character.isLetter(c)){
			int piece = pieceNum(c);
			pieceBoards[piece] = pieceBoards[piece] | (1L << sq);
	        board[sq] = piece;
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
	
	byte gameFlags = gameState.getFlags();
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
			gameState.setFlags(gameFlags);
			break;
			
		case 'K':
			gameFlags |= 0b10000000;
			gameState.setFlags(gameFlags);
			break;
			
		case 'Q':
			gameFlags |= 0b01000000;	
			gameState.setFlags(gameFlags);
			break;

		case 'k':
			gameFlags |= 0b00100000;	
			gameState.setFlags(gameFlags);
			break;
			
		case 'q':
			gameFlags |= 0b00010000;	
			gameState.setFlags(gameFlags);
			break;
		
		}
	}
}

private void setEnPassantFlag(String fen){
	byte gameFlags = gameState.getFlags();
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
	gameState.setFlags(gameFlags);
	
}

private int[] ANtoArrayIndex(int Row, char Col){
    
    return new int[]{Row - 1, (int) Col - 97};
}

private int pieceNum(char c){
	
	char[] FENPieces = {'P', 'K', 'B', 'R', 'Q', 'K', 'p', 'k', 'b', 'r', 'q', 'k'};
	return Arrays.binarySearch(FENPieces, 0, 11, c);
	

}


public Constants getConstants() {
	return Constants;
}

public void setConstants(Constants constants) {
	Constants = constants;
}

}