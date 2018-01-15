package joeflowplayschess.engine;

import java.util.ArrayList;
import java.util.List;

import static joeflowplayschess.engine.Constants.*;

public class MoveGeneration {

    private Constants constants;

    public MoveGeneration(Constants constantsToUse){
        constants = constantsToUse;
    }

    /**
     * Top level function for calling the individual bestMove generation functions for each piece type. Returns
     * an array of ints representing all the possible moves.
     *
     */
    public int[][] generateAllMovesWithMoveScorePlaceholders(int colour, GameState state){

        List<Integer> possibleMoves = new ArrayList<>();

        generatePawnTargets(possibleMoves, colour, state);
        generateKnightTargets(possibleMoves, colour, state);
        generateKingTargets(possibleMoves, colour, state);
        generateRookTargets(possibleMoves, colour, state);
        generateBishopTargets(possibleMoves, colour, state);
        generateQueenTargets(possibleMoves, colour, state);


        int[][] movesArray = new int[possibleMoves.size()][2];

        for(int i = 0; i < movesArray.length; i++){
            movesArray[i][0] = possibleMoves.get(i);
        }

        return movesArray;

    }

    public int[] generateAllMoves(int colour, GameState state){

        List<Integer> possibleMoves = new ArrayList<>();

        generatePawnTargets(possibleMoves, colour, state);
        generateKnightTargets(possibleMoves, colour, state);
        generateKingTargets(possibleMoves, colour, state);
        generateRookTargets(possibleMoves, colour, state);
        generateBishopTargets(possibleMoves, colour, state);
        generateQueenTargets(possibleMoves, colour, state);


        int[] movesArray = new int[possibleMoves.size()];

        for(int i = 0; i < movesArray.length; i++){
            movesArray[i] = possibleMoves.get(i);
        }

        return movesArray;

    }

    /**
     * Generates all possible pawn targets and invokes the bestMove generation function to add the moves to the moves List, which is passed aa an input argument
     */
    private void generatePawnTargets(List moves, int colour, GameState state){

        long pawns = state.getPieceBoards()[wPawn + colour*6];

        if(pawns == 0) return;

        long pawnPush, pawnDoublePush, promotions, attackTargets, attacks, epAttacks, promotionAttacks;

        int[] pushDiff = new int[]{8, 64 - 8};	//push one forward
        int[][] attackDiff = new int[][]{{7, 64-9}, {9,64-7}}; //push one forward and one left or right


        long[] fileMask = new long[]{~Constants.FILE_H, ~Constants.FILE_A};
        long[] promotionMask = new long[]{Constants.RANK_8, Constants.RANK_1};
        long[] doublePushMask = new long[]{Constants.RANK_3, Constants.RANK_6};
        long[] enPassantMask = new long[]{Constants.RANK_6, Constants.RANK_3};

        int diff = pushDiff[colour];

        //Build a bitboard representing all free, unoccupied squares on the board
        long emptySquares = state.getEmptySquares();

        //build a bitboard representing all enemy pieces on the board
        long enemyPieces = state.getEnemyPieces(colour);


        long enPassantTargetSquare = Constants.FILES[(state.getFlags() & 0b00001110) >>> 1] & enPassantMask[colour];

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
                generatePawnMoves(epAttacks, diff, colour, moves, Constants.moveFlagEnPassant, state.getBoard());
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
    private void generateKnightTargets(List moves, int colour, GameState state){

        long knights = state.getPieceBoards()[wKnight + colour*6];

        if(knights == 0) return;

        int pieceType = wKnight + (colour * 6);
        long friendlyPieces = state.getFriendlyPieces(colour);

        long targets;
        while(knights > 0){
            int fromSq = Long.numberOfTrailingZeros(knights);
            targets = constants.KnightMoves[fromSq] & ~friendlyPieces;
            generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());
            knights &= knights - 1;
        }

    }

    /**
     * Generates King moves using the Constants.KingMoves pre-generated bitboards. Checks for castling ability as well.
     *
     */
    private void generateKingTargets(List moves, int colour, GameState state){

        long king = state.getPieceBoards()[wKing + colour*6];

        if(king == 0) return;

        int pieceType = wKing + (colour * 6);

        long allPieces = state.getAllPieces();
        long friendlyPieces = state.getFriendlyPieces(colour);

        int fromSq = Long.numberOfTrailingZeros(king);

        byte flags = state.getFlags();
        int[] board = state.getBoard();

        long targets = constants.KingMoves[fromSq] & ~friendlyPieces;
        generateMoves(fromSq, targets, pieceType, moves, 0, board);

        if((flags & (1 << 6)) != 0 && (allPieces & Constants.queenCastleSquares[colour]) == 0){
            generateMoves(fromSq, Constants.queenSideCastleDestinationSquare[colour], pieceType, moves, Constants.moveFlagQueenSideCastle, board);
        }
        if((flags & (1 << 7)) != 0 && (allPieces & Constants.kingCastleSquares[colour]) == 0){
            generateMoves(fromSq, Constants.kingSideCastleDestinationSquare[colour], pieceType, moves, Constants.moveFlagKingSideCastle, board);
        }
    }

    /**
     * Generates rook moves using magic bitboards. See Constants class for more info.
     */
    private void generateRookTargets(List moves, int colour, GameState state){

        long rooks = state.getPieceBoards()[wRook + colour*6];

        if(rooks == 0) return;

        long allPieces, friendlyPieces, targets;
        int i, j, fromSq, index;
        int pieceType = wRook + (colour * 6);

        allPieces = state.getAllPieces();

        friendlyPieces = state.getFriendlyPieces(colour);

        while(rooks != 0){
            fromSq = Long.numberOfTrailingZeros(rooks);

            index = (int) (((allPieces & constants.RookMaskOnSquare[fromSq])*
                    constants.magicNumberRook[fromSq]) >>>
                    constants.magicShiftsRook[fromSq]);

            targets = constants.magicMovesRook[fromSq][index] & (~friendlyPieces);
            generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());

            rooks &= rooks -1;
        }
    }

    /**
     * Generates bishop moves using magic bitboards. See Constants class for more info.
     */
    private void generateBishopTargets(List moves, int colour, GameState state){

        long bishops = state.getPieceBoards()[wBishop + colour*6];

        if(bishops == 0) return;

        long allPieces, friendlyPieces, targets;
        int i, j, fromSq, index;
        int pieceType = wBishop + (colour * 6);

        allPieces = state.getAllPieces();

        friendlyPieces = state.getFriendlyPieces(colour);

        while(bishops != 0){
            fromSq = Long.numberOfTrailingZeros(bishops);

            index = (int) (((allPieces & constants.BishopMaskOnSquare[fromSq])*
                    constants.magicNumberBishop[fromSq]) >>>
                    constants.magicShiftsBishop[fromSq]);

            targets = constants.magicMovesBishop[fromSq][index] & (~friendlyPieces);
            generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());

            bishops &= bishops -1;
        }
    }

    /**
     * Generates queen moves using magic bitboards. Uses a combination of bishop magic bitboards and rook magic bitboardsd.
     * See Constants class for more info.
     */
    private void generateQueenTargets(List moves, int colour, GameState state){

        long queens = state.getPieceBoards()[wQueen + colour*6];

        if(queens == 0) return;

        long allPieces, friendlyPieces, targets;
        int i, j, fromSq, rookIndex, bishopIndex;
        int pieceType = wQueen + (colour * 6);

        allPieces = state.getAllPieces();

        friendlyPieces = state.getFriendlyPieces(colour);

        while(queens != 0){
            fromSq = Long.numberOfTrailingZeros(queens);

            rookIndex = (int) (((allPieces & constants.RookMaskOnSquare[fromSq])*
                    constants.magicNumberRook[fromSq]) >>>
                    constants.magicShiftsRook[fromSq]);

            bishopIndex = (int) (((allPieces & constants.BishopMaskOnSquare[fromSq])*
                    constants.magicNumberBishop[fromSq]) >>>
                    constants.magicShiftsBishop[fromSq]);

            targets = (constants.magicMovesBishop[fromSq][bishopIndex] | constants.magicMovesRook[fromSq][rookIndex])
                    & (~friendlyPieces);
            generateMoves(fromSq, targets, pieceType, moves, 0, state.getBoard());

            queens &= queens -1;
        }
    }


    /**
     * Receives a bitboard representing all the target (destination) squares for a given piece, and parses the bitboard one-by-one
     * to generate individual moves, encoded as integers, and adds them to a List of moves which is an input argument.
     */
    private void generateMoves(int from, long Targets, int pieceType, List moveList, int flags, int[] board){

        while(Targets != 0){ //while bits are still set in the target bitboard

            int toSq = Long.numberOfTrailingZeros(Targets); //Get square index by computing the position of the the least significant bit set in the long
            int capture = board[toSq]; //Return piece occupying that square, if any. Will be empty (0xE) if no piece occupies the square.
            int move = pieceType << 28 | capture << 24 | from << 16 | toSq << 8 | flags; //Encode the bestMove information in an int
            moveList.add(move); //Add to the Move List
            Targets &= Targets - 1; //Mask out the least significant bit before repeating the loop again


        }
    }

    /**
     * Separate method for generating pawn moves. Similar to the generateMoves method but uses the restricted pawn bestMove rules to generate the starting square dynamically
     */
    private void generatePawnMoves(long Targets, int moveDiff, int colour, List moveList, int flags, int[] board){

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
    private void generatePawnPromotionMoves(long Targets, int moveDiff, int colour, List moveList, int[] board){

        int pieceType = wPawn + (colour * 6);

        while(Targets != 0){
            int toSq = Long.numberOfTrailingZeros(Targets);
            int fromSq = Integer.remainderUnsigned(toSq - moveDiff, 64);
            int capture = board[toSq];
            int move = pieceType << 28 | capture << 24 | fromSq << 16 | toSq << 8;

            moveList.add(move | Constants.moveFlagPromotion | wRook + (colour * 6));   //Rook
            moveList.add(move | Constants.moveFlagPromotion | wKnight + (colour * 6)); //Knight
            moveList.add(move | Constants.moveFlagPromotion | wBishop + (colour * 6)); //Bishop
            moveList.add(move | Constants.moveFlagPromotion | wQueen + (colour * 6));  //Queen
            Targets &= Targets - 1;

        }
    }

    /*Allows for pushing pawns either forward one rank, or backwards one rank, just by adjusting the shift
     * value
     */
    private long circularLeftShift(long bitBoard, int shift){
        return bitBoard << shift | bitBoard >> (64 - shift);
    }

}
