package joeflowplayschess;

public class MoveSelectorRunnable implements Runnable {

	ChessEngine engine;
	String move;
	
	public MoveSelectorRunnable(ChessEngine chessEngine, String bestMove){
		engine = chessEngine;
		move = bestMove;
	}
	@Override
	public void run() {
		

	}

}
