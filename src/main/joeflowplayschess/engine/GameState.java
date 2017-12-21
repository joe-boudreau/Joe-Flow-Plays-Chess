package joeflowplayschess.engine;

import java.util.Arrays;

import static joeflowplayschess.engine.Constants.WHITE;

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
public class GameState {
	
	private byte   flags;
	private long[] pieceBoards;
	private int[]  board;
	private int	   turn;
	private int    moveCount;

	private ZobristKeys zobristKeyHelper;
	private long   zobristKey;
	
	public GameState(ZobristKeys zobristKeys) {
		
		flags = (byte) (Constants.WHITE_KINGSIDE_CASTLE | Constants.WHITE_QUEENSIDE_CASTLE |
							Constants.BLACK_KINGSIDE_CASTLE | Constants.BLACK_QUEENSIDE_CASTLE); //Initialize with all castling rights
		pieceBoards = new long[13];
		board = new int[64];
		turn = WHITE;
		moveCount = 0;
		setToStartPosition();
		zobristKeyHelper = zobristKeys;
		zobristKey = zobristKeyHelper.generateZobristKey(this);
	}
	
	public GameState(byte flags, int[] board, int colourToMove, int moveCount, long zobristKey, ZobristKeys zobristKeys) {

		this.flags = flags;
		this.board = board;
		this.pieceBoards = new long[13];
		this.turn = colourToMove;
		this.moveCount = moveCount;
		this.zobristKey = zobristKey;
		this.zobristKeyHelper = zobristKeys;
		generatePieceBoards();
	}
	



	//Piece Boards
	public long[] getPieceBoards() {
		return pieceBoards;
	}
	public void setPieceBoards(long[] gamePieceBoards) {
		this.pieceBoards = gamePieceBoards;
	}



	//Game Board
	public int[] getBoard() {
		return board;
	}
	public void setBoard(int[] gameBoard) {
		this.board = gameBoard;
	}



	//Turn to bestMove
	public int getTurn() {
		return turn;
	}
	public void setTurn(int turn) {
		this.turn = turn;
	}
	public void switchTurn() {
		//This will convert WHITE to BLACK and BLACK to WHITE
		turn = 1 - turn;
	}


	//Move Count
	public int getMoveCount() {
		return moveCount;
	}

	public void setMoveCount(int moveCount) {
		this.moveCount = moveCount;
	}
	public void incrementMoveCount(){
		moveCount++;
	}


	//Zobrist Key
	public long getZobristKey() {
		return zobristKey;
	}

	//Game Flags
	public byte getFlags() {
		return flags;
	}

	public void updateZobristKeyAndFlags(int[] parsedMove, byte newFlags) {
		this.zobristKey = zobristKeyHelper.updateZobristKey(zobristKey, parsedMove, flags, newFlags);
		this.flags = newFlags;
	}

	/**
	 * Returns a new instance of the current GameState object. No shared references.
	 *
	 * @return	an equivalent GameState
	 */
	public GameState copy(){
		return new GameState(flags, Arrays.copyOf(board, 64), turn, moveCount, zobristKey, zobristKeyHelper);
	}
	
	public long getFriendlyPieces(int colour) {
		
		long friendlyPieces = 0;
	    for(int j = colour*6 + 1; j < colour*6 + 6 + 1; j++){
	        friendlyPieces |= pieceBoards[j];
	    }
	    return friendlyPieces;
		
	}

	public long getEnemyPieces(int colour) {
	    //build a bitboard representing all enemy pieces on the board
	    long enemyPieces = 0;
	    for(int j = (1-colour)*6 + 1; j < (1-colour)*6 + 6 + 1; j++){
	        enemyPieces |= pieceBoards[j];
	    }
	    return enemyPieces;
	}
	
	public long getEmptySquares() {
	    //Build a bitboard representing all free, unoccupied squares on the board
	    long freeSquares = Constants.ALL_SET;
	    for(long piece : pieceBoards){
	        freeSquares &= (~piece);
	    }
	    return freeSquares;
	}
	
	public long getAllPieces() {
		
		return Arrays.stream(pieceBoards).reduce(0L, (x, y) -> x | y);

	}

	public int totalPiecesRemaining(){
		return Long.bitCount(getAllPieces());
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
		        board[sq] = Constants.wRook;
		    }
		    else if(sq == 1 || sq == 6){
		        board[sq] = Constants.wKnight;
		    }
			else if(sq == 2 || sq == 5){
		        board[sq] = Constants.wBishop;
		    }
			else if(sq == 3){
		        board[sq] = Constants.wQueen;
		    }
			else if(sq == 4){
		        board[sq] = Constants.wKing;
		    }
			else if(sq > 7 && sq < 16){
		        board[sq] = Constants.wPawn;
		    }

			else if(sq > 15 && sq < 48){
		        board[sq] = Constants.empty;
		    }
		    
		    //Black pieces
			else if(sq == 56 || sq == 63){
		        board[sq] = Constants.bRook;
		    }
			else if(sq == 57 || sq == 62){
		        board[sq] = Constants.bKnight;
		    }
			else if(sq == 58 || sq == 61){
		        board[sq] = Constants.bBishop;
		    }
			else if(sq == 59){
		        board[sq] = Constants.bQueen;
		    }
			else if(sq == 60){
		        board[sq] = Constants.bKing;
		    }
			else if(sq > 47 && sq < 56){
		        board[sq] = Constants.bPawn;
		    }
		}
		generatePieceBoards();
	}

	private void generatePieceBoards(){
		for(int i = 0; i < 64; i++){
			if(board[i] != Constants.empty) {
				pieceBoards[board[i]] |= (1L << i);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GameState)) return false;

		GameState gameState = (GameState) o;

		if (flags != gameState.flags) return false;
		if (!Arrays.equals(pieceBoards, gameState.pieceBoards)) return false;
		if (turn != gameState.getTurn()) return false;
		return Arrays.equals(board, gameState.board);
	}

	@Override
	public String toString(){
		String n = "\r\n";
		char[] pieceMapping = new char[]{'0', 'p', 'n', 'b', 'r', 'q', 'k', 'P', 'N', 'B', 'R', 'Q', 'K'};
		char[][] boardRows = new char[8][16];

		String board ="";
		int rowNum = 7;
		for(char[] row : boardRows){
			for(int i = 0; i < 7; i++){
				row[2*i] = pieceMapping[this.board[rowNum*8 + i]];
				row[2*i + 1] = ' ';
			}
			row[14] = pieceMapping[this.board[rowNum*8 + 7]];
			rowNum--;
			board += new String(row) + n;
		}

		return "Board:" + n +
				board +
				"Flags:" + n +
				Long.toBinaryString(Byte.toUnsignedLong(flags)) + n +
				"Turn:" + n +
				(turn == WHITE ? "WHITE" : "BLACK") + n +
				"Zobrist Key: " + n +
				zobristKey + n;

	}
}
