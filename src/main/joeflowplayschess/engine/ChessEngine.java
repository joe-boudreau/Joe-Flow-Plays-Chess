/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess.engine;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static joeflowplayschess.engine.Constants.*;
import static joeflowplayschess.engine.TranspositionTable.NodeType;
import static joeflowplayschess.engine.TranspositionTable.PositionEntry;

/**
 * The ChessEngine class computes all the decision making for the engine, which plays
 * as black exclusively. 
 * 
 * The architecture is based on bit-board game representation, which
 * allows for rapid computations of game information and efficient memory usage.
 * The bestMove generation uses fairly common techniques, including magic-bitboards
 *  for the bestMove generation of sliding pieces (rooks, bishops, queen). Move
 *  selection is made using a recursive search through the bestMove tree with alpha
 *  beta pruning techniques. The board evaluation is a minimax score based 
 *  function which uses a material score calculation as the primary factor for
 *  board advantage, with heuristic evaluation factors as well if there is no
 *  material advantage found. Factors such as pawn structure, centre control,
 *  piece advancement, and piece mobility are looked at.
 * 
 *
 */
public class ChessEngine {

    static Logger logger = Logger.getLogger(ChessEngine.class);

	//declarations
	private GameState gameState;
	private int 	  nodeCount;
	private Constants constants;
	private MoveGeneration moveGenerator;
	private ZobristKeys zobristKeys;
	private TranspositionTable transpositionTable;
	private int nodesPruned = 0;
	private float avgPrunage = 0;
	private int moveCount = 0;

	//declarations + initializations
	private static int defaultDepth = 	    5;
	private boolean debugMode = 			true;
	private boolean initialized =			false;
	private boolean firstgame = 			true;

public ChessEngine(){}

public void init(){
	
	if(!initialized){
		if(firstgame){
			constants = Constants.init(this.getClass().getClassLoader());
			firstgame = false;
		}
		zobristKeys = new ZobristKeys();
		transpositionTable = new TranspositionTable(2000003);
		gameState = new GameState(zobristKeys);  //set up board for beginning of game
        moveGenerator = new MoveGeneration(constants);

		initialized = true;
	}
}


//--Decision Making Logic----------------------------------------------------------------------

/**
 * Top-level decision controller which initiates the bestMove selection process for black.
 * 
 * This function is called by the main JoeFlowPlaysChess class when it is black's turn.
 * The function returns an int[] array which is the standard 32 bit bestMove encoding, split into
 * a 5 or 6 element array. A standard bestMove will return a 5 element array, which is just the bestMove
 * information. If the bestMove results in black winning, or a black stalemate, then the array returned
 * will have 6 elements.  The first 5 elements of the array are the bestMove information, and the last
 * element will be a flag signifying a black checkmate (0) or a stalemate caused by black's bestMove (-1)
 * 
 * @param colour	Always BLACK, which is 1
 * @return 			array of ints containing bestMove and game information
 */

public int[] selectMove(int colour){
	return selectMove(colour, defaultDepth);
}

public int[] selectMove(int colour, int searchDepth){

	int[] bestMove, moveInfo;

	nodeCount = 0;

	gameState.setTurn(colour);
	bestMove = chooseBestMove(gameState, searchDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, null);

	avgPrunage = ((moveCount*avgPrunage + ((float)nodesPruned / (nodeCount + nodesPruned))) / (moveCount + 1));

	logger.info("Nodes pruned:" + nodesPruned);
	logger.info("Running Average Prunage:" + avgPrunage*100 + "%");
	nodesPruned = 0;
	moveCount++;

	return checkForMateAndUpdateBoard(searchDepth, bestMove);

}

public int[] selectMoveRestricted(String[] FENMoves){

	int[] bestMove, moveInfo;
	
	nodeCount = 0;
	
	bestMove = chooseBestMove(gameState, defaultDepth, Integer.MIN_VALUE, Integer.MAX_VALUE,
							  Stream.of(FENMoves).map(s -> UCI.convertFENtoMoveInt(gameState, s)).mapToInt(Integer::intValue).toArray());
	
	return checkForMateAndUpdateBoard(defaultDepth, bestMove);

}

private int[] checkForMateAndUpdateBoard( int searchDepth, int[] bestMove) {
	int colour = gameState.getTurn();
    int[] moveInfo;
	moveInfo = Utils.parseMove(bestMove[0]);
	
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
	
	//Update game information now that the bestMove has been completed
	updateGame(moveInfo, gameState);
	
	//Print some relevant infomation to the console
	if(debugMode) {
		logger.info("Nodes traversed: " + nodeCount);
		logger.info("Best Move Score For Black: " + bestMove[1]);
		logger.info("Game Flags: " + Integer.toBinaryString(Byte.toUnsignedInt(gameState.getFlags())));
		logger.info(gameState.toString());
	}
	
	return moveInfo;
}

/**
 * Move Selection function which uses recursion to search the possible bestMove space to a specified depth
 * 
 * This is the primary decision making function for the engine. It calls the generateAllMoves() function
 * to generate a list of every possible bestMove, and then for each possible bestMove a copy of the game state is
 * updated with the results of the bestMove, and then this function is called again from the perspective of the
 * opposing player and the process is repeated. This is done until a specified depth is reached (determined 
 * by the value of class field "searchdepth" and then the board at that state is evaluated using an evaluation
 * function. The results of the evaluation are returned through the recursion stack to the root function call
 * to decide the best bestMove to make next.
 * 
 * An alpha and beta score are used to keep track of the most positive (alpha) and most negative (beta) scores encountered
 * from moves previously searched in the search tree, which allows for pruning of the tree if the better bestMove for the
 * opposing player has already been determined and the tree currently being searched would not be selected by them.
 */
private int[] chooseBestMove(GameState gState, int depth, int alpha, int beta, int[] movesToSearch) {

	int i;
	int[] moves, moveScore, parsedMove;
	int colour = gState.getTurn();
	boolean legal = true;


	PositionEntry existingEntry = transpositionTable.getEntryIfExists(gState);
	if (existingEntry != null) {
		Integer existingValue = transpositionTable.getValueFromEntry(existingEntry, gState, depth, alpha, beta);
		if (existingValue != null) {
			//Use Transposition Table entry - no need to search tree
			return new int[]{existingEntry.bestMove, existingValue};
		} else {
			moves = MoveSorter.sortMovesWithBestMove(moveGenerator.generateAllMoves(colour, gState), existingEntry.bestMove);
		}
	} else {

		moves = MoveSorter.sortMoves(moveGenerator.generateAllMoves(colour, gState));
	}


	boolean check = inCheck(colour, gState);
	int[] bestMove = new int[]{0, Integer.MAX_VALUE * (1 - 2 * colour)};

	for (i = 0; i < moves.length; i++) {

		nodeCount++;
		GameState tState = gState.copy();

		parsedMove = Utils.parseMove(moves[i]);

		updateGame(parsedMove, tState);

		if (!check && Utils.isCastleMove(parsedMove, colour)) {
			legal = isCastleLegal(colour, tState, parsedMove[3] == Constants.queenSideCastleDestinationSquare[colour]);
		}


		if (legal && !inCheck(colour, tState)) {

			if (depth > 0) {
				//Call chooseBestMove on the resulting board position after bestMove is made, from the opposing players view
				tState.switchTurn();
				int[] theirMove = chooseBestMove(tState, depth - 1, alpha, beta, null);
				moveScore = new int[]{moves[i], theirMove[1]};
			} else {
				//Evaluate board position if at the terminal depth
				moveScore = new int[]{moves[i], BoardEvaluation.evaluateGameScore(tState, moveGenerator)};
			}


			if (colour == BLACK) {

				//A higher bestMove score is beneficial for BLACK
				if (moveScore[1] > bestMove[1]) {
					bestMove = moveScore;
				}

				if (bestMove[1] > alpha) {
					alpha = bestMove[1];

					if (beta <= alpha) {
						nodesPruned += (moves.length - i);
						transpositionTable.put(gState, depth, beta, bestMove[0], NodeType.BETA);
						return new int[]{bestMove[0], beta}; //Beta or alpha cut-off
					}
				}
			} else {

				//A lower (possibly negative) game score is beneficial for WHITE
				if (moveScore[1] < bestMove[1]) {
					bestMove = moveScore;
				}

				if (bestMove[1] < beta) {
					beta = bestMove[1];

					if (beta <= alpha) {
						nodesPruned += (moves.length - i);
						transpositionTable.put(gState, depth, alpha, bestMove[0], NodeType.ALPHA);
						return new int[]{bestMove[0], alpha}; //Beta or alpha cut-off
					}
				}
			}
		}
		legal = true;
	}


	if (bestMove[0] == 0) {
		//If the bestMove is 0, this means no moves for black are legal and it is either in checkmate or it is a stalemate
		if (check) {
			bestMove = new int[]{-1, (-2 * colour + 1) * 1000000 * depth}; //checkmate
		} else {
			bestMove = new int[]{-1, (2 * colour - 1) * 1000000 * depth}; //stalemate
		}
	}

	transpositionTable.put(gState, depth, bestMove[1], bestMove[0], NodeType.EXACT);
	return bestMove;

}


	/**
 * Accepts a bestMove, and the object reference to the current game information,
 * and updates the game information based off the bestMove
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
    
    int colour = piece <= 6 ? WHITE : BLACK;

    pieceBoards[piece] &= (~(1L << fromSq)); //Remove moving piece from old square
    board[fromSq] = empty;
    
    if((moveFlags & Constants.moveFlagPromotion) != 0){ //Pawn Promotion
        
        pieceBoards[moveFlags & Constants.moveFlagPromotedPiece] |= (1L << toSq); //Add promoted piece to new square
        board[toSq] = moveFlags & Constants.moveFlagPromotedPiece;
    }
    else if((moveFlags & Constants.moveFlagEnPassant) != 0){ //En Passant
        
        int[] otherPiece = new int[]{bPawn, wPawn};
        int[] sqDelta = new int[]{-8, 8};
        
        pieceBoards[piece] |= (1L << toSq); //add piece to new square
        pieceBoards[otherPiece[colour]] &= (~(1L << (toSq + sqDelta[colour]))); //remove captured piece
        board[toSq + sqDelta[colour]] = empty;
        board[toSq] = piece;
    }
    else if((moveFlags & Constants.moveFlagKingSideCastle) != 0){ //King-side Castle
        
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
    else if((moveFlags & Constants.moveFlagQueenSideCastle) != 0){ //Queen-side Castle
        
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
    
    if((flags & 0b11110000) != 0){ //If castles are still possible, check if bestMove removes castle eligibility
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
    flags &= 0b11110000; // clear en-passant flags from last bestMove
    
    if((piece == wPawn || piece == bPawn) && Math.abs(toSq - fromSq) == 16){ //en passant possible
        flags = (byte) (flags | (toSq%8 << 1) | 1);
    }

    state.updateZobristKeyAndFlags(move, flags);
    state.incrementMoveCount();
}

/**
 * Checks if given colour is in check currently. Returns true if in check
 * 
 * Generates all moves for the opposing colour, and checks if the King is a possible target for capture
 */
public boolean inCheck(int colour, GameState state){

    int[] moves = moveGenerator.generateAllMoves(1 - colour, state);

    for(int move : moves){
        if( ((move << 4) >>> 28) == (colour*6 + 6)) {
        	return true;
		}
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
public boolean isCastleLegal(int colour, GameState state, boolean queenSide){

	GameState tState = state.copy();

	tState.getBoard()[Constants.initKingPos[colour]] = empty;

	if(queenSide){
		if(colour == WHITE && ((tState.getFlags() & Constants.WHITE_QUEENSIDE_CASTLE) == 0)) {return false;}
		if(colour == BLACK && ((tState.getFlags() & Constants.BLACK_QUEENSIDE_CASTLE) == 0)) {return false;}

		tState.getBoard()[Constants.initKingPos[colour]-1] = kings[colour];
		tState.getPieceBoards()[kings[colour]] <<= 1;
		if(inCheck(colour, tState)){ return false;}
	}
	else{
		if(colour == WHITE && ((tState.getFlags() & Constants.WHITE_KINGSIDE_CASTLE) == 0)) {return false;}
		if(colour == BLACK && ((tState.getFlags() & Constants.BLACK_KINGSIDE_CASTLE) == 0)) {return false;}

		tState.getBoard()[Constants.initKingPos[colour]+1] = kings[colour];
		tState.getPieceBoards()[kings[colour]] >>= 1;
		if(inCheck(colour, tState)){ return false;}
	}

	return true;

}

//--Methods for UCI Interface-------------------------------------------------------

public void makeANMove(String ANmove) {
    int[] move = Utils.parseMove(UCI.convertFENtoMoveInt(gameState, ANmove));
    int startSq = move[2];
    int endSq = move[3];
    int flags = move[4];
    makeMove(startSq, endSq, flags);

}

public void parseFENAndUpdate(String fen){
    UCI.parseFENAndUpdate(gameState, fen);
}

//--Methods for JoeFlowPlaysChess Class-------------------------------------------------------

/**
 * Called by the user side class to update the chess engine game information once a user has made
 * a bestMove
 */
public void makeMove(int[] oldPos, int[] newPos, int moveFlags){

int oldIndex = Utils.getIndex(oldPos);
int newIndex = Utils.getIndex(newPos);

makeMove(oldIndex, newIndex, moveFlags);
 
}

public void makeMove(int oldIndex, int newIndex, int moveFlags){

int piece =          gameState.getBoard()[oldIndex];
int capturedPiece =  gameState.getBoard()[newIndex];
int fromSq =         oldIndex;
int toSq =           newIndex;

int move = piece << 28 | capturedPiece << 24 | fromSq << 16 | toSq << 8 | moveFlags;

int[] moveInfo = Utils.parseMove(move);

updateGame(moveInfo, gameState);
 
}

/**
 * Generate a complete list of legal moves for white
 * TODO: WHITE CAN STILL CASTLE EVEN IF IN CHECK .... NOT GOOD
 */
public int[] whiteLegalMoves(){
    
ArrayList<Integer> legalList = new ArrayList();
int[] whiteMoves, legalMoves, tempBoard;

whiteMoves = moveGenerator.generateAllMoves(WHITE, gameState);



for(int wmove : whiteMoves){

	

	GameState tGameState = gameState.copy();
    
    updateGame(Utils.parseMove(wmove), tGameState);
    
    if(!inCheck(WHITE, tGameState)){ //If bestMove doesn't leave white in check
        legalList.add(wmove);		 //then Add bestMove
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
public boolean isCastleLegalForWhite(boolean queenSide){
	return isCastleLegal(WHITE, gameState, queenSide);
}

}