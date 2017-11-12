/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess.engine;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Pre-computed values used in ChessEngine class. Some are for convenience, but
 * this class is depended on by ChessEngine to compute the necessary attack sets
 * for sliding piece move generation. Most of the code for that, which involves
 * something referred to as "magic bitboards", is based off a perfact hashing
 * algorithm for quick efficient move sets. I got the algorithm from the following
 * blog:
 * http://www.afewmorelines.com/understanding-magic-bitboards-in-chess-programming/
 * But there were a number of changes I needed to make to the code for it to run
 * in my framework.
 * 
 * Everything here is based off the following bitboard representation:
 * 
 * 56 57 58 59 60 61 62 63
 * 48 49 50 51 52 53 54 55
 * 40 41 42 43 44 45 46 47
 * 32 33 34 35 36 37 38 39
 * 24 25 26 27 28 29 30 31
 * 16 17 18 19 20 21 22 23
 * 8  9  10 11 12 13 14 15
 * 0  1  2  3  4  5  6  7
 * 
 * where the 64-bits in the long represent the following:
 * 
 * MSb [63, 62, 61 .. 0] LSb
 * 
 * For example, all the squares in the bottom row, a.k.a Rank 1, could be represent
 * as long 0xff, or binary 0b11111111, since those are the 8 least significant
 * digits
 * 
 * 
 * 
 * @author thejoeflow
 */

public class Constants implements Serializable{

    static Logger logger = Logger.getLogger(Constants.class);

    public static int WHITE = 			0;
    public static int BLACK =			1;

    public static int wPawn = 			0;
    public static int wKnight = 		1;
    public static int wBishop = 		2;
    public static int wRook = 			3;
    public static int wQueen = 		    4;
    public static int wKing = 			5;

    public static int bPawn = 			6;
    public static int bKnight =		    7;
    public static int bBishop =		    8;
    public static int bRook = 			9;
    public static int bQueen = 		    10;
    public static int bKing = 			11;

    public static int empty = 			0xE;

    public static int[] kings = 		        new int[]{wKing, bKing};

    public static long ALL_SET =                0xffffffffffffffffL; //all 64 squares

    public static long RANK_1 =                 0xffL;
    public static long RANK_2 =                 0xff00L;
    public static long RANK_3 =                 0xff0000L;
    public static long RANK_4 =                 0xff000000L;
    public static long RANK_5 =                 0xff00000000L;
    public static long RANK_6 =                 0xff0000000000L;
    public static long RANK_7 =                 0xff000000000000L;
    public static long RANK_8 =                 0xff00000000000000L;

    public static long[] RANKS =                new long[]{RANK_1, RANK_2, RANK_3, RANK_4, RANK_5, RANK_6, RANK_7, RANK_8};

    public static long FILE_A =                 0x101010101010101L;
    public static long FILE_B =                 0x202020202020202L;
    public static long FILE_C =                 0x404040404040404L;
    public static long FILE_D =                 0x808080808080808L;
    public static long FILE_E =                 0x1010101010101010L;
    public static long FILE_F =                 0x2020202020202020L;
    public static long FILE_G =                 0x4040404040404040L;
    public static long FILE_H =                 0x8080808080808080L;

    public static long[] FILES =                new long[]{FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H};

    public static long CENTER_4 =               0x1818000000L;

    //Used when setting moveFlags. Makes for easier code and less mistakes
    public static int moveFlagPromotedPiece =   0b00001111;
    public static int moveFlagPromotion =       0b00010000;
    public static int moveFlagEnPassant =       0b00100000;
    public static int moveFlagQueenSideCastle = 0b01000000;
    public static int moveFlagKingSideCastle =  0b10000000;

    public static int gameFlagEnPassantMask = 0b00001110;


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
    public static byte EN_PASSANT =             0b00000001;
    public static byte FILE_0 =                 0b00000000;
    public static byte FILE_1 =                 0b00000010;
    public static byte FILE_2 =                 0b00000100;
    public static byte FILE_3 =                 0b00000110;
    public static byte FILE_4 =                 0b00001000;
    public static byte FILE_5 =                 0b00001010;
    public static byte FILE_6 =                 0b00001100;
    public static byte FILE_7 =                 0b00001110;
    public static byte BLACK_QUEENSIDE_CASTLE = 0b00010000;
    public static byte BLACK_KINGSIDE_CASTLE =  0b00100000;
    public static byte WHITE_QUEENSIDE_CASTLE = 0b01000000;
    public static byte WHITE_KINGSIDE_CASTLE =  (byte) 0b10000000;

    //The squares in between the castling squares which need to be checked for potential checks
    public static long[] queenCastleSquares =   new long[]{0xe, 0xe00000000000000L};
    public static long[] kingCastleSquares =    new long[]{0x60, 0x6000000000000000L};

    public static long[] queenSideCastleDestinationSquare = {0x4L, 0x400000000000000L};
    public static long[] kingSideCastleDestinationSquare = 	{0x40L, 0x4000000000000000L};

    public static int[] initKingPos =	new int[]{4, 60};



    //All the following arrays are used in the magic bitboard generation
    public long[][] occupancyVariation = new long[64][];
    public long[][] occupancyAttackSet = new long[64][];

    public long[] magicNumberRook =      new long[64];
    public int[] magicShiftsRook =       new int[64];

    public long[] magicNumberBishop =    new long[64];
    public int[] magicShiftsBishop =     new int[64];

    public long[][] magicMovesRook =     new long[64][];
    public long[][] magicMovesBishop =   new long[64][];

    public long[] RookMaskOnSquare =     new long[64];
    public long[] BishopMaskOnSquare =   new long[64];

    public long[] KnightMoves =          new long[64];
    public long[] KingMoves =            new long[64];

    public static Constants init(ClassLoader classLoader) {

        URL constantsURL = classLoader.getResource("chess.constants");

        try {
            File constantsFile;
            if(constantsURL != null){
                constantsFile = new File(constantsURL.toURI());
                logger.info("Constants Initialization - Found cached version of Constants. Loading into memory.");
                return (Constants) new ObjectInputStream(new FileInputStream(constantsFile)).readObject();
            }
            else{
                Constants c = new Constants();
                logger.info("Constants Initialization - No cached version of Constants found. Generating cache file.");
                constantsFile = new File("resources/chess.constants");
                new ObjectOutputStream(new FileOutputStream(constantsFile)).writeObject(c);
                logger.info("Constants Initialization - Constants written to 'resources/chess.constants' successfully");
                return c;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Constants(){
        /*KingMoves and KnightMoves are 64-element long arrays which represent the
        possible destination squares for kings and knights at each square on the board.
        They are built by initially defining the 35th square and bitshifting in either
        direction to define squares 34 --> 0 and 36 --> 63. Then the wrap-around
        overflow must be masked out for the edge filFes in the following for loop.
        */
        KingMoves[35] = 0x1c141c000000L;
        /*
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 1 1 1 0 0 0
        0 0 1 0 1 0 0 0
        0 0 1 1 1 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        */
        KnightMoves[35] = 0x14220022140000L;
        /*
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 1 0 1 0 0 0
        0 1 0 0 0 1 0 0
        0 0 0 0 0 0 0 0
        0 1 0 0 0 1 0 0
        0 0 1 0 1 0 0 0
        0 0 0 0 0 0 0 0
        */
        for(int i = 36; i<64; i++){
            KingMoves[i] = KingMoves[i-1] << 1;
            KnightMoves[i] = KnightMoves[i-1] << 1;
        }

        for(int j = 34; j>-1; j--){
            KingMoves[j] = KingMoves[j+1] >> 1;
            KnightMoves[j] = KnightMoves[j+1] >> 1;
        }

        //Mask out wrap-around overflows as a result of the bitshifting
        for(int k = 0; k<64; k++){
            if( k%8 == 7){ KingMoves[k] &= ~(FILE_A);}
            if( k%8 == 0){ KingMoves[k] &= ~(FILE_H);}
            if( k%8 == 6){ KnightMoves[k] &= ~(FILE_A);}
            if( k%8 == 7){ KnightMoves[k] &= ~(FILE_A | FILE_B);}
            if( k%8 == 0){ KnightMoves[k] &= ~(FILE_G | FILE_H);}
            if( k%8 == 1){ KnightMoves[k] &= ~(FILE_H);}
        }

        /*
        The following code builds the RookMaskOnSquare and BishopMaskOnSquare arrays.
        These are 64-element long arrays which represent the destination squares for
        rooks and bishops assuming an empty board.

        For example, for a rook on square 35:
            0 0 0 0 0 0 0 0
            0 0 0 1 0 0 0 0
            0 0 0 1 0 0 0 0
            0 0 0 1 0 0 0 0
            0 1 1 0 1 1 1 0
            0 0 0 1 0 0 0 0
            0 0 0 1 0 0 0 0
            0 0 0 0 0 0 0 0

        The boundary squares are not set within the mask because these will always
        be blocking squares for sliding piece movement, they do not need to be checked
        for piece occupancy when determining the attack sets.

        These arrays will be used in the three functions which build the magic
        bitboards for sliding move piece generation.
        */

        //ray directions for rooks and bishops
        int[] rookDelta = new int[]{-1, 1, -8, 8};
        int[] bishopDelta = new int[]{-9, 7, 9, -7};

        //terminating files/ranks for the respective rays
        long[] rookTerminator = new long[]{FILE_A, FILE_H, RANK_1, RANK_8};
        long[] bishopTerminator = new long[]{RANK_1, FILE_A, RANK_8, FILE_H};

        int square;
        for(int i = 0; i < 64; i++){ //for each square

            for(int j = 0; j < 4; j++){ //for each ray direction
                square = i; //start at the origin square
                while(((1L << square) & rookTerminator[j]) == 0){
                    RookMaskOnSquare[i] |= (1L << square);
                    square += rookDelta[j];
                }
                square = i;
                //diagonal rays can be terminated by either a file or rank, so both conditions must be checked
                while(((1L << square) & bishopTerminator[j]) == 0 && ((1L << square) & bishopTerminator[(j+1)%4]) == 0){
                    BishopMaskOnSquare[i] |= (1L << square);
                    square +=  bishopDelta[j];
                }
            }

            //remove the origin square since it would not be a destination square
            RookMaskOnSquare[i] ^= (1L << i);
            BishopMaskOnSquare[i] ^= (1L << i);

        }

    //Rooks
    generateOccupancyVariations(true);
    generateMagicNumbers(true);
    generateMoveDatabase(true);

    //Bishops
    generateOccupancyVariations(false);
    generateMagicNumbers(false);
    generateMoveDatabase(false);


    }


    /**
     * Generates every occupancy variation for a particular square for both bishops
     * and rooks, and then generates each corresponding attack set for every
     * occupancy variation.
     *
     * For example, the following are two possible occupancy variations for a rook
     * on square 2:
     *
     * Variation 1 (Rook - square 2)
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 1 R 0 1 0 1 0
     *
     *   Variation 2 (Rook - square 2)
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   1 1 R 0 1 0 0 0
     *
     *   However, both these occupancy variations produce the same resulting attack
     *   set for rooks, which is shown below
     *
     *   Attack set (Rook - square 2)
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 1 R 0 1 0 0 0
     *
     *   The purpose of the attack set is to define the boundaries of the rays for the
     *   sliding piece in every direction based off the positions of all the other
     *   pieces on the board. Once this is determined, the blocking pieces which define
     *   the boundaries of the attack set just need to be checked to find out if they
     *   are capturable or not, or in other words if they are enemy pieces or friendly
     *   pieces.
     *
     * The number of occupancy variations for a particular piece on a square is just
     * the total number of different permutations of pieces existing on the possible
     * destination squares for a piece on a square. This is simply 2^(number of
     * destination squares on the mask). For example, for the rook on Square 2, the
     * destination square mask looks like this:
     *   0 0 0 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 1 R 1 1 1 1 0
     *
     * Since this mask has 11 bits set in it, the amount of different occupancy
     * variations for this square will be 2^11.
     *
     * @param isRook    true if rook; false if bishop
     */
    @SuppressWarnings("empty-statement")
    private void generateOccupancyVariations(boolean isRook){

        int i, j, square;
        long mask;
        int variationCount;
        int[] setBitsInMask, setBitsInIndex;
        int[] bitCount = new int[64];

        for(square = 0; square < 64; square++){

            /*mask is a bitboard representing the possible destination squares
            for the rook or bishop on a particular square
            */
            mask = isRook? RookMaskOnSquare[square] : BishopMaskOnSquare[square];
            setBitsInMask = getIndexOfSetBits(mask);
            bitCount[square] = Long.bitCount(mask); //Number of set bits in the mask
            variationCount = (int)(1L << bitCount[square]); //2^bitCount[square]

            occupancyVariation[square] = new long[variationCount];
            occupancyAttackSet[square] = new long[variationCount];

            for(i = 0; i < variationCount; i++){

                occupancyVariation[square][i] = 0;

                /*Map the bits set in the index (0 - 2^N-1) to the bits set in the
                mask in order to loop through every occupancy variation possible
                */
                setBitsInIndex = getIndexOfSetBits(i);
                for(j = 0; j < setBitsInIndex.length; j++){

                    occupancyVariation[square][i] |= (1L << setBitsInMask[setBitsInIndex[j]]);
                }

                /*Build the corresponding Attack Set for the particular occupancy variation
                Essentially, loop through in each of the 4 ray directions until you hit a set bit
                or a border on the board. If it is not a border of the board, then set the bit in
                the square that the loop was terminated on. This will be the bounding square that
                the piece is able to move to.
                */
                if(isRook){

                    for(j = square+8; j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=8);
                    if (j<64) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square-8; j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=8);
                    if (j>-1) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square+1; j%8!=0 && (occupancyVariation[square][i] & (1L << j)) == 0; j++);
                    if (j%8!=0) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square-1; (j%8 + 8)%8!=7 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j--);
                    if ((j%8 + 8)%8!=7) occupancyAttackSet[square][i] |= (1L << j);

                }
                //For bishops, the diagonal rays can be terminated by either of two borders, so two
                //boundary conditions need to be checked
                else{

                    for(j = square+9; j%8!=0 && j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=9);
                    if (j%8!=0 && j<64) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square-9; (j%8 + 8)%8!=7 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=9);
                    if ((j%8 + 8)%8!=7 && j>-1) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square+7; j%8!=7 && j<64 && (occupancyVariation[square][i] & (1L << j)) == 0; j+=7);
                    if (j%8!=7 && j<64) occupancyAttackSet[square][i] |= (1L << j);

                    for(j = square-7; (j%8 + 8)%8!=0 && j>-1 && (occupancyVariation[square][i] & (1L << j)) == 0; j-=7);
                    if ((j%8 + 8)%8!=0 && j>-1) occupancyAttackSet[square][i] |= (1L << j);

                }
            }
        }
    }

    /**
     * Magic Numbers are generated using a perfect hashing technique. Every occupancy
     * variation is mapped to its corresponding attack set through operations involving
     * a "magic number" which is specific to each square. This function searches for
     * and stores the magic numbers which will be used during move generation for this
     * purpose. Given the occupancy of pieces on the board, perform the operations using
     * the magic number to instantly return the attack set that represents the possible
     * squares the sliding piece can move to.
     *
     * There is one magic number for each square and sliding piece type combination (rook
     * or bishop), which means that in total there are 2 x 64 = 128 magic numbers that need
     * to be computed.
     *
     * For each of these piece type and square combinations, there is a magic number, M, such
     * that the following conditions hold:
     *
     * For every occupancy variation (v) and corresponding attack set (A) pair, [v, A], which
     * were computed in the function 'generateoccupancyVariations()' above, the following holds:
     *
     *
     * F(v, M) --> I
     * G(I) --> A
     *
     * where F is a function that generates an index, I, and G is an array which returns the correct
     * attack set A. Note that since multiple occupancy variations can map to the same attack set A,
     * then if F returns the same I for two different variations v and v', this is acceptable as long
     * as both v and v' correspond to the same attack set A.
     *
     * @param isRook    true if rook; false if bishop
     */
    private void generateMagicNumbers(boolean isRook){

        int i, j, square, variationCount;
        boolean fail;

        Random r = new Random(); //initialize the random number generator
        long magicNumber = 0;
        int index;

        for (square = 0; square < 64; square++){

            int bitCount = Long.bitCount(isRook ? RookMaskOnSquare[square] : BishopMaskOnSquare[square]); //Number of bits set in the blank mask
            variationCount = (int)(1L << bitCount); //Number of variations to look at (2^(Number of bits set))
            HashMap<Integer, Long> usedBy = new HashMap<Integer, Long>();

            do{

                magicNumber = r.nextLong() & r.nextLong() & r.nextLong(); //magic numbers usually are longs with not many bits set

                usedBy.clear();

                for(i = 0, fail = false; i < variationCount && !fail; i++){

                    //right shift enough so that the index is no larger than the variation count (2 ^ bitCount)
                    //cast to a 32-bit int. No movement mask will ever have more than 15 bits set, so this is a safe operation
                    index = (int)((occupancyVariation[square][i] * magicNumber) >>> (64 - bitCount));

                    //set fail to true if the index already points to an attack set and that attack set does not match the current occupancy variation
                    fail = usedBy.containsKey(index) && usedBy.get(index) != occupancyAttackSet[square][i];
                    //Add the correct attack set to the index position generated by the current occupancy variation
                    //This won't matter if the previous fail check evaluated to true, but the loop won't break until the fail condition is checked in the for loop statement
                    usedBy.put(index, occupancyAttackSet[square][i]);
                }
            }
            while(fail); //if fail is still false, then all the variations must have been added to the usedBy array successfully with no undesirable collisions, therefore the magic number is found


            if(isRook){
                magicNumberRook[square] = magicNumber; 		//record the magic number for that square
                magicShiftsRook[square] = 64 - bitCount; 	//the shifts is directly related to the square and how many bits are set in the blank mask
            }
            else{
                magicNumberBishop[square] = magicNumber;	//record the magic number for that square
                magicShiftsBishop[square] = 64 - bitCount;	//the shifts is directly related to the square and how many bits are set in the blank mask
            }
        }
    }

    /**
     * Generates the valid move bitboard for every square and occupancy variation and stores it at the magic index of the variation. Main difference between
     * a move bitboard and an attack set is that the edge ranks and files are set to 1 to indicate they are valid moves. For example,
     *
     *   Attack set (Rook - square 2)
     *   0 0 0 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 1 R 0 1 0 0 0
     *
     *   Equivalent Move Bitboard (Rook - square 2)
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 1 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   0 0 0 0 0 0 0 0
     *   1 1 R 0 1 0 0 0
     *
     *   In the game logic, the returned move bitboard will be ANDed with the inverse of all friendly pieces on the board (non-capturable pieces) and that will
     *   return the possible target squares for the sliding piece and take care of the edge squares.
     *
     */
    private void generateMoveDatabase(boolean isRook){

        long validMoves;
        int variations, bitCount;
        int square, i, j, magicIndex;

        for(square = 0; square < 64; square++){

            bitCount = Long.bitCount(isRook ? RookMaskOnSquare[square] : BishopMaskOnSquare[square]);
            variations = (int)(1L << bitCount);

            if(isRook){
                magicMovesRook[square] = new long[variations];
            }
            else{
                magicMovesBishop[square] = new long[variations];
            }

            for(i = 0; i < variations; i++){

                validMoves = 0;

                if(isRook){

                    magicIndex = (int)((occupancyVariation[square][i] * magicNumberRook[square]) >>> magicShiftsRook[square]);

                    for(j = square+8; j < 64; j+=8){ //move up
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break; //if square occupied, end there
                    }
                    for(j = square-8; j > -1; j-=8){ //move down
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    for(j = square+1; j%8 != 0; j++){ //move right
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    for(j = square-1; (j%8 + 8)%8 != 7; j--){ //move left
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }

                    magicMovesRook[square][magicIndex] = validMoves;
                }
                else{

                    magicIndex = (int)((occupancyVariation[square][i] * magicNumberBishop[square]) >>> magicShiftsBishop[square]);

                    for(j = square+9; j < 64 && j%8 != 0; j+=9){ //move up and right
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    for(j = square-9; j > -1 && (j%8 + 8)%8 != 7; j-=9){ //move down and left
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    for(j = square+7; j < 64 && j%8 != 7; j+=7){ //move up and left
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    for(j = square-7; j > -1 && (j%8 + 8)%8 != 0; j-=7){ //move down and right
                        validMoves |= (1L << j);
                        if((occupancyVariation[square][i] & (1L << j)) != 0) break;
                    }
                    magicMovesBishop[square][magicIndex] = validMoves;

                }
            }
        }

    }

    /**
     * Returns an array representing the position of every bit set in a long, from 0 (LSB) to 63 (MSB)
     *
     * @param l	long to compute the set bits of
     * @return 	an int[] with the position of every set bit
     */
    private int[] getIndexOfSetBits(long l){
        ArrayList<Integer> setBits = new ArrayList();

        while(l > 0){
            setBits.add(Long.numberOfTrailingZeros(l)); //get the leading one
            l &= l-1; 									//mask out the leading one
        }

        int[] sBi = new int[setBits.size()];

        for(int i = 0; i < sBi.length; i++){
            sBi[i] = setBits.get(i);
        }
        return sBi;
    }
}
