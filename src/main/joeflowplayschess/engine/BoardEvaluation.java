package joeflowplayschess.engine;

import java.util.Arrays;
import static java.lang.Long.bitCount;
import static joeflowplayschess.engine.Constants.*;
import static joeflowplayschess.engine.Constants.WHITE;

public class BoardEvaluation {

    /**
     * Evaluates a game board according to various metrics, most importantly using a weighted
     * material score calculation.
     *
     */
    public static int evaluateGameScore(GameState state, MoveGeneration moveGenerator){

        int overallScore;

        long[] pieceBoards = state.getPieceBoards();

        overallScore = 2*(bitCount(pieceBoards[bPawn]) - bitCount(pieceBoards[wPawn])) +
                7*(bitCount(pieceBoards[bKnight]) - bitCount(pieceBoards[wKnight])) +
                6*(bitCount(pieceBoards[bBishop]) - bitCount(pieceBoards[wBishop])) +
                10*(bitCount(pieceBoards[bRook]) - bitCount(pieceBoards[wRook])) +
                18*(bitCount(pieceBoards[bQueen]) - bitCount(pieceBoards[wQueen]));

        overallScore = 2*(overallScore) + centreControlScore(pieceBoards);

        overallScore = 3*(overallScore) + 2*(pawnStructureScore(bPawn) - pawnStructureScore(wPawn));

        overallScore = 2*(overallScore) + positionScore(BLACK, state, moveGenerator) - positionScore(WHITE, state, moveGenerator);

        return overallScore;

    }

    /**
     * Advancement score values pieces in ranks further away from their side. It is the smallest
     * weighted heuristic, but it tends to ensure black is aggressive and plays an offensive strategy
     *
     * Not used currently
     */
    private static int advancementScore(long[] currpieceBoards, int colour){

        int[] rankScore = new int[8];
        int advancement = 0;

        for(int i = 0; i < 8; i++){
            for(int j = colour*6; j < colour*6+6; j++){
                rankScore[i] += bitCount(currpieceBoards[j] & Constants.RANKS[i]);
            }

            advancement += rankScore[i]*(i + (7-i)*colour);
        }

        return advancement;
    }

    /**
     * Assigns a score to the number of pieces in the center of the board. Control of the center
     * squares is advantageous
     */
    private static int centreControlScore(long[] pieceBoards){

        long blackCentre = 0, whiteCentre = 0;

        for(int i = 0; i < 6; i++){
            whiteCentre |= (pieceBoards[i] & Constants.CENTER_4);
        }
        for(int j = 6; j < 12; j++){
            blackCentre |= (pieceBoards[j] & Constants.CENTER_4);
        }

        return bitCount(blackCentre) - bitCount(whiteCentre);
    }

    /**
     * Assigns a score to the amount of pieces you are currently attacking (and conversely the amount
     * of your pieces being attacked by the opposing player) as well as how many moves you have available
     * to choose from at that board position. Mobility is advantageous, generally.
     */
    private static int positionScore(int colour, GameState state, MoveGeneration moveGenerator){

        int[] yourMoves = moveGenerator.generateAllMoves(colour, state);

        int piecesAttacked = (int) Arrays.stream(yourMoves).filter(m -> ((m << 4) >>> 28) > 0).count();

        return 2*(piecesAttacked) + yourMoves.length;
    }

    /**
     * Assigns a score to the amount of isolated and doubled pawns the player has in the current
     * board position. Valued negatively.
     */
    private static int pawnStructureScore(long pawns){

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
}
