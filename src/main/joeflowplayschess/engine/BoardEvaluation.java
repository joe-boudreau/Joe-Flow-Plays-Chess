package joeflowplayschess.engine;

import java.util.Arrays;
import java.util.Random;

import static java.lang.Long.bitCount;
import static joeflowplayschess.engine.Constants.*;
import static joeflowplayschess.engine.Constants.WHITE;

public class BoardEvaluation {

    private static final Random rand = new Random();

    private static final int[] PIECE_VALUE = {100, 320, 325, 500, 975};

    //The following piece square value tables are oriented for black.
    // To use for white pieces, lookup index (square + 56) - ((int)square/8)*16
    private static final int[] PAWN_SQUARE_SCORE = new int[]{
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 27, 27, 10,  5,  5,
            0,  0,  0, 25, 25,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-25,-25, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_SQUARE_SCORE = new int[]{
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-20,-30,-30,-20,-40,-50
    };

    private static final int[] BISHOP_SQUARE_SCORE = new int[]{
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-40,-10,-10,-40,-10,-20
    };

    private static final int[][] SQUARE_SCORES = {PAWN_SQUARE_SCORE, KNIGHT_SQUARE_SCORE, BISHOP_SQUARE_SCORE};

    private static final int[] KING_SQUARE_SCORE = new int[]{
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20,  20,   0,   0,   0,   0,  20,  20,
            20,  30,  10,   0,   0,  10,  30,  20
    };

    private static final int[] KING_END_TABLE_SQUARE_SCORE = new int[]{
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };

    private static final int[][] KING_SQUARE_SCORES = {KING_SQUARE_SCORE, KING_END_TABLE_SQUARE_SCORE};

    /**
     * Evaluates a game board according to various metrics, most importantly using a weighted
     * material score calculation.
     *
     */
    public static int evaluateGameScore(GameState state, MoveGeneration moveGenerator){

        int overallScore = 0;

        long[] pieceBoards = state.getPieceBoards();

        //Material Score Black
        for(int piece = bPawn; piece <= bQueen; piece++){
            overallScore += PIECE_VALUE[piece - 6]*bitCount(pieceBoards[piece]);
        }
        //Material Score White
        for(int piece = wPawn; piece <= wQueen; piece++){
            overallScore -= PIECE_VALUE[piece]*bitCount(pieceBoards[piece]);
        }

        //Piece Position Score Black
        for(int piece = bPawn; piece <= bBishop; piece++){
            overallScore += piecePositionScore(pieceBoards[piece], SQUARE_SCORES[piece-6], BLACK);
        }

        //Piece Position Score White
        for(int piece = wPawn; piece <= wBishop; piece++){
            overallScore -= piecePositionScore(pieceBoards[piece], SQUARE_SCORES[piece], WHITE);
        }

        //TODO remove if statement
        overallScore += kingPositionScore(pieceBoards[bKing], BLACK, KING_SQUARE_SCORES[state.totalPiecesRemaining() > 10 ? 0 : 1]);

        overallScore += kingPositionScore(pieceBoards[wKing], WHITE, KING_SQUARE_SCORES[state.totalPiecesRemaining() > 10 ? 0 : 1]);

        overallScore = 2*(overallScore) + centreControlScore(state);

        overallScore = 3*(overallScore) + 2*(pawnStructureScore(bPawn) - pawnStructureScore(wPawn));

        overallScore = 2*(overallScore) + mobilityScore(BLACK, state, moveGenerator) - mobilityScore(WHITE, state, moveGenerator);

        //Little bit of randomness
        overallScore += rand.nextInt(3);

        return overallScore;

    }

    public static int piecePositionScore(long pieceBoard, int[] squareValues, int colour){

        int score = 0;
        int piecePosition;

        while (pieceBoard > 0) {
            piecePosition = Long.numberOfTrailingZeros(pieceBoard);
            score += squareValues[squareMappedToTableIndexByColour(piecePosition, colour)];
            pieceBoard &= (pieceBoard - 1);
        }
        return score;
    }

    public static int kingPositionScore(long king, int colour, int[] squareValues){
        int piecePosition;
        piecePosition = Long.numberOfTrailingZeros(king);
        //TODO make linear interpolated value
        return squareValues[squareMappedToTableIndexByColour(piecePosition, colour)];

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
    private static int centreControlScore(GameState gameState){

        long  whiteCentre = gameState.getFriendlyPieces(WHITE) & Constants.CENTER_4;
        long  blackCentre = gameState.getFriendlyPieces(BLACK) & Constants.CENTER_4;

        return bitCount(blackCentre) - bitCount(whiteCentre);
    }

    /**
     * Assigns a score to the amount of pieces you are currently attacking (and conversely the amount
     * of your pieces being attacked by the opposing player) as well as how many moves you have available
     * to choose from at that board position. Mobility is advantageous, generally.
     */
    private static int mobilityScore(int colour, GameState state, MoveGeneration moveGenerator){

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
        int file, pawn;

        tPawns = pawns;
        while(tPawns > 0){
            pawn = Long.numberOfTrailingZeros(tPawns);
            if((pawns & (1L << (pawn+8))) != 0){ dubs++;}
            tPawns &= (tPawns-1);
        }

        tPawns = pawns;
        while(tPawns > 0){
            pawn = Long.numberOfTrailingZeros(tPawns);
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

    private static int squareMappedToTableIndexByColour(int square, int colour){
        return (colour)*square + (1-colour)*((square + 56) - ((int)square/8)*16);
    }
}
