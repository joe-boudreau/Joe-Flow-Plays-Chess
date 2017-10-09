package joeflowplayschess.engine;

import java.util.Arrays;

public class GameState {
	
	private byte 	  gameFlags;
	private long[] 	  gamePieceBoards;
	private int[] 	  gameBoard;
	
	public GameState() {
		
		gameFlags = (byte) (Constants.WHITE_KINGSIDE_CASTLE | Constants.WHITE_QUEENSIDE_CASTLE |
							Constants.BLACK_KINGSIDE_CASTLE | Constants.BLACK_QUEENSIDE_CASTLE); //Initialize with all castling rights
		gamePieceBoards = new long[12];
		gameBoard = new int[64];
		setToStartPosition();
	}
	
	public GameState(byte flags, int[] board) {

		gameFlags = flags;
		gameBoard = board;
		gamePieceBoards = new long[12];
		generatePieceBoards();
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
		return new GameState(gameFlags, Arrays.copyOf(gameBoard, 64));
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
		
		return Arrays.stream(gamePieceBoards).reduce(0L, (x, y) -> x | y);

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
		        gameBoard[sq] = Constants.wRook;
		    }
		    else if(sq == 1 || sq == 6){
		        gameBoard[sq] = Constants.wKnight;
		    }
			else if(sq == 2 || sq == 5){
		        gameBoard[sq] = Constants.wBishop;
		    }
			else if(sq == 3){
		        gameBoard[sq] = Constants.wQueen;
		    }
			else if(sq == 4){
		        gameBoard[sq] = Constants.wKing;
		    }
			else if(sq > 7 && sq < 16){
		        gameBoard[sq] = Constants.wPawn;
		    }

			else if(sq > 15 && sq < 48){
		        gameBoard[sq] = Constants.empty;
		    }
		    
		    //Black pieces
			else if(sq == 56 || sq == 63){
		        gameBoard[sq] = Constants.bRook;
		    }
			else if(sq == 57 || sq == 62){
		        gameBoard[sq] = Constants.bKnight;
		    }
			else if(sq == 58 || sq == 61){
		        gameBoard[sq] = Constants.bBishop;
		    }
			else if(sq == 59){
		        gameBoard[sq] = Constants.bQueen;
		    }
			else if(sq == 60){
		        gameBoard[sq] = Constants.bKing;
		    }
			else if(sq > 47 && sq < 56){
		        gameBoard[sq] = Constants.bPawn;
		    }
		}
		generatePieceBoards();
	}

	private void generatePieceBoards(){
		for(int i = 0; i < 64; i++){
			if(gameBoard[i] != Constants.empty) {
				gamePieceBoards[gameBoard[i]] |= (1L << i);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GameState)) return false;

		GameState gameState = (GameState) o;

		if (gameFlags != gameState.gameFlags) return false;
		if (!Arrays.equals(gamePieceBoards, gameState.gamePieceBoards)) return false;
		return Arrays.equals(gameBoard, gameState.gameBoard);
	}

	@Override
	public String toString(){
		String n = "\r\n";
		char[] pieceMapping = new char[]{'p', 'n', 'b', 'r', 'q', 'k', 'P', 'N', 'B', 'R', 'Q', 'K', ' ', ' ', '0'};
		char[][] boardRows = new char[8][16];

		String board ="";
		int rowNum = 7;
		for(char[] row : boardRows){
			for(int i = 0; i < 7; i++){
				row[2*i] = pieceMapping[gameBoard[rowNum*8 + i]];
				row[2*i + 1] = ' ';
			}
			row[14] = pieceMapping[gameBoard[rowNum*8 + 7]];
			rowNum--;
			board += new String(row) + n;
		}

		return "Board:" + n +
				board +
				"Flags:" + n +
				Long.toBinaryString(Byte.toUnsignedLong(gameFlags));
	}
}
