package joeflowplayschess;

import java.util.Arrays;

public class GameState {
	
	private byte 	  gameFlags;
	private long[] 	  gamePieceBoards;
	private int[] 	  gameBoard;
	
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
	
	private Constants Constants;
	
	public GameState(boolean startPos, Constants constants) {
		
		gameFlags = (byte) 0b11110000; //Initialize with all castling rights
		gamePieceBoards = new long[12];
		gameBoard = new int[64];
		
		Constants = constants;
		if(startPos) {
			setToStartPosition();
		}
	}
	
	private GameState(byte flags, long[] pieceBoards, int[] board, Constants constants) {
		
		Constants = constants;
		
		gameFlags = flags;
		gamePieceBoards = pieceBoards;
		gameBoard = board;
	}
	
	
	public byte getFlags() {
		return gameFlags;
	}
	public void setFlags(byte gameFlags) {
		this.gameFlags = gameFlags;
	}
	public long[] getPieceBoards() {
		return gamePieceBoards;
	}
	public void setPieceBoards(long[] gamePieceBoards) {
		this.gamePieceBoards = gamePieceBoards;
	}
	public int[] getBoard() {
		return gameBoard;
	}
	public void setBoard(int[] gameBoard) {
		this.gameBoard = gameBoard;
	}
	
	
	public GameState copy(){
		return new GameState(gameFlags, Arrays.copyOf(gamePieceBoards, 12), Arrays.copyOf(gameBoard, 64), Constants);
	}
	
	public long getFriendlyPieces(int colour) {
		
		long friendlyPieces = 0;
	    for(int j = colour*6; j < colour*6 + 6; j++){
	        friendlyPieces |= gamePieceBoards[j];
	    }
	    return friendlyPieces;
		
	}

	public long getEnemyPieces(int colour) {
	    //build a bitboard representing all enemy pieces on the board
	    long enemyPieces = 0;
	    for(int j = (1-colour)*6; j < (1-colour)*6 + 6; j++){
	        enemyPieces |= gamePieceBoards[j];
	    }
	    return enemyPieces;
	}
	
	public long getEmptySquares() {
	    //Build a bitboard representing all free, unoccupied squares on the board
	    long freeSquares = Constants.ALL_SET; 
	    for(long piece : gamePieceBoards){
	        freeSquares &= (~piece);
	    }
	    return freeSquares;
	}
	
	public long getAllPieces() {
		
		Arrays.stream(gamePieceBoards).reduce(0L, );
	    long allPieces = 0;
	    for(int i = 0; i < 12; i++){
	        allPieces |= gamePieceBoards[i];
	    }	
	    return allPieces;
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
	public void setToStartPosition(){
		
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
		
	}
	
	
	
}
