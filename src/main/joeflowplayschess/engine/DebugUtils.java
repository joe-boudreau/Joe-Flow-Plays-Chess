package joeflowplayschess.engine;

public class DebugUtils {


    public static void printAsBitBoard(long l){

        String bits = Long.toBinaryString(l);

        for(int j = 0; j < Long.numberOfLeadingZeros(l); j++){
            bits = "0"+bits;
        }

        for(int i = 0; i < 8; i++){
            System.out.println(new StringBuilder(bits.substring(i*8, i*8+8)).reverse().toString());

        }
    }

    public static void printBoardArray(int[] b){

        for(int i = 56; i > -1; i-=8){
            System.out.println(b[i] + "  " + b[i+1] + "  " +
                    b[i+2] + "  " + b[i+3] + "  " +
                    b[i+4] + "  " + b[i+5] + "  " +
                    b[i+6] + "  " + b[i+7] + "  ");
        }
    }

    public static void printMovesAsStrings(int[] moves){

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
}
