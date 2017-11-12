package joeflowplayschess.engine;

import joeflowplayschess.Utils;

import java.util.Arrays;
import java.util.List;

import static joeflowplayschess.engine.Constants.*;

public class UCI {

    private static List<Character> FENPieces = Arrays.asList(new Character[]{'P', 'N', 'B', 'R', 'Q', 'K', 'p', 'n', 'b', 'r', 'q', 'k'});

    public String bestMove() {
        return "";
    }

    public static int convertFENtoMoveInt(GameState gameState, String fen){

        byte flags = 0;
        String startPos = fen.substring(0, 2);
        String endPos = fen.substring(2, 4);


        int[] startSq = ANtoArrayIndex(Integer.parseInt(startPos.substring(1)), startPos.charAt(0));
        int[] endSq = ANtoArrayIndex(Integer.parseInt(endPos.substring(1)), endPos.charAt(0));

        int startInd = Utils.getIndex(startSq);
        int endInd = Utils.getIndex(endSq);
        int[] gameBoard = gameState.getBoard();
        byte gameFlags = gameState.getFlags();

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

        return gameBoard[startInd] << 28 | gameBoard[endInd] << 24 |
                startInd << 16 | endInd << 8 | flags;
    }

    public static void parseFENAndUpdate(GameState gameState, String fen) {

        String[] fields = fen.split(" +");

        String pieces =						 fields[0];
        String colourToMove =				 fields[1];
        String castling = 					 fields[2];
        String enPassantSquare = 			 fields[3];
        String halfmovesSinceCaptureOrPawn = fields[4];
        String moveNumber = 				 fields[5];

        setBoardPositions(gameState, pieces);
        gameState.setTurn(colourToMove.equals("w") ? WHITE : BLACK);
        setCastlingRights(gameState, castling);
        setEnPassantFlag(gameState, enPassantSquare);


    }

    public static void setBoardPositions(GameState gameState, String fen) {
        int sq = 56;

        char c;
        int i = 0;
        int len = fen.length();
        long[] pieceBoards = gameState.getPieceBoards();
        int[] board = gameState.getBoard();

        while(i < len) {

            c = fen.charAt(i);

            if(Character.isLetter(c)){
                int piece = FENPieces.indexOf(c);
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

    public static void setCastlingRights(GameState gameState, String fen) {

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

    public static void setEnPassantFlag(GameState gameState, String fen){
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

    private static int[] ANtoArrayIndex(int Row, char Col){

        return new int[]{Row - 1, (int) Col - 97};
    }
}
